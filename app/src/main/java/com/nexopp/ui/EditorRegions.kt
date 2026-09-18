// Ruta: app/src/main/java/com/nexopp/ui/EditorRegions.kt
package com.nexopp.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import com.nexopp.render.insertElements
import com.nexopp.render.insertTextElement
import androidx.compose.foundation.background
import com.nexopp.stem.FunctionPlotterDialog
import com.nexopp.stem.ScientificCalculatorDialog
import com.nexopp.stem.UnitConverterDialog
import com.nexopp.stem.TechnicalSymbolsDialog
import com.nexopp.stem.PeriodicTableDialog
import com.nexopp.stem.TableDialog
import com.nexopp.audio.AudioRecordingDialog
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.nexopp.render.BarrelDoubleAction
import com.nexopp.render.DrawingSurfaceView
import com.nexopp.render.PlaceKind
import com.nexopp.render.Placement
import com.nexopp.render.SearchStatus
import com.nexopp.ui.theme.rememberCanvasChromeColors
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import com.nexopp.document.structure.DocumentStructureDialog
import com.nexopp.document.structure.DocumentStructureStore
import com.nexopp.document.attachments.AttachmentDialog
import com.nexopp.document.attachments.AttachmentStore
import java.io.File

@Composable
fun UnifiedTopBar(
    ui: EditorUiState,
    pane: PaneState,
    tabs: TabsUiState,
    settings: AppSettings,
    onSettingsChange: (AppSettings) -> Unit,
    audio: AudioUiState,
    onOpen: () -> Unit,
    onNewTab: () -> Unit,
    onSave: () -> Unit,
    onSaveAs: () -> Unit,
    onImportPdf: () -> Unit,
    onExportPdf: () -> Unit,
    onSettings: () -> Unit,
    splitView: Boolean,
    onToggleSplitView: () -> Unit,
    onExit: () -> Unit,
    onShareExport: (com.nexopp.io.ExportManager.ExportFormat, List<Int>, Float) -> Unit = { _, _, _ -> },
    onSaveExport: (com.nexopp.io.ExportManager.ExportFormat, String, List<Int>, Float) -> Unit = { _, _, _, _ -> },
    onPickAttachment: () -> Unit = {},
    onPickImage: (com.nexopp.render.Placement) -> Unit = {}
) {
    val context = LocalContext.current
    val surface = pane.surface
    val currentNotebookName = tabs.titles.getOrNull(tabs.activeIndex)?.ifBlank { "apuntes.xopp" } ?: "apuntes.xopp"
    val structureStore = remember(context) { DocumentStructureStore(File(context.filesDir, "notebooks")) }
    val attachmentStore = remember(context) { AttachmentStore(File(context.filesDir, "notebooks")) }

    var showPageManager by remember { mutableStateOf(false) }
    var showStructureDialog by remember { mutableStateOf(false) }
    var showAttachmentDialog by remember { mutableStateOf(false) }
    var showFunctionPlotter by remember { mutableStateOf(false) }
    var showScientificCalculator by remember { mutableStateOf(false) }
    var showTableDialog by remember { mutableStateOf(false) }
    var showInsertLinkDialog by remember { mutableStateOf(false) }
    var showAudioDialog by remember { mutableStateOf(false) }
    var showUnitConverter by remember { mutableStateOf(false) }
    var showTechnicalSymbols by remember { mutableStateOf(false) }
    var showPeriodicTable by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }

    CategorizedTopBar(
        ui = ui,
        pane = pane,
        settings = settings,
        onSettingsChange = onSettingsChange,
        audio = audio,
        onExit = onExit,
        onAddPageQuick = { surface?.addPage() },
        onOpenPageManager = { showPageManager = true },
        onOpenStructure = { showStructureDialog = true },
        onOpenAttachments = { showAttachmentDialog = true },
        onOpenFunctionPlotter = { showFunctionPlotter = true },
        onOpenScientificCalculator = { showScientificCalculator = true },
        onOpenTableDialog = { showTableDialog = true },
        onOpenTechnicalSymbols = { showTechnicalSymbols = true },
        onOpenPeriodicTable = { showPeriodicTable = true },
        onOpenExportDialog = { showExportDialog = true },
        onOpenInsertLinkDialog = { showInsertLinkDialog = true },
        onOpenSettings = onSettings,
        onOpenDocument = onOpen,
        onNewDocument = onNewTab,
        onSaveDocument = onSave,
        onSaveAsDocument = onSaveAs,
        onImportPdf = onImportPdf,
        splitView = splitView,
        onToggleSplitView = onToggleSplitView,
        onPickImage = { onPickImage(com.nexopp.render.Placement(pane.currentPage, 100.0, 100.0)) }
    )

    if (showPageManager) {
        PageManagerDialog(
            visible = true,
            surface = surface,
            currentPage = pane.currentPage,
            onDismiss = { showPageManager = false },
            onGoToPage = { surface?.goToPage(it) },
            onOpenStructure = { showStructureDialog = true },
            onOpenAttachments = { showAttachmentDialog = true }
        )
    }

    if (showStructureDialog) {
        DocumentStructureDialog(
            initialStructure = structureStore.loadStructure(currentNotebookName),
            totalPages = surface?.toDocument()?.pages?.size ?: pane.pageCount.coerceAtLeast(1),
            currentPageIndex = pane.currentPage,
            onDismiss = { showStructureDialog = false },
            onSaveStructure = { structureStore.saveStructure(currentNotebookName, it) },
            onNavigateToPage = { pageIndex ->
                surface?.goToPage(pageIndex)
                showStructureDialog = false
            },
            onInsertLinkElement = { label, targetPageIndex ->
                surface?.insertTextElement(
                    text = "🔗 $label → Pág. ${targetPageIndex + 1}",
                    x = 80.0,
                    y = 120.0
                )
                showStructureDialog = false
            }
        )
    }

    if (showAttachmentDialog) {
        AttachmentDialog(
            notebookFileName = currentNotebookName,
            attachmentStore = attachmentStore,
            currentPageIndex = pane.currentPage,
            onDismiss = { showAttachmentDialog = false },
            onPickFileToAttach = onPickAttachment
        )
    }

    if (showFunctionPlotter) {
        FunctionPlotterDialog(
            originX = 250.0,
            originY = 250.0,
            onDismiss = { showFunctionPlotter = false },
            onInsertPlot = { elements ->
                surface?.insertElements(elements)
            }
        )
    }

    if (showScientificCalculator) {
        ScientificCalculatorDialog(
            onDismiss = { showScientificCalculator = false },
            onInsertText = { text ->
                surface?.insertTextElement(text)
            }
        )
    }

    if (showUnitConverter) {
        UnitConverterDialog(
            onDismiss = { showUnitConverter = false },
            onInsertText = { text ->
                surface?.insertTextElement(text)
            }
        )
    }

    if (showTechnicalSymbols) {
        TechnicalSymbolsDialog(
            onDismiss = { showTechnicalSymbols = false },
            onInsertSymbol = { sym, asLatex ->
                if (asLatex) {
                    ui.texPlacement = Placement(pane.currentPage, 100.0, 100.0)
                } else {
                    surface?.insertTextElement(sym.char)
                }
            }
        )
    }

    if (showPeriodicTable) {
        val targetPage = pane.currentPage
        PeriodicTableDialog(
            onDismiss = { showPeriodicTable = false },
            onInsertElement = { element ->
                surface?.insertElements(listOf(element), pageIndex = targetPage)
                showPeriodicTable = false
            }
        )
    }

    if (showExportDialog) {
        ExportDialog(
            currentPageNo = pane.currentPage,
            totalPageCount = surface?.toDocument()?.pages?.size ?: pane.pageCount.coerceAtLeast(1),
            defaultTitle = tabs.titles.getOrNull(tabs.activeIndex)?.ifBlank { "apuntes" } ?: "apuntes",
            onDismiss = { showExportDialog = false },
            onShare = { format, indices, scale ->
                showExportDialog = false
                onShareExport(format, indices, scale)
            },
            onSaveToStorage = { format, filename, indices, scale ->
                showExportDialog = false
                onSaveExport(format, filename, indices, scale)
            }
        )
    }

    if (showTableDialog) {
        TableDialog(
            originX = 80.0,
            originY = 120.0,
            onDismiss = { showTableDialog = false },
            onInsertTable = { elements ->
                surface?.insertElements(elements)
            }
        )
    }

    if (showInsertLinkDialog) {
        val targetPage = pane.currentPage
        InsertLinkDialog(
            onDismiss = { showInsertLinkDialog = false },
            onInsertLink = { url, title, asCard ->
                val displayTitle = if (title.isNotBlank()) title else url
                if (asCard) {
                    val card = LinkCardVisualBuilder.buildLinkCard(
                        x = 80.0,
                        y = 120.0,
                        url = url,
                        title = displayTitle,
                        siteName = null,
                        mediaType = if (url.contains("youtube.com") || url.contains("youtu.be")) LinkMediaType.VIDEO else LinkMediaType.GENERIC
                    )
                    surface?.insertElements(listOf(card), pageIndex = targetPage)
                } else {
                    surface?.insertTextElement(
                        text = "🔗 $displayTitle\n$url",
                        x = 80.0,
                        y = 120.0,
                        pageIndex = targetPage
                    )
                }
                showInsertLinkDialog = false
            }
        )
    }

    if (showAudioDialog) {
        AudioRecordingDialog(
            notebookTitle = currentNotebookName,
            isRecording = audio.recording,
            folderChosen = audio.folderChosen,
            folderUri = audio.folderUri,
            onChooseFolder = audio.onChooseFolder,
            onDismiss = { showAudioDialog = false },
            onStartRecording = { customName, _, customUri ->
                audio.onStartRecordingNamed(customName, customUri)
            },
            onStopRecording = { customName, customUri ->
                audio.onStopRecordingAndSave(customName, customUri)
                showAudioDialog = false
            }
        )
    }

}

