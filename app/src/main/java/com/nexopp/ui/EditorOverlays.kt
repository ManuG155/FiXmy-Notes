// Ruta: app/src/main/java/com/nexopp/ui/EditorOverlays.kt
package com.nexopp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.nexopp.format.SaveFormat
import com.nexopp.recognition.OcrReviewDialog
import com.nexopp.render.ImportPdfMode
import com.nexopp.render.captureBackgroundRegion
import com.nexopp.render.clearBackgroundRegion
import com.nexopp.render.clearSelection
import com.nexopp.render.cutBackgroundRegion
import com.nexopp.render.clearTextSelection
import com.nexopp.render.copySelection
import com.nexopp.render.copyTextSelection
import com.nexopp.render.cancelSpline
import com.nexopp.render.cutSelection
import com.nexopp.render.deleteElement
import com.nexopp.render.deleteSelection
import com.nexopp.render.duplicateSelection
import com.nexopp.render.finishSpline
import com.nexopp.render.getSelectedStrokes
import com.nexopp.render.insertMultipleImages
import com.nexopp.render.insertTextAdjacentToSelection
import com.nexopp.render.insertTextElement
import com.nexopp.render.pasteClipboard
import com.nexopp.render.recognizeSelectedStrokes
import com.nexopp.render.replaceSelectionWithText
import com.nexopp.render.restyleSelection
import com.nexopp.render.undoLastSplineNode
import androidx.compose.ui.platform.LocalContext

