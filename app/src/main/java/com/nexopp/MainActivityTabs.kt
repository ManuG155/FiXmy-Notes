// --- MainActivityTabs.kt ---
package com.nexopp

import android.graphics.Bitmap
import android.net.Uri
import com.nexopp.panes.EditorPane
import com.nexopp.render.PageThumbnail
import com.nexopp.render.PdfPageCache
import com.nexopp.render.blankDocument
import com.nexopp.tabs.DocColors
import com.nexopp.tabs.OpenTab
import com.nexopp.tabs.TabStore
import com.nexopp.ui.TabsUiState
import java.io.File

internal fun MainActivity.tabsUiState(p: EditorPane): TabsUiState {
    tabsTick.value
    val dots = DocColors.assign(panes.flatMap { pane -> pane.tabs.tabs.map(OpenTab::docKey) })
    return TabsUiState(
        titles = p.tabs.tabs.map { it.title },
        dotColors = p.tabs.tabs.map { dots[it.docKey] },
        activeIndex = p.tabs.activeIndex,
        onSelect = { selectTab(it, p) },
        onClose = { closeTab(it, p) },
        onNew = { newTab(p) },
        onMove = { sendTabToOtherPane(it, p, keepHere = false) },
        onMirror = { sendTabToOtherPane(it, p, keepHere = true) },
        onReorder = { from, to -> reorderTab(from, to, p) },
        onOverview = { snapshotActiveTab(p) },
        preview = { index, widthPx, onReady -> previewTabPage(p, index, widthPx, onReady) },
    )
}

internal fun MainActivity.sendTabToOtherPane(index: Int, from: EditorPane, keepHere: Boolean) {
    val other = panes.firstOrNull { it !== from } ?: return
    snapshotActiveTab(from)
    val source = from.tabs.tabs.getOrNull(index) ?: return
    hydrate(other)
    val newId = TabStore.newId()
    if (!source.hydrated) other.store.adopt(from.store, source.id, newId)
    other.tabs.open(source.copy(id = newId))
    if (!keepHere) {
        val showing = from.tabs.active?.id
        from.tabs.close(index)
        if (from.tabs.isEmpty) from.tabs.open(blankTab())
        from.tabs.active?.takeIf { it.id != showing }?.let { show(it, from) }
    }
    other.tabs.active?.let { show(it, other) }
    splitView.value = true
    from.persist()
    other.persist()
    tabsTick.value++
}

internal fun MainActivity.hydrate(p: EditorPane) {
    if (!p.tabs.isEmpty) return
    val session = p.store.load() ?: return
    session.tabs.forEach(p.tabs::open)
    p.tabs.select(session.activeIndex)
}

internal fun MainActivity.snapshotActiveTab(p: EditorPane = pane) {
    mirrors.flush()
    val view = p.surface ?: return
    if (p.tabs.active?.hydrated == false) return
    p.tabs.updateActive {
        it.copy(
            document = view.toDocument(),
            format = p.saveFormat,
            pdfPath = view.pdfSourceFile()?.absolutePath,
            page = view.visiblePageIndex(),
        )
    }
}

internal fun MainActivity.show(tab: OpenTab, p: EditorPane = pane) {
    if (tab.hydrated) return showTab(tab, p)
    Thread {
        val full = p.store.hydrate(tab)
        runOnUiThread {
            if (p.tabs.active?.id != full.id) return@runOnUiThread
            p.tabs.updateActive { if (it.id == full.id) full else it }
            showTab(full, p)
            tabsTick.value++
        }
    }.start()
}

internal fun MainActivity.showTab(tab: OpenTab, p: EditorPane = pane) {
    val view = p.surface ?: return
    p.saveFormat = tab.format
    p.pendingSaveName = tab.title
    val pdf = tab.pdfPath?.let(::File)?.takeIf(File::exists)
    view.setPdfSource(pdf?.let(PdfPageCache::shared))
    view.setPdfTextIndex(null)
    view.load(tab.document)
    if (tab.page > 0) view.goToPage(tab.page)
    if (pdf != null) extractPdfTextInBackground(pdf, view)
}

internal fun MainActivity.previewTabPage(
    p: EditorPane,
    index: Int,
    widthPx: Int,
    onReady: (Bitmap?) -> Unit,
) {
    val tab = p.tabs.tabs.getOrNull(index) ?: return onReady(null)
    previewWorker.execute {
        val full = if (tab.hydrated) tab else runCatching { p.store.hydrate(tab) }.getOrNull()
        val page = full?.document?.pages?.getOrNull(full.page)
        val bitmap = page?.let { runCatching { PageThumbnail.render(it, widthPx) }.getOrNull() }
        runOnUiThread { onReady(bitmap) }
    }
}

internal fun MainActivity.selectTab(index: Int, p: EditorPane = pane) {
    snapshotActiveTab(p)
    if (!p.tabs.select(index)) return
    p.tabs.active?.let { show(it, p) }
    tabsTick.value++
    p.persist()
}

internal fun MainActivity.reorderTab(from: Int, to: Int, p: EditorPane = pane) {
    if (!p.tabs.move(from, to)) return
    tabsTick.value++
    p.persist()
}

internal fun MainActivity.closeTab(index: Int, p: EditorPane = pane) {
    snapshotActiveTab(p)
    val tabToClose = p.tabs.tabs.getOrNull(index)
    if (tabToClose != null) {
        crashRecoveryManager.discard(tabToClose.id)
    }
    val showing = p.tabs.active?.id
    p.tabs.close(index) ?: return
    if (p.tabs.isEmpty) p.tabs.open(blankTab())
    p.tabs.active?.takeIf { it.id != showing }?.let { show(it, p) }
    tabsTick.value++
    p.persist()
    prunePdfCache()
}

internal fun MainActivity.newTab(p: EditorPane = pane) {
    snapshotActiveTab(p)
    p.tabs.open(blankTab())
    p.tabs.active?.let { show(it, p) }
    tabsTick.value++
    p.persist()
}

internal fun blankTab() =
    OpenTab(TabStore.newId(), MainActivity.UNTITLED, blankDocument())

internal fun MainActivity.restoreTabs(p: EditorPane, then: () -> Unit = {}) {
    if (!p.tabs.isEmpty) {
        p.tabs.active?.let { show(it, p) }
        then()
        return
    }
    Thread {
        val session = p.store.load()
        runOnUiThread {
            if (p.tabs.isEmpty) {
                if (session == null) {
                    p.tabs.open(blankTab())
                } else {
                    session.tabs.forEach(p.tabs::open)
                    p.tabs.select(session.activeIndex)
                }
                p.tabs.active?.let { show(it, p) }
                tabsTick.value++
            }
            then()
        }
    }.start()
}

internal fun MainActivity.persistTabs() {
    mirrors.flush()
    panes.forEach { it.persist() }
    prunePdfCache()
}

internal fun MainActivity.prunePdfCache() {
    val live = panes.flatMap { p ->
        p.tabs.tabs.map(OpenTab::pdfPath) + listOf(p.surface?.pdfSourceFile()?.absolutePath)
    }
    val liveImages = panes.flatMap { p ->
        p.surface?.imageSources()?.values?.map(File::getAbsolutePath).orEmpty()
    }
    io.prune(live, liveImages)
}

internal fun MainActivity.toggleSplitView() {
    val on = !splitView.value
    if (!on) {
        panes.forEach { snapshotActiveTab(it); it.persist() }
        activePane.value = 0
    }
    splitView.value = on
}

internal fun MainActivity.displayName(uri: Uri): String = io.displayName(uri, MainActivity.UNTITLED)