@Composable
internal fun SearchControls(pane: PaneState) {
    fun apply(status: SearchStatus) {
        pane.searchCurrent = status.current
        pane.searchTotal = status.total
    }
    if (!pane.searchOpen) {
        IconButton(
            onClick = {
                pane.searchOpen = true
                pane.surface?.setSearchQuery(pane.searchQuery)?.let(::apply)
            },
            modifier = Modifier.size(40.dp)
        ) {
            Icon(Icons.Filled.Search, contentDescription = "Buscar", modifier = Modifier.size(22.dp))
        }
        return
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        CompactSearchField(
            value = pane.searchQuery,
            onValueChange = {
                pane.searchQuery = it
                pane.surface?.setSearchQuery(it)?.let(::apply) ?: apply(SearchStatus())
            },
        )
        Text(
            "${pane.searchCurrent}/${pane.searchTotal}",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 6.dp)
        )
        CompactIconButton(
            contentDescription = "Coincidencia anterior",
            enabled = pane.searchTotal > 0,
            onClick = { pane.surface?.previousSearchHit()?.let(::apply) },
        ) { Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Coincidencia anterior", modifier = Modifier.size(20.dp)) }
        CompactIconButton(
            contentDescription = "Siguiente coincidencia",
            enabled = pane.searchTotal > 0,
            onClick = { pane.surface?.nextSearchHit()?.let(::apply) },
        ) { Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Siguiente coincidencia", modifier = Modifier.size(20.dp)) }
        CompactIconButton(contentDescription = "Cerrar búsqueda", onClick = {
            pane.searchOpen = false
            pane.searchQuery = ""
            pane.surface?.clearSearch()?.let(::apply) ?: apply(SearchStatus())
        }) { Icon(Icons.Filled.Close, contentDescription = "Cerrar búsqueda", modifier = Modifier.size(20.dp)) }
    }
}

@Composable
private fun CompactIconButton(
    contentDescription: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(36.dp)
            .alpha(if (enabled) 1f else 0.38f)
            .semantics { this.contentDescription = contentDescription }
            .clickable(enabled = enabled, onClick = onClick),
    ) {
        icon()
    }
}

