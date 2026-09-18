// --- MainActivityDocuments.kt ---
package com.nexopp

import android.content.Intent
import android.net.Uri
import com.nexopp.audio.documentAudioFiles
import com.nexopp.format.SaveFormat
import com.nexopp.io.LoadedFile
import com.nexopp.io.StorageLimits
import com.nexopp.io.UriStaging
import com.nexopp.io.xoppNameFor
import com.nexopp.render.DrawingSurfaceView
import com.nexopp.render.ImageImport
import com.nexopp.render.ImportPdfMode
import com.nexopp.render.MultiImagePlacement
import com.nexopp.render.PdfImport
import com.nexopp.render.PdfPageCache
import com.nexopp.render.PdfTextExtractor
import com.nexopp.render.PdfTextIndexCache
import com.nexopp.render.blankDocument
import com.nexopp.render.insertMultipleImages
import com.nexopp.tabs.OpenTab
import com.nexopp.tabs.TabStore
import com.nexopp.ui.AppSettings
import java.io.File

internal fun MainActivity.openDocument(uri: Uri) {
    snapshotActiveTab()
    val created = tabs.open(
        OpenTab(TabStore.newId(), displayName(uri), blankDocument(), uri.toString()),
    )
    pendingSaveName = displayName(uri)
    tabsTick.value++
    io.persist(uri)
    inBackground("Abriendo ${displayName(uri)}…", { io.stageIn(uri, "open") }) { result ->
        result.mapCatching { staged -> try { loadDocument(staged, uri) } finally { staged.delete() } }
            .onSuccess { snapshotActiveTab() }
            .onFailure {
                toast("Error al abrir: ${it.message}")
                tabs.close(created)
                tabs.active?.let(::showTab)
            }
        tabsTick.value++
        persistTabs()
    }
}

internal fun MainActivity.applyStorageLimits(settings: AppSettings) {
    io.limits = StorageLimits(
        textImportBytes = settings.textImportLimitBytes,
        pdfCacheBytes = settings.pdfCacheLimitBytes,
        imageCacheBytes = settings.pdfCacheLimitBytes,
    )
}

internal fun MainActivity.loadDocument(staged: File, source: Uri) {
    when (val loaded = io.read(staged, displayName(source), source)) {
        is LoadedFile.Pdf -> {
            saveFormat = if (loaded.generated) SaveFormat.ZIPPED else SaveFormat.ORIGINAL
            adoptPdf(loaded.file, ImportPdfMode.REPLACE, source.toString(), inStore = loaded.generated)
            pendingSaveName = xoppNameFor(displayName(source))
            tabs.updateActive { it.copy(title = pendingSaveName, uri = null) }
            tabsTick.value++
            return
        }
        is LoadedFile.Image -> {
            saveFormat = SaveFormat.ORIGINAL
            if (!adoptImage(loaded.file, source.toString())) {
                toast("No se pudo leer esa imagen")
                return
            }
            pendingSaveName = xoppNameFor(loaded.name)
            tabs.updateActive { it.copy(title = pendingSaveName, uri = null) }
            tabsTick.value++
            return
        }
        is LoadedFile.Doc -> {
            saveFormat = loaded.format
            surface?.setPdfSource(loaded.pdf?.let(PdfPageCache::shared))
            surface?.setPdfTextIndex(null)
            surface?.setImageSources(loaded.images)
            surface?.load(loaded.document)
            if (loaded.pdf != null) extractPdfTextInBackground(loaded.pdf)
            else if (loaded.missingPdf) toast("PDF de fondo no encontrado; esas páginas estarán en blanco")
            else if (loaded.missingImage) toast("Imagen de fondo no encontrada; esas páginas estarán en blanco")
        }
    }
    pullAudioSidecars()
}