@Composable
fun BoxScope.EditorOverlays(
    ui: EditorUiState,
    pane: PaneState,
    settings: AppSettings,
    onSettingsChange: (AppSettings) -> Unit,
    currentSaveFormat: () -> SaveFormat,
    onSaveAs: (filename: String, format: SaveFormat) -> Unit,
    onImportPdf: (ImportPdfMode) -> Unit,
) {
    val surface = pane.surface
    val palette = rememberColorPaletteState(settings, onSettingsChange)
    val barModifier = Modifier.align(Alignment.BottomCenter).padding(24.dp)

    SelectionOverlays(ui, pane, palette, barModifier, settings, onSettingsChange)

    ui.textPlacement?.let { placement ->
        val existing = placement.existing
        val defaults = ui.textDefaults
        TextBoxDialog(
            title = if (existing != null) "Editar texto" else "Añadir texto",
            initialContent = existing?.content ?: "",
            initialFamily = existing?.let { com.nexopp.format.FontDescription.parse(it.font) }?.family ?: defaults.family,
            initialBold = existing?.let { com.nexopp.format.FontDescription.parse(it.font) }?.bold ?: defaults.bold,
            initialItalic = existing?.let { com.nexopp.format.FontDescription.parse(it.font) }?.italic ?: defaults.italic,
            initialUnderline = existing?.let { it.extraAttrs["underline"] == "true" } ?: defaults.underline,
            initialSize = existing?.size ?: defaults.size,
            initialColor = existing?.color ?: ui.color,
            palette = palette,
            onConfirm = { content, family, bold, italic, underline, sizePt, colorArgb ->
                val extras = if (underline) mapOf("underline" to "true") else emptyMap()
                surface?.insertText(
                    placement,
                    content,
                    com.nexopp.format.FontDescription(family, bold, italic).compose(),
                    sizePt,
                    colorArgb,
                    extras,
                )
                if (existing == null) {
                    defaults.family = family; defaults.bold = bold; defaults.italic = italic
                    defaults.underline = underline
                    defaults.size = sizePt; defaults.color = colorArgb
                }
                ui.textPlacement = null
            },
            onDismiss = { surface?.cancelTextEdit(); ui.textPlacement = null },
        )
    }
    ui.texPlacement?.let { placement ->
        LatexInputDialog(
            title = "Ecuación LaTeX",
            initial = "",
            confirmLabel = "Insertar",
            onConfirm = { latex -> surface?.insertTex(placement, latex, ui.color); ui.texPlacement = null },
            onDismiss = { ui.texPlacement = null },
        )
    }
    ui.activeLinkCard?.let { (url, title, mediaType) ->
        LinkCardOptionsDialog(
            url = url,
            title = title,
            mediaType = mediaType,
            onDismiss = { ui.activeLinkCard = null; ui.activeLinkCardElement = null },
            onDelete = {
                ui.activeLinkCardElement?.let { (pIdx, elem) ->
                    surface?.deleteElement(elem, pIdx)
                }
                ui.activeLinkCard = null
                ui.activeLinkCardElement = null
            }
        )
    }
    if (ui.showPeriodicTableDialog) {
        val targetPage = pane.currentPage
        com.nexopp.stem.PeriodicTableDialog(
            onDismiss = { ui.showPeriodicTableDialog = false },
            onInsertElement = { element ->
                surface?.insertElements(listOf(element), pageIndex = targetPage)
                ui.showPeriodicTableDialog = false
            }
        )
    }
    if (ui.showTableEditDialog) {
        com.nexopp.stem.TableDialog(
            originX = 80.0,
            originY = 120.0,
            onDismiss = { ui.showTableEditDialog = false },
            onInsertTable = { elements ->
                surface?.insertElements(elements)
                ui.showTableEditDialog = false
            }
        )
    }
    if (ui.showFunctionPlotterDialog) {
        com.nexopp.stem.FunctionPlotterDialog(
            originX = 250.0,
            originY = 250.0,
            onDismiss = { ui.showFunctionPlotterDialog = false },
            onInsertPlot = { elements ->
                surface?.insertElements(elements)
                ui.showFunctionPlotterDialog = false
            }
        )
    }
    if (ui.showImportPdf) {
        ImportPdfDialog(
            merging = surface?.hasPdfBackground() == true,
            onConfirm = { mode -> ui.showImportPdf = false; onImportPdf(mode) },
            onDismiss = { ui.showImportPdf = false },
        )
    }
    if (ui.showSaveAs) {
        SaveAsDialog(
            initialFormat = currentSaveFormat(),
            onConfirm = { filename, format -> ui.showSaveAs = false; onSaveAs(filename, format) },
            onDismiss = { ui.showSaveAs = false },
        )
    }

    if (ui.showBrowserDialog) {
        IntegratedBrowserDialog(
            onInsertText = { text ->
                val pIdx = surface?.visiblePageIndex() ?: 0
                val extras = if (ui.textDefaults.underline) mapOf("underline" to "true") else emptyMap()
                surface?.insertTextElement(
                    text = text,
                    sizePt = ui.textDefaults.size.toDouble(),
                    color = ui.color,
                    font = com.nexopp.format.FontDescription(ui.textDefaults.family, ui.textDefaults.bold, ui.textDefaults.italic).compose(),
                    extraAttrs = extras,
                    pageIndex = pIdx
                )
            },
            onInsertImage = { bytes ->
                val pIdx = surface?.visiblePageIndex() ?: 0
                surface?.insertMultipleImages(listOf(bytes), pIdx)
            },
            onDismiss = { ui.showBrowserDialog = false }
        )
    }

    if (ui.showSpeechToText) {
        val context = LocalContext.current
        val speechManager = remember(context) {
            com.nexopp.audio.SpeechToTextManager(context) { dictatedText ->
                val pIdx = surface?.visiblePageIndex() ?: 0
                val extras = if (ui.textDefaults.underline) mapOf("underline" to "true") else emptyMap()
                surface?.insertTextElement(
                    text = dictatedText,
                    sizePt = ui.textDefaults.size.toDouble(),
                    color = ui.color,
                    font = com.nexopp.format.FontDescription(ui.textDefaults.family, ui.textDefaults.bold, ui.textDefaults.italic).compose(),
                    extraAttrs = extras,
                    pageIndex = pIdx
                )
            }
        }
        SpeechTranscriptionDialog(
            speechManager = speechManager,
            onDismiss = { ui.showSpeechToText = false }
        )
    }
}