@Composable
private fun CompactSearchField(value: String, onValueChange: (String) -> Unit) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = TextStyle(fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface),
        modifier = Modifier
            .width(190.dp)
            .height(38.dp)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f), RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        decorationBox = { inner ->
            Box(contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) Text("Buscar en cuaderno...", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                inner()
            }
        },
    )
}

internal fun redefineCustomColor(
    newColor: Int,
    ui: EditorUiState,
    surface: DrawingSurfaceView?,
    settings: AppSettings,
    onSettingsChange: (AppSettings) -> Unit,
) {
    ui.color = newColor
    surface?.colorArgb = newColor
    onSettingsChange(settings.copy(customColor = newColor).withColorUsed(newColor))
}

internal fun redefineWidthSlot(
    i: Int,
    newPt: Float,
    ui: EditorUiState,
    surface: DrawingSurfaceView?,
    settings: AppSettings,
    onSettingsChange: (AppSettings) -> Unit,
) {
    val old = settings.penWidths[i]
    val slots = settings.penWidths.toMutableList().also { it[i] = newPt }
    val active = ui.width == old
    onSettingsChange(
        settings.copy(penWidths = slots, lastWidth = if (active) newPt else settings.lastWidth)
    )
    if (active) { ui.width = newPt; surface?.baseWidthPt = newPt }
}