internal fun MainActivity.importPdf(uri: Uri, mode: ImportPdfMode = ImportPdfMode.REPLACE) {
    runCatching { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
    inBackground("Importando ${displayName(uri)}…", { io.stageIn(uri, "import") }) { result ->
        result.mapCatching { staged -> try { adoptPdf(staged, mode, uri.toString()) } finally { staged.delete() } }
            .onFailure { toast("Error al importar PDF: ${it.message}") }
    }
}

internal fun MainActivity.adoptPdf(source: File, mode: ImportPdfMode, reference: String, inStore: Boolean = false) {
    val file = if (inStore) source else io.adoptPdf(source)
    val view = surface
    val existing = view?.pdfSourceFile()
    if (mode == ImportPdfMode.APPEND && existing != null && view.hasPdfBackground()) {
        appendMergedPdf(view, existing, file)
        return
    }
    val cache = PdfPageCache.shared(file)
    view?.setPdfSource(cache)
    view?.setPdfTextIndex(null)
    when (mode) {
        ImportPdfMode.REPLACE -> {
            view?.setImageSources(emptyMap())
            view?.load(PdfImport.documentFor(cache, reference))
        }
        ImportPdfMode.APPEND -> view?.appendPages(PdfImport.pagesFor(cache, reference))
    }
    if (view == null) cache.close()
    PdfTextIndexCache.forget(file)
    extractPdfTextInBackground(file)
}

internal fun MainActivity.adoptImage(source: File, reference: String): Boolean {
    val (widthPx, heightPx) = ImageImport.pixelSize(source) ?: return false
    surface?.setPdfSource(null)
    surface?.setPdfTextIndex(null)
    surface?.setImageSources(mapOf(reference to io.adoptImage(source)))
    surface?.load(ImageImport.documentFor(widthPx, heightPx, reference))
    return true
}

internal fun MainActivity.appendMergedPdf(view: DrawingSurfaceView, existing: File, incoming: File) {
    val offset = view.pdfSourcePageCount()
    val added = PdfPageCache(incoming).use { PdfImport.pagesFor(it, reference = null, pageNoOffset = offset) }
    val joined = io.merge(existing, incoming)
    PdfTextIndexCache.forget(joined)
    view.setPdfSource(PdfPageCache.shared(joined))
    view.setPdfTextIndex(null)
    view.appendPdfPages(added, joined.absolutePath)
    extractPdfTextInBackground(joined)
}

internal fun MainActivity.extractPdfTextInBackground(file: File, into: DrawingSurfaceView? = surface) {
    val view = into ?: return
    PdfTextIndexCache.get(file)?.let { return view.setPdfTextIndex(it) }
    Thread {
        val index = PdfTextExtractor().extract(file)
        PdfTextIndexCache.put(file, index)
        view.post { view.setPdfTextIndex(index) }
    }.start()
}

internal fun MainActivity.exportPdf(uri: Uri) = runCatching {
    staging.writeTo(uri) { output: java.io.OutputStream -> surface?.exportPdf(output) }
}.onFailure { toast("Error al exportar PDF: ${it.message}") }

internal fun MainActivity.saveExportToUri(
    uri: Uri,
    format: com.nexopp.io.ExportManager.ExportFormat,
    pageIndices: List<Int>,
    scale: Float
) {
    val view = surface ?: return
    val doc = view.toDocument()
    val exportMgr = com.nexopp.io.ExportManager(this, view.pdfSourceFile()?.let(PdfPageCache::shared))
    inBackground("Exportando documento…", {
        contentResolver.openOutputStream(uri, "wt")?.use { out ->
            exportMgr.exportToStream(doc, out, format, pageIndices, scale)
        }
    }) { result ->
        result.onSuccess { toast("Exportación completada") }
            .onFailure { toast("Error al exportar: ${it.message}") }
    }
}

internal fun MainActivity.shareExport(
    format: com.nexopp.io.ExportManager.ExportFormat,
    pageIndices: List<Int>,
    scale: Float
) {
    val view = surface ?: return
    val doc = view.toDocument()
    val title = pendingSaveName.ifBlank { "apuntes" }
    val exportMgr = com.nexopp.io.ExportManager(this, view.pdfSourceFile()?.let(PdfPageCache::shared))
    inBackground("Preparando archivo para compartir…", {
        exportMgr.createShareIntent(doc, title, format, pageIndices, scale)
    }) { result ->
        result.onSuccess { intent ->
            if (intent != null) {
                startActivity(Intent.createChooser(intent, "Compartir apuntes con…"))
            } else {
                toast("No se pudo generar el archivo para compartir")
            }
        }.onFailure {
            toast("Error al preparar exportación: ${it.message}")
        }
    }
}

internal fun MainActivity.insertPickedImages(uris: List<Uri>) = runCatching {
    if (uris.isEmpty()) return@runCatching
    val placement = pendingImagePlacement
    pendingImagePlacement = null

    val targetUris = if (uris.size > MultiImagePlacement.MAX_IMAGES) {
        toast("Se seleccionaron ${uris.size} imágenes. Se procesarán las primeras ${MultiImagePlacement.MAX_IMAGES}.")
        uris.take(MultiImagePlacement.MAX_IMAGES)
    } else {
        uris
    }

    val bytesList = mutableListOf<ByteArray>()
    for (uri in targetUris) {
        val bytes = staging.readBytes(uri)
        val opts = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
        android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
        if (opts.outWidth <= 0 || opts.outHeight <= 0) {
            toast("Error: una de las imágenes seleccionadas es inválida o corrupta. Operación cancelada.")
            return@runCatching
        }
        bytesList.add(bytes)
    }

    val view = surface ?: return@runCatching
    val pageIndex = placement?.pageIndex ?: view.visiblePageIndex()
    val success = view.insertMultipleImages(bytesList, pageIndex, placement)
    if (!success) {
        toast("Error al insertar imágenes.")
    }
}.onFailure { toast("Error al insertar imágenes: ${it.message}") }

internal fun MainActivity.insertPickedImage(uri: Uri) {
    insertPickedImages(listOf(uri))
}

internal fun MainActivity.insertCapturedPhoto(bitmap: android.graphics.Bitmap) = runCatching {
    val placement = pendingImagePlacement ?: com.nexopp.render.Placement(surface?.visiblePageIndex() ?: 0, 100.0, 100.0)
    pendingImagePlacement = null
    val stream = java.io.ByteArrayOutputStream()
    bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 92, stream)
    val bytes = stream.toByteArray()
    surface?.insertImage(placement, bytes)
}.onFailure { toast("Error al procesar foto: ${it.message}") }