@Composable
private fun BoxScope.SelectionOverlays(
    ui: EditorUiState,
    pane: PaneState,
    palette: ColorPaletteState,
    barModifier: Modifier,
    settings: AppSettings,
    onSettingsChange: (AppSettings) -> Unit,
) {
    val surface = pane.surface
    var showOcrDialog by remember { mutableStateOf(false) }
    var ocrText by remember { mutableStateOf("") }
    var ocrConfidence by remember { mutableStateOf(0.9) }
    var ocrIsMath by remember { mutableStateOf(false) }

    if (showOcrDialog) {
        OcrReviewDialog(
            initialText = ocrText,
            confidence = ocrConfidence,
            isMath = ocrIsMath,
            onDismiss = { showOcrDialog = false },
            onReplaceSelection = { text, isLatex ->
                surface?.replaceSelectionWithText(text, isLatex)
                showOcrDialog = false
            },
            onInsertAdjacent = { text, isLatex ->
                surface?.insertTextAdjacentToSelection(text, isLatex)
                showOcrDialog = false
            }
        )
    }

    if (pane.hasSelection) {
        SelectionActionBar(
            onCut = { surface?.cutSelection() },
            onCopy = { surface?.copySelection() },
            onDuplicate = { surface?.duplicateSelection() },
            onDelete = { surface?.deleteSelection() },
            onRecolor = { c -> surface?.restyleSelection(c, null) },
            palette = palette,
            onReWidth = { w -> surface?.restyleSelection(null, w.toDouble()) },
            widthSlots = settings.penWidths,
            onOcr = {
                val strokes = surface?.getSelectedStrokes() ?: emptyList()
                if (strokes.isNotEmpty()) {
                    val res = surface?.recognizeSelectedStrokes(isMath = false)
                    ocrText = res?.text ?: ""
                    ocrConfidence = (res?.candidates?.firstOrNull()?.confidence ?: 0.85f).toDouble()
                    ocrIsMath = false
                    showOcrDialog = true
                }
            },
            onDeselect = { surface?.clearSelection() },
            modifier = barModifier,
        )
    } else if (ui.tool == EditorTool.SELECT || ui.tool == EditorTool.LASSO_SELECT || ui.tool == EditorTool.BG_SELECT) {
        SelectModeBar(
            canPaste = pane.hasClipboard,
            onPaste = {
                if (ui.tool != EditorTool.SELECT && ui.tool != EditorTool.LASSO_SELECT) {
                    ui.tool = EditorTool.SELECT
                    surface?.applyTool(ui.tool)
                    groupOf(EditorTool.SELECT)?.let {
                        onSettingsChange(
                            settings.copy(
                                toolGroupSelections =
                                    it.withSelection(settings.toolGroupSelections, EditorTool.SELECT),
                            ),
                        )
                    }
                }
                surface?.pasteClipboard()
            },
            modifier = barModifier,
            hasRegion = pane.hasBackgroundRegion,
            onCopyRegion = { surface?.captureBackgroundRegion() },
            onCutRegion = { surface?.cutBackgroundRegion() },
            onClearRegion = { surface?.clearBackgroundRegion() },
        )
    }
    if (pane.splineNodes > 0) {
        SplineModeBar(
            nodeCount = pane.splineNodes,
            onFinish = { surface?.finishSpline() },
            onUndoPoint = { surface?.undoLastSplineNode() },
            onCancel = { surface?.cancelSpline() },
            modifier = barModifier,
        )
    }
    if (pane.hasTextSelection) {
        TextSelectionBar(
            onCopy = { surface?.copyTextSelection() },
            onDeselect = { surface?.clearTextSelection() },
            modifier = barModifier,
        )
    }
}

@Composable
fun TransferOverlay(label: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
            .pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent() } },
        contentAlignment = Alignment.Center,
    ) {
        Surface(shape = MaterialTheme.shapes.large, tonalElevation = 4.dp) {
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp),
            ) {
                androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                Text(label)
            }
        }
    }
}