internal fun applyFill(
    fill: Int?,
    surface: DrawingSurfaceView?,
    settings: AppSettings,
    onSettingsChange: (AppSettings) -> Unit,
) {
    surface?.currentFill = fill
    onSettingsChange(
        settings.copy(fillEnabled = fill != null, fillAlpha = fill ?: settings.fillAlpha)
    )
}

@Composable
internal fun toolbarLayerCallbacks(surface: DrawingSurfaceView?, pane: PaneState): ToolbarLayerCallbacks =
    ToolbarLayerCallbacks(
        layers = pane.layers,
        hasSelection = pane.hasSelection,
        onAddLayer = { surface?.addLayer() },
        onDeleteLayer = { i -> surface?.deleteLayer(i) },
        onMergeLayerDown = { i -> surface?.mergeLayerDown(i) },
        onRenameLayer = { i, name -> surface?.renameLayer(i, name) },
        onMoveLayer = { from, to -> surface?.moveLayer(from, to) },
        onActivateLayer = { surface?.setActiveLayer(it) },
        onToggleLayerHidden = { i, visible -> surface?.setLayerHidden(i, visible) },
        onMoveSelectionToLayer = { surface?.moveSelectionToLayer(it) },
    )

@Composable
internal fun toolbarPagesCallbacks(
    pane: PaneState,
    settings: AppSettings,
    onSettingsChange: (AppSettings) -> Unit,
    surface: DrawingSurfaceView?,
): ToolbarPagesCallbacks =
    ToolbarPagesCallbacks(
        pageCount = pane.pageCount,
        currentPage = pane.currentPage,
        onAddPage = { surface?.addPage() },
        onRemovePage = { surface?.removePage() },
        onGoToPage = { surface?.goToPage(it) },
        pageSize = pane.pageSize,
        onPageSize = { w, h -> surface?.setPageSize(w, h) },
        pageColumns = settings.pageColumns,
        onPageColumns = {
            surface?.setColumns(it)
            onSettingsChange(settings.copy(pageColumns = it))
        },
        pagesEditMode = pane.pagesEditMode,
        onPagesEditMode = { pane.pagesEditMode = it; surface?.setPagesEditMode(it) },
        selectedPages = pane.selectedPages,
        onDeleteSelectedPages = { surface?.deleteSelectedPages() },
        onClearPageSelection = { surface?.clearPageSelection() },
        copiedPages = pane.copiedPages,
        onCopySelectedPages = { surface?.copySelectedPages() },
        onPastePages = { surface?.pasteCopiedPages() },
    )

private fun DrawingSurfaceView.applyInitialStyle(ui: EditorUiState, settings: AppSettings) {
    applyTool(ui.tool)
    applySettings(settings)
    colorArgb = ui.color
    baseWidthPt = ui.width
    currentLineStyle = ui.lineStyle
    currentFill = settings.currentFill
}

private fun DrawingSurfaceView.bindTo(state: PaneState) {
    onLayersChanged = {
        state.layers = visibleLayers()
        state.backgroundStyle = visiblePageBackgroundStyle()
        state.pageSize = visiblePageSize()
    }
    state.layers = visibleLayers()
    state.backgroundStyle = visiblePageBackgroundStyle()
    state.pageSize = visiblePageSize()
    onHistoryChanged = { u, r -> state.canUndo = u; state.canRedo = r }
    onZoomChanged = { z -> state.zoom = z }
    onPageCountChanged = { n -> state.pageCount = n }
    onPageSelectionChanged = { n -> state.selectedPages = n }
    onPageClipboardChanged = { n -> state.copiedPages = n }
    onCurrentPageChanged = { page ->
        state.currentPage = page
        state.backgroundStyle = visiblePageBackgroundStyle()
        state.pageSize = visiblePageSize()
    }
    onScrollChanged = { y, total, vp -> state.scrollY = y; state.contentHeight = total; state.viewportHeight = vp }
    onSelectionChanged = { s -> state.hasSelection = s }
    onTextSelectionChanged = { s -> state.hasTextSelection = s }
    onClipboardChanged = { c -> state.hasClipboard = c }
    onBackgroundRegionChanged = { r -> state.hasBackgroundRegion = r }
    onSplineChanged = { n -> state.splineNodes = n }
    onSearchChanged = { s -> state.searchCurrent = s.current; state.searchTotal = s.total }
}