internal fun MainActivity.importAttachmentUri(uri: Uri) = runCatching {
    val notebook = pendingSaveName.ifBlank { "cuaderno.xopp" }
    val filename = displayName(uri).ifBlank { "adjunto_${System.currentTimeMillis()}" }
    val mime = contentResolver.getType(uri) ?: "application/octet-stream"
    val stream = contentResolver.openInputStream(uri) ?: return@runCatching
    val curPage = surface?.visiblePageIndex()
    stream.use { input ->
        attachmentStore.addAttachment(notebook, filename, mime, input, curPage)
    }
    toast("Archivo adjunto guardado: $filename")
}.onFailure { toast("Error al adjuntar archivo: ${it.message}") }

internal fun MainActivity.saveDocument(uri: Uri) {
    val view = surface ?: return
    val staged = runCatching {
        io.encode(view.toDocument(), view.pdfSourceFile(), saveFormat, uri, view.imageSources())
    }
        .getOrElse { toast("Error al guardar: ${it.message}"); return }

    inBackground("Guardando ${displayName(uri)}…", { io.stageOut(staged, uri) }) { result ->
        staged.delete()
        result.onFailure { toast("Error al guardar: ${it.message}") }
            .onSuccess { afterSaved(view, uri) }
    }
}

internal fun MainActivity.afterSaved(view: DrawingSurfaceView, uri: Uri) {
    io.persist(uri)
    tabs.active?.id?.let(crashRecoveryManager::discard)
    tabs.updateActive { it.copy(title = displayName(uri), uri = uri.toString()) }
    pendingSaveName = displayName(uri)
    snapshotActiveTab()
    tabsTick.value++
    persistTabs()
    pushAudioSidecars()
    if (audioFolder == null && audio.missingFor(view.toDocument()).isEmpty() &&
        documentHasAudio(view)
    ) {
        toast("Elige una carpeta de audio para guardar las grabaciones junto a este archivo")
    }
}

internal fun MainActivity.saveActiveTab() {
    val target = tabs.active?.uri?.let(Uri::parse)?.takeIf(io::isWritable)
    if (target != null) saveDocument(target) else saveLauncher.launch(pendingSaveName)
}

internal fun MainActivity.documentHasAudio(view: DrawingSurfaceView): Boolean =
    documentAudioFiles(view.toDocument()).isNotEmpty()

internal fun MainActivity.beginSaveAs(filename: String, format: SaveFormat) {
    saveFormat = format
    pendingSaveName = filename
    saveLauncher.launch(filename)
}