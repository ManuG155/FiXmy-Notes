// Ruta: app/src/main/java/com/nexopp/ui/EditorScreen.kt
package com.nexopp.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.nexopp.format.SaveFormat
import com.nexopp.format.model.Tool
import com.nexopp.render.DrawingSurfaceView
import com.nexopp.render.EraserMode
import com.nexopp.render.GuideKind
import com.nexopp.render.ImportPdfMode
import com.nexopp.render.InputSettings
import com.nexopp.render.PlaceKind
import com.nexopp.render.Placement
import com.nexopp.render.ShapeKind

fun DrawingSurfaceView.applyTool(tool: EditorTool) {
    handMode = tool == EditorTool.HAND
    selectMode = tool == EditorTool.SELECT || tool == EditorTool.LASSO_SELECT
    backgroundSelectMode = tool == EditorTool.BG_SELECT
    lassoMode = tool == EditorTool.LASSO_SELECT
    textSelectMode = tool == EditorTool.TEXT_SELECT
    verticalSpaceMode = tool == EditorTool.VERTICAL_SPACE
    audioPlayMode = tool == EditorTool.PLAY_OBJECT
    placeKind = when (tool) {
        EditorTool.TEXT -> PlaceKind.TEXT
        EditorTool.IMAGE -> PlaceKind.IMAGE
        EditorTool.TEXIMAGE -> PlaceKind.TEX
        else -> null
    }
    shapeKind = when (tool) {
        EditorTool.LINE -> ShapeKind.LINE
        EditorTool.ARROW -> ShapeKind.ARROW
        EditorTool.DOUBLE_ARROW -> ShapeKind.DOUBLE_ARROW
        EditorTool.COORDINATE_AXIS -> ShapeKind.COORDINATE_AXIS
        EditorTool.RECTANGLE -> ShapeKind.RECTANGLE
        EditorTool.ELLIPSE -> ShapeKind.ELLIPSE
        EditorTool.SPLINE -> ShapeKind.SPLINE
        else -> null
    }
    if (tool == EditorTool.ERASER || tool == EditorTool.ERASER_WHOLE) {
        eraserMode =
            if (tool == EditorTool.ERASER_WHOLE) EraserMode.WHOLE_STROKE else EraserMode.STANDARD
    }
    when (tool) {
        EditorTool.PEN -> this.tool = Tool.PEN
        EditorTool.HIGHLIGHTER -> this.tool = Tool.HIGHLIGHTER
        EditorTool.ERASER, EditorTool.ERASER_WHOLE -> this.tool = Tool.ERASER
        EditorTool.LINE, EditorTool.ARROW, EditorTool.DOUBLE_ARROW, EditorTool.COORDINATE_AXIS,
        EditorTool.RECTANGLE, EditorTool.ELLIPSE, EditorTool.SPLINE,
        -> this.tool = Tool.PEN
        else -> Unit 
    }
}

fun DrawingSurfaceView.applySettings(s: AppSettings) {
    inputSettings = InputSettings(
        fingerDraws = s.fingerDraws,
        strictPalmRejection = s.strictPalmRejection,
        barrelAction = s.barrelAction,
        secondaryBarrelAction = s.secondaryBarrelAction,
        barrelDoubleAction = s.barrelDoubleAction,
        paletteInvocation = s.paletteInvocation,
    )
    showHover = s.showHover
    paletteHaptics = s.paletteHaptics
    paletteCloseOnSelect = s.paletteCloseOnSelect
    pressureGamma = s.sensitivity.gamma
    strokePrecision = s.strokePrecision
    recognizeShapes = s.recognizeShapes
    setColumns(s.pageColumns)
    snapToGrid = s.snapToGrid
    snapRotation = s.snapRotation
    if (s.guideKind == GuideKind.NONE) {
        if (guide != null) placeGuide(GuideKind.NONE)
    } else if (guide == null) {
        placeGuide(s.guideKind)
    }
    flingStrength = s.momentum
    momentumCurve = s.momentumCurve
    panSensitivity = s.panSensitivity
    palette = s.radialPalette
    presetColors = s.presets.associate { it.id to it.colorArgb }
}