private fun DrawingSurfaceView.bindEditorActions(
    ui: EditorUiState,
    index: Int,
    onActivePane: (Int) -> Unit,
    onPickImage: (Placement) -> Unit,
    getSettings: () -> AppSettings,
    onSettingsChange: (AppSettings) -> Unit,
) {
    onToggleFullPage = { ui.fullPage = !ui.fullPage }
    onBarrelDoubleClick = { action ->
        when (action) {
            BarrelDoubleAction.TOGGLE_ERASER -> ui.toggleTool(EditorTool.ERASER)
            BarrelDoubleAction.TOGGLE_SELECT -> ui.toggleTool(EditorTool.SELECT)
            BarrelDoubleAction.TOGGLE_FULL_PAGE -> ui.fullPage = !ui.fullPage
            else -> Unit
        }
        applyTool(ui.tool)
    }
    onPaletteAction = { action ->
        applyPaletteAction(action, ui, this, getSettings(), onSettingsChange)
    }
    onPlace = { kind, placement ->
        onActivePane(index)
        when (kind) {
            PlaceKind.TEXT -> ui.textPlacement = placement
            PlaceKind.TEX -> ui.texPlacement = placement
            PlaceKind.IMAGE -> onPickImage(placement)
        }
    }
    onElementTapped = { element, pageIndex, _, _ ->
        onActivePane(index)
        when (element) {
            is com.nexopp.format.model.TextElement -> {
                ui.textPlacement = Placement(pageIndex, element.x, element.y, element)
            }
            is com.nexopp.format.model.TexImageElement -> {
                ui.texPlacement = Placement(pageIndex, element.left, element.top)
            }
            is com.nexopp.format.model.ImageElement -> {
                val stemType = element.extraAttrs["stem"]
                when {
                    stemType == "link_card" -> {
                        val url = element.extraAttrs["url"] ?: ""
                        val title = element.extraAttrs["title"] ?: ""
                        val mediaType = element.extraAttrs["mediaType"] ?: "GENERIC"
                        ui.activeLinkCard = Triple(url, title, mediaType)
                        ui.activeLinkCardElement = Pair(pageIndex, element)
                    }
                    stemType == "periodic_element_card" || stemType == "periodic_table_full" -> {
                        ui.showPeriodicTableDialog = true
                    }
                    stemType == "function_plot" -> {
                        ui.showFunctionPlotterDialog = true
                    }
                    stemType?.startsWith("table") == true -> {
                        ui.showTableEditDialog = true
                    }
                }
            }
            is com.nexopp.format.model.Stroke -> {
                val stemType = element.extraAttrs["stem"]
                if (stemType?.startsWith("table") == true) {
                    ui.showTableEditDialog = true
                }
            }
            else -> Unit
        }
    }
}

