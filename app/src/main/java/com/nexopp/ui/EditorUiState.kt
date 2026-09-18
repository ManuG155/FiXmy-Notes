// --- EditorUiState.kt ---
package com.nexopp.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.nexopp.format.FontDescription
import com.nexopp.format.model.LineStyle
import com.nexopp.render.Placement

const val TEXT_SIZE_PT = 12.0
const val TEXT_SIZE_MIN = 6f
const val TEXT_SIZE_MAX = 96f

class TextDefaults {
    var family by mutableStateOf(FontDescription.DEFAULT_FAMILY)
    var bold by mutableStateOf(false)
    var italic by mutableStateOf(false)
    var underline by mutableStateOf(false)
    var size by mutableStateOf(TEXT_SIZE_PT)
    var color by mutableStateOf(PEN_COLORS.first())
}

private fun isShapeTool(t: EditorTool): Boolean = when (t) {
    EditorTool.LINE, EditorTool.ARROW, EditorTool.DOUBLE_ARROW,
    EditorTool.RECTANGLE, EditorTool.ELLIPSE, EditorTool.COORDINATE_AXIS,
    EditorTool.SPLINE -> true
    else -> false
}

class EditorUiState(
    tool: EditorTool = EditorTool.PEN,
    color: Int = AppSettings.DEFAULT_LAST_COLOR,
    width: Float = 1.5f,
    penColor: Int = if (tool == EditorTool.HIGHLIGHTER) AppSettings.DEFAULT_LAST_COLOR else color,
    penWidth: Float = if (tool == EditorTool.HIGHLIGHTER) 1.5f else width,
    highlighterColor: Int = if (tool == EditorTool.HIGHLIGHTER) color else 0xFFFFF176.toInt(),
    highlighterWidth: Float = if (tool == EditorTool.HIGHLIGHTER) width else 12.0f,
    eraserWidth: Float = if (tool == EditorTool.ERASER || tool == EditorTool.ERASER_WHOLE) width else 5.0f,
) {
    var penColor by mutableStateOf(penColor)
    var penWidth by mutableStateOf(penWidth)

    var highlighterColor by mutableStateOf(highlighterColor)
    var highlighterWidth by mutableStateOf(highlighterWidth)

    var eraserWidth by mutableStateOf(eraserWidth)

    private var _tool by mutableStateOf(tool)
    var tool: EditorTool
        get() = _tool
        set(value) {
            _tool = value
            when (value) {
                EditorTool.HIGHLIGHTER -> {
                    _color = highlighterColor
                    _width = highlighterWidth
                }
                EditorTool.ERASER, EditorTool.ERASER_WHOLE -> {
                    _width = eraserWidth
                }
                EditorTool.PEN -> {
                    _color = penColor
                    _width = penWidth
                }
                else -> Unit
            }
        }

    private var _color by mutableStateOf(
        when (tool) {
            EditorTool.HIGHLIGHTER -> highlighterColor
            else -> penColor
        }
    )
    var color: Int
        get() = _color
        set(value) {
            _color = value
            if (_tool == EditorTool.HIGHLIGHTER) {
                highlighterColor = value
            } else if (_tool == EditorTool.PEN || isShapeTool(_tool)) {
                penColor = value
            }
        }

    private var _width by mutableStateOf(
        when (tool) {
            EditorTool.HIGHLIGHTER -> highlighterWidth
            EditorTool.ERASER, EditorTool.ERASER_WHOLE -> eraserWidth
            else -> penWidth
        }
    )
    var width: Float
        get() = _width
        set(value) {
            _width = value
            when (_tool) {
                EditorTool.HIGHLIGHTER -> highlighterWidth = value
                EditorTool.ERASER, EditorTool.ERASER_WHOLE -> eraserWidth = value
                else -> penWidth = value
            }
        }

    var lineStyle by mutableStateOf(LineStyle.PLAIN)

    var showSettings by mutableStateOf(false)
    var showSaveAs by mutableStateOf(false)
    var showImportPdf by mutableStateOf(false)

    var fullPage by mutableStateOf(false)

    var splitFraction by mutableStateOf(0.5f)

    var textPlacement by mutableStateOf<Placement?>(null)
    var texPlacement by mutableStateOf<Placement?>(null)
    var activeLinkCard by mutableStateOf<Triple<String, String, String>?>(null)
    var activeLinkCardElement by mutableStateOf<Pair<Int, com.nexopp.format.model.ImageElement>?>(null)
    var showPeriodicTableDialog by mutableStateOf(false)
    var showTableEditDialog by mutableStateOf(false)
    var showFunctionPlotterDialog by mutableStateOf(false)
    var showBrowserDialog by mutableStateOf(false)
    var showSpeechToText by mutableStateOf(false)

    val textDefaults = TextDefaults()

    val panes = List(PANE_COUNT) { PaneState() }

    fun toggleTool(target: EditorTool) {
        if (tool == target) {
            tool = toolBeforeToggle ?: EditorTool.PEN
            toolBeforeToggle = null
        } else {
            toolBeforeToggle = tool
            tool = target
        }
    }

    private var toolBeforeToggle: EditorTool? = null

    fun pane(active: Int): PaneState = panes[active.coerceIn(panes.indices)]
}

@Composable
fun rememberEditorUiState(settings: AppSettings): EditorUiState = remember {
    val initialTool = startingTool(settings.defaultTool, settings.toolGroupSelections)
    EditorUiState(
        tool = initialTool,
        color = settings.lastColor,
        width = settings.lastWidth,
        penColor = settings.lastColor,
        penWidth = settings.lastWidth,
        highlighterColor = settings.lastHighlighterColor,
        highlighterWidth = settings.lastHighlighterWidth,
        eraserWidth = settings.lastEraserWidth,
    )
}