@Composable
fun EditorScreen(
    onOpen: () -> Unit,
    onSave: () -> Unit,
    onSaveAs: (filename: String, format: SaveFormat) -> Unit,
    currentSaveFormat: () -> SaveFormat,
    onImportPdf: (ImportPdfMode) -> Unit,
    onExportPdf: () -> Unit,
    onPickImage: (Placement) -> Unit,
    onSurfaceCreated: (Int, DrawingSurfaceView) -> Unit,
    settings: AppSettings,
    onSettingsChange: (AppSettings) -> Unit,
    audio: AudioUiState = AudioUiState(),
    tabs: List<TabsUiState> = listOf(TabsUiState()),
    splitView: Boolean = false,
    onToggleSplitView: () -> Unit = {},
    activePane: Int = 0,
    onActivePane: (Int) -> Unit = {},
    busy: String? = null,
    onExit: () -> Unit = {},
    onShareExport: (com.nexopp.io.ExportManager.ExportFormat, List<Int>, Float) -> Unit = { _, _, _ -> },
    onSaveExport: (com.nexopp.io.ExportManager.ExportFormat, String, List<Int>, Float) -> Unit = { _, _, _, _ -> },
    onPickAttachment: () -> Unit = {}
) {
    val ui = rememberEditorUiState(settings)
    val pane = ui.pane(activePane)

    EditorBackHandler(ui = ui, pane = pane, busy = busy != null, onExit = onExit)

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (!ui.fullPage) {
                UnifiedTopBar(
                    ui = ui,
                    pane = pane,
                    tabs = tabs[activePane.coerceIn(tabs.indices)],
                    settings = settings,
                    onSettingsChange = onSettingsChange,
                    audio = audio,
                    onOpen = onOpen,
                    onNewTab = { tabs[activePane.coerceIn(tabs.indices)].onNew() },
                    onSave = onSave,
                    onSaveAs = { ui.showSaveAs = true },
                    onImportPdf = { ui.showImportPdf = true },
                    onExportPdf = onExportPdf,
                    onSettings = { ui.showSettings = true },
                    splitView = splitView,
                    onToggleSplitView = onToggleSplitView,
                    onExit = onExit,
                    onShareExport = onShareExport,
                    onSaveExport = onSaveExport,
                    onPickAttachment = onPickAttachment,
                    onPickImage = onPickImage
                )
            }

            EditorBody(
                ui = ui,
                pane = pane,
                settings = settings,
                onSettingsChange = onSettingsChange,
                tabs = tabs,
                splitView = splitView,
                onToggleSplitView = onToggleSplitView,
                onActivePane = onActivePane,
                onSurfaceCreated = onSurfaceCreated,
                onPickImage = onPickImage,
                modifier = Modifier.fillMaxWidth().weight(1f)
            )
        }

        LaunchedEffect(settings) { ui.panes.forEach { it.surface?.applySettings(settings) } }

        if (ui.showSettings) {
            SettingsScreen(
                settings = settings,
                onChange = onSettingsChange,
                onBack = { ui.showSettings = false },
            )
        }

        if (busy != null) TransferOverlay(busy)

        EditorOverlays(
            ui = ui,
            pane = pane,
            settings = settings,
            onSettingsChange = onSettingsChange,
            currentSaveFormat = currentSaveFormat,
            onSaveAs = onSaveAs,
            onImportPdf = onImportPdf,
        )
    }
}

@Composable
private fun EditorBody(
    ui: EditorUiState,
    pane: PaneState,
    settings: AppSettings,
    onSettingsChange: (AppSettings) -> Unit,
    tabs: List<TabsUiState>,
    splitView: Boolean,
    onToggleSplitView: () -> Unit = {},
    onActivePane: (Int) -> Unit,
    onSurfaceCreated: (Int, DrawingSurfaceView) -> Unit,
    onPickImage: (Placement) -> Unit,
    modifier: Modifier = Modifier,
) {
    val paneAt: @Composable (Int, Modifier) -> Unit = { index, paneModifier ->
        EditorPaneView(
            index = index,
            ui = ui,
            settings = settings,
            onSettingsChange = onSettingsChange,
            tabs = tabs,
            onActivePane = onActivePane,
            onSurfaceCreated = onSurfaceCreated,
            onPickImage = onPickImage,
            modifier = paneModifier,
        )
    }

    Box(modifier = modifier) {
        if (splitView) {
            SplitLayout(
                fraction = ui.splitFraction,
                onFraction = { ui.splitFraction = it },
                onClose = onToggleSplitView,
                modifier = Modifier.fillMaxSize(),
                first = { paneAt(0, it) },
                second = { paneAt(1, it) },
            )
        } else {
            paneAt(0, Modifier.fillMaxSize())
        }
    }
}