@Composable
fun EditorPaneView(
    index: Int,
    ui: EditorUiState,
    settings: AppSettings,
    onSettingsChange: (AppSettings) -> Unit,
    tabs: List<TabsUiState>,
    onActivePane: (Int) -> Unit,
    onSurfaceCreated: (Int, DrawingSurfaceView) -> Unit,
    onPickImage: (Placement) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state = ui.panes[index]
    val chrome = rememberCanvasChromeColors()
    val latestSettings = rememberUpdatedState(settings)
    Column(
        modifier = modifier.pointerInput(index) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                onActivePane(index)
            }
        },
    ) {
        if (!ui.fullPage) TabStrip(tabs[index.coerceIn(tabs.indices)], modifier = Modifier.fillMaxWidth())
        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            AndroidView(
                factory = { ctx ->
                    DrawingSurfaceView(ctx).also {
                        it.applyInitialStyle(ui, settings)
                        it.bindTo(state)
                        it.bindEditorActions(ui, index, onActivePane, onPickImage, { latestSettings.value }, onSettingsChange)
                        state.surface = it
                        onSurfaceCreated(index, it)
                    }
                },
                update = { it.applyChromeColors(chrome.backdrop, chrome.selection, chrome.guide) },
                modifier = Modifier.fillMaxSize(),
            )
            ScrollThumb(
                scrollY = state.scrollY,
                totalHeightPx = state.contentHeight,
                viewportPx = state.viewportHeight,
                currentPage = state.currentPage,
                pageCount = state.pageCount,
                onScrollTo = { state.surface?.scrollToY(it) },
                modifier = Modifier.matchParentSize(),
            )
            PageCounter(
                currentPage = state.currentPage,
                pageCount = state.pageCount,
                modifier = Modifier
                    .align(pageCounterAlignment(settings.pageCounterVertical, settings.pageCounterHorizontal))
                    .padding(8.dp),
            )
        }
    }
}

@Composable
internal fun OverflowMenu(
    onOpen: () -> Unit,
    onNewTab: () -> Unit,
    onSave: () -> Unit,
    onSaveAs: () -> Unit,
    onImportPdf: () -> Unit,
    onExportPdf: () -> Unit,
    onSettings: () -> Unit,
    splitView: Boolean,
    onToggleSplitView: () -> Unit,
    onOpenStructure: () -> Unit = {},
    onOpenAttachments: () -> Unit = {},
    onAddPage: () -> Unit = {},
) {
    var open by remember { mutableStateOf(false) }
    IconButton(onClick = { open = true }) {
        Icon(Icons.Filled.Menu, contentDescription = "Menú")
    }
    DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
        DropdownMenuItem(
            text = { Text("Añadir página inmediatamente") },
            leadingIcon = { Icon(Icons.Filled.PostAdd, contentDescription = null) },
            onClick = { open = false; onAddPage() },
        )
        DropdownMenuItem(
            text = { Text("Abrir") },
            leadingIcon = { Icon(Icons.Filled.FileOpen, contentDescription = null) },
            onClick = { open = false; onOpen() },
        )
        DropdownMenuItem(
            text = { Text("Nuevo documento") },
            leadingIcon = { Icon(Icons.AutoMirrored.Filled.NoteAdd, contentDescription = null) },
            onClick = { open = false; onNewTab() },
        )
        DropdownMenuItem(
            text = { Text("Índice y Estructura") },
            leadingIcon = { Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null) },
            onClick = { open = false; onOpenStructure() },
        )
        DropdownMenuItem(
            text = { Text("Archivos Adjuntos") },
            leadingIcon = { Icon(Icons.Filled.AttachFile, contentDescription = null) },
            onClick = { open = false; onOpenAttachments() },
        )
        DropdownMenuItem(
            text = { Text("Importar PDF") },
            leadingIcon = { Icon(Icons.Filled.PictureAsPdf, contentDescription = null) },
            onClick = { open = false; onImportPdf() },
        )
        DropdownMenuItem(
            text = { Text("Exportar a PDF") },
            leadingIcon = { Icon(Icons.Filled.PictureAsPdf, contentDescription = null) },
            onClick = { open = false; onExportPdf() },
        )
        DropdownMenuItem(
            text = { Text("Guardar") },
            leadingIcon = { Icon(Icons.Filled.Save, contentDescription = null) },
            onClick = { open = false; onSave() },
        )
        DropdownMenuItem(
            text = { Text("Guardar como…") },
            leadingIcon = { Icon(Icons.Filled.SaveAs, contentDescription = null) },
            onClick = { open = false; onSaveAs() },
        )
        DropdownMenuItem(
            text = { Text(if (splitView) "Cerrar vista dividida" else "Vista dividida") },
            leadingIcon = { Icon(Icons.Filled.VerticalSplit, contentDescription = null) },
            onClick = { open = false; onToggleSplitView() },
        )
        DropdownMenuItem(
            text = { Text("Ajustes") },
            leadingIcon = { Icon(Icons.Filled.Settings, contentDescription = null) },
            onClick = { open = false; onSettings() },
        )
    }
}