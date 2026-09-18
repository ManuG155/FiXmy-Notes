package com.nexopp.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexopp.format.FontDescription
import com.nexopp.format.model.LineStyle
import com.nexopp.render.DrawingSurfaceView
import com.nexopp.render.GuideKind
import com.nexopp.render.selectedTextElement
import com.nexopp.render.setTextFontSize
import com.nexopp.render.toggleTextBold
import com.nexopp.render.toggleTextItalic
import com.nexopp.render.toggleTextUnderline
import kotlin.math.roundToInt

enum class TopBarSection(val label: String, val icon: ImageVector) {
    ESCRITURA("Escritura", Icons.Filled.Create),
    DIBUJO("Dibujo", Icons.Filled.SquareFoot),
    MATEMATICAS("Matemáticas", Icons.Filled.Calculate),
    GRAFICAS("Gráficas", Icons.AutoMirrored.Filled.ShowChart),
    TEXTO("Texto", Icons.Filled.TextFields),
    INSERTAR("Insertar", Icons.Filled.AddCircleOutline),
    PAGINAS("Páginas", Icons.Filled.AutoStories),
    AUDIO("Audio", Icons.Filled.Mic),
    EXPORTAR("Exportar", Icons.Filled.Share),
    AJUSTES("Ajustes", Icons.Filled.Settings)
}

val TEXT_FONT_SIZES = listOf(8, 9, 10, 11, 12, 14, 16, 18, 20, 24, 28, 32, 36, 48)

@Composable
fun CategorizedTopBar(
    ui: EditorUiState,
    pane: PaneState,
    settings: AppSettings,
    onSettingsChange: (AppSettings) -> Unit,
    audio: AudioUiState,
    onExit: () -> Unit,
    onAddPageQuick: () -> Unit,
    onOpenPageManager: () -> Unit,
    onOpenStructure: () -> Unit,
    onOpenAttachments: () -> Unit,
    onOpenFunctionPlotter: () -> Unit,
    onOpenScientificCalculator: () -> Unit,
    onOpenTableDialog: () -> Unit,
    onOpenTechnicalSymbols: () -> Unit,
    onOpenPeriodicTable: () -> Unit,
    onOpenExportDialog: () -> Unit,
    onOpenInsertLinkDialog: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenDocument: () -> Unit,
    onNewDocument: () -> Unit,
    onSaveDocument: () -> Unit,
    onSaveAsDocument: () -> Unit,
    onImportPdf: () -> Unit,
    splitView: Boolean,
    onToggleSplitView: () -> Unit,
    onPickImage: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val surface = pane.surface
    var activeSection by remember { mutableStateOf(TopBarSection.ESCRITURA) }
    var isMinimized by remember { mutableStateOf(false) }

    val styleCallbacks = ToolbarStyleCallbacks(
        color = ui.color,
        onColor = {
            ui.color = it
            surface?.colorArgb = it
            if (ui.tool == EditorTool.HIGHLIGHTER) {
                onSettingsChange(settings.copy(lastHighlighterColor = it))
            } else {
                onSettingsChange(settings.withColorUsed(it).copy(lastColor = it))
            }
        },
        palette = rememberColorPaletteState(settings, onSettingsChange),
        onRedefineCustom = { newColor -> redefineCustomColor(newColor, ui, surface, settings, onSettingsChange) },
        width = ui.width,
        onWidth = {
            ui.width = it
            surface?.baseWidthPt = it
            when (ui.tool) {
                EditorTool.HIGHLIGHTER -> onSettingsChange(settings.copy(lastHighlighterWidth = it))
                EditorTool.ERASER, EditorTool.ERASER_WHOLE -> onSettingsChange(settings.copy(lastEraserWidth = it))
                else -> onSettingsChange(settings.copy(lastWidth = it))
            }
        },
        widthSlots = settings.penWidths,
        onRedefineSlot = { i, newPt -> redefineWidthSlot(i, newPt, ui, surface, settings, onSettingsChange) },
        lineStyle = ui.lineStyle,
        onLineStyle = { ui.lineStyle = it; surface?.currentLineStyle = it },
        fill = settings.currentFill,
        onFill = { applyFill(it, surface, settings, onSettingsChange) },
    )

    val layerCallbacks = toolbarLayerCallbacks(surface, pane)

    Surface(
        tonalElevation = 2.dp,
        shadowElevation = 3.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Fila 1: Barra Superior Principal con Navegación, Undo/Redo permanente, Pestañas de Secciones y Controles de Ventana
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Izquierda: Volver, Deshacer (Siempre visible), Rehacer (Siempre visible), Añadir Página '+' (Siempre accesible)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(onClick = onExit, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver a la Biblioteca", modifier = Modifier.size(22.dp))
                    }

                    // Deshacer Permanente
                    IconButton(
                        onClick = { pane.surface?.undo() },
                        enabled = pane.canUndo,
                        modifier = Modifier
                            .size(40.dp)
                            .alpha(if (pane.canUndo) 1.0f else 0.38f)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Deshacer", modifier = Modifier.size(22.dp))
                    }

                    // Rehacer Permanente
                    IconButton(
                        onClick = { pane.surface?.redo() },
                        enabled = pane.canRedo,
                        modifier = Modifier
                            .size(40.dp)
                            .alpha(if (pane.canRedo) 1.0f else 0.38f)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Rehacer", modifier = Modifier.size(22.dp))
                    }

                    // Botón '+' Añadir Página Inmediata
                    IconButton(
                        onClick = onAddPageQuick,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f))
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Añadir Página Inmediatamente", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                    }
                }

                // Centro: Selector de Secciones Categorizadas
                Row(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .padding(horizontal = 8.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TopBarSection.values().forEach { sec ->
                        val isSelected = activeSection == sec
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    activeSection = sec
                                    if (isMinimized) isMinimized = false
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    sec.icon,
                                    contentDescription = sec.label,
                                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    sec.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Derecha: Capas, Fondo, Búsqueda, Minimizar/Expandir y Menú
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    BackgroundPopupButton(pane.backgroundStyle, onBackgroundStyle = { surface?.setPageBackgroundStyle(it) })
                    LayersPopupButton(layerCallbacks)
                    SearchControls(pane)

                    // Toggle Minimizar / Expandir Barra
                    IconButton(
                        onClick = { isMinimized = !isMinimized },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            if (isMinimized) Icons.Filled.KeyboardArrowDown else Icons.Filled.KeyboardArrowUp,
                            contentDescription = if (isMinimized) "Expandir barra de herramientas" else "Minimizar barra para pantalla completa",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    OverflowMenu(
                        onOpen = onOpenDocument,
                        onNewTab = onNewDocument,
                        onSave = onSaveDocument,
                        onSaveAs = onSaveAsDocument,
                        onImportPdf = onImportPdf,
                        onExportPdf = onOpenExportDialog,
                        onSettings = onOpenSettings,
                        splitView = splitView,
                        onToggleSplitView = onToggleSplitView,
                        onOpenStructure = onOpenStructure,
                        onOpenAttachments = onOpenAttachments,
                        onAddPage = onAddPageQuick
                    )
                }
            }

            // Fila 2: Panel Contextual Dinámico según la Sección Activa (Plegable)
            AnimatedVisibility(
                visible = !isMinimized,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .horizontalScroll(rememberScrollState()),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(horizontal = 16.dp)
                                    .wrapContentWidth(Alignment.CenterHorizontally),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)
                            ) {
                                when (activeSection) {
                                    TopBarSection.ESCRITURA -> {
                                        EscrituraSectionContent(
                                            ui = ui,
                                            surface = surface,
                                            styleCallbacks = styleCallbacks,
                                            settings = settings,
                                            onSettingsChange = onSettingsChange
                                        )
                                    }
                                    TopBarSection.DIBUJO -> {
                                        DibujoSectionContent(
                                            ui = ui,
                                            surface = surface,
                                            styleCallbacks = styleCallbacks,
                                            settings = settings,
                                            onSettingsChange = onSettingsChange
                                        )
                                    }
                                    TopBarSection.MATEMATICAS -> {
                                        MatematicasSectionContent(
                                            ui = ui,
                                            pane = pane,
                                            surface = surface,
                                            onOpenTechnicalSymbols = onOpenTechnicalSymbols,
                                            onOpenPeriodicTable = onOpenPeriodicTable,
                                            onOpenScientificCalculator = onOpenScientificCalculator
                                        )
                                    }
                                    TopBarSection.GRAFICAS -> {
                                        GraficasSectionContent(
                                            onOpenPlotter = onOpenFunctionPlotter,
                                            surface = surface,
                                            pane = pane
                                        )
                                    }
                                    TopBarSection.TEXTO -> {
                                        TextoSectionContent(
                                            ui = ui,
                                            surface = surface,
                                            styleCallbacks = styleCallbacks
                                        )
                                    }
                                    TopBarSection.INSERTAR -> {
                                        InsertarSectionContent(
                                            ui = ui,
                                            pane = pane,
                                            surface = surface,
                                            onOpenTableDialog = onOpenTableDialog,
                                            onOpenTechnicalSymbols = onOpenTechnicalSymbols,
                                            onOpenInsertLinkDialog = onOpenInsertLinkDialog,
                                            onPickImage = onPickImage
                                        )
                                    }
                                    TopBarSection.PAGINAS -> {
                                        PaginasSectionContent(
                                            pane = pane,
                                            surface = surface,
                                            onAddPageQuick = onAddPageQuick,
                                            onOpenPageManager = onOpenPageManager,
                                            onOpenStructure = onOpenStructure,
                                            onOpenAttachments = onOpenAttachments
                                        )
                                    }
                                    TopBarSection.AUDIO -> {
                                        AudioSectionContent(
                                            audio = audio,
                                            onOpenSpeechToText = { ui.showSpeechToText = true }
                                        )
                                    }
                                    TopBarSection.EXPORTAR -> {
                                        ExportarSectionContent(
                                            onOpenExportDialog = onOpenExportDialog
                                        )
                                    }
                                    TopBarSection.AJUSTES -> {
                                        AjustesSectionContent(
                                            onOpenSettings = onOpenSettings,
                                            settings = settings,
                                            onSettingsChange = onSettingsChange
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EscrituraSectionContent(
    ui: EditorUiState,
    surface: DrawingSurfaceView?,
    styleCallbacks: ToolbarStyleCallbacks,
    settings: AppSettings,
    onSettingsChange: (AppSettings) -> Unit
) {
    var showPenMenu by remember { mutableStateOf(false) }
    var showHighlighterMenu by remember { mutableStateOf(false) }
    var showEraserMenu by remember { mutableStateOf(false) }

    // 1. Herramientas: Pluma, Subrayador, Borrador, Lazo
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        // Pluma
        Box {
            SectionToolChip(
                label = "Pluma",
                icon = Icons.Filled.Create,
                selected = ui.tool == EditorTool.PEN,
                onClick = {
                    ui.tool = EditorTool.PEN
                    surface?.applyTool(EditorTool.PEN)
                    surface?.colorArgb = ui.penColor
                    surface?.baseWidthPt = ui.penWidth
                },
                onLongClick = {
                    ui.tool = EditorTool.PEN
                    surface?.applyTool(EditorTool.PEN)
                    surface?.colorArgb = ui.penColor
                    surface?.baseWidthPt = ui.penWidth
                    showPenMenu = true
                }
            )
            ToolMiniContextMenu(
                expanded = showPenMenu,
                onDismissRequest = { showPenMenu = false },
                tool = EditorTool.PEN,
                currentColor = ui.penColor,
                currentWidth = ui.penWidth,
                palette = styleCallbacks.palette,
                onColorChange = { col ->
                    ui.penColor = col
                    ui.color = col
                    surface?.colorArgb = col
                    onSettingsChange(settings.withColorUsed(col).copy(lastColor = col))
                },
                onWidthChange = { w ->
                    ui.penWidth = w
                    ui.width = w
                    surface?.baseWidthPt = w
                    onSettingsChange(settings.copy(lastWidth = w))
                }
            )
        }

        // Subrayador
        Box {
            SectionToolChip(
                label = "Subrayador",
                icon = Icons.Filled.Brush,
                selected = ui.tool == EditorTool.HIGHLIGHTER,
                onClick = {
                    ui.tool = EditorTool.HIGHLIGHTER
                    surface?.applyTool(EditorTool.HIGHLIGHTER)
                    surface?.colorArgb = ui.highlighterColor
                    surface?.baseWidthPt = ui.highlighterWidth
                },
                onLongClick = {
                    ui.tool = EditorTool.HIGHLIGHTER
                    surface?.applyTool(EditorTool.HIGHLIGHTER)
                    surface?.colorArgb = ui.highlighterColor
                    surface?.baseWidthPt = ui.highlighterWidth
                    showHighlighterMenu = true
                }
            )
            ToolMiniContextMenu(
                expanded = showHighlighterMenu,
                onDismissRequest = { showHighlighterMenu = false },
                tool = EditorTool.HIGHLIGHTER,
                currentColor = ui.highlighterColor,
                currentWidth = ui.highlighterWidth,
                palette = styleCallbacks.palette,
                onColorChange = { col ->
                    ui.highlighterColor = col
                    ui.color = col
                    surface?.colorArgb = col
                    onSettingsChange(settings.copy(lastHighlighterColor = col))
                },
                onWidthChange = { w ->
                    ui.highlighterWidth = w
                    ui.width = w
                    surface?.baseWidthPt = w
                    onSettingsChange(settings.copy(lastHighlighterWidth = w))
                }
            )
        }

        // Borrador
        Box {
            SectionToolChip(
                label = "Borrador",
                icon = Icons.Filled.Delete,
                selected = ui.tool == EditorTool.ERASER || ui.tool == EditorTool.ERASER_WHOLE,
                onClick = {
                    ui.tool = EditorTool.ERASER
                    surface?.applyTool(EditorTool.ERASER)
                    surface?.baseWidthPt = ui.eraserWidth
                },
                onLongClick = {
                    ui.tool = EditorTool.ERASER
                    surface?.applyTool(EditorTool.ERASER)
                    surface?.baseWidthPt = ui.eraserWidth
                    showEraserMenu = true
                }
            )
            ToolMiniContextMenu(
                expanded = showEraserMenu,
                onDismissRequest = { showEraserMenu = false },
                tool = EditorTool.ERASER,
                currentColor = 0,
                currentWidth = ui.eraserWidth,
                palette = styleCallbacks.palette,
                onColorChange = {},
                onWidthChange = { w ->
                    ui.eraserWidth = w
                    ui.width = w
                    surface?.baseWidthPt = w
                    onSettingsChange(settings.copy(lastEraserWidth = w))
                }
            )
        }

        // Lazo
        SectionToolChip(
            label = "Lazo",
            icon = Icons.Filled.HighlightAlt,
            selected = ui.tool == EditorTool.LASSO_SELECT || ui.tool == EditorTool.SELECT,
            onClick = { ui.tool = EditorTool.LASSO_SELECT; surface?.applyTool(EditorTool.LASSO_SELECT) }
        )
    }

    VerticalDivider(modifier = Modifier.height(28.dp))

    // 2. Colores Rápidos Integrados (Círculos limpios rellenos sin artefactos)
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        FAST_PEN_COLORS.take(6).forEach { colorInt ->
            val isSelected = ui.color == colorInt
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(colorInt))
                    .border(
                        width = if (isSelected) 3.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray.copy(alpha = 0.5f),
                        shape = CircleShape
                    )
                    .clickable { styleCallbacks.onColor(colorInt) }
            )
        }
    }

    // 3. Selector de Grosor y Selector de Color Completo
    ToolbarColorPopup(styleCallbacks = styleCallbacks)

    VerticalDivider(modifier = Modifier.height(28.dp))

    // 4. Reconocer Formas Switch (Espacioso y sin solapamientos)
    CompactSwitchRow(
        label = "Formas auto",
        checked = settings.recognizeShapes,
        onCheckedChange = {
            surface?.recognizeShapes = it
            onSettingsChange(settings.copy(recognizeShapes = it))
        }
    )
}

@Composable
private fun DibujoSectionContent(
    ui: EditorUiState,
    surface: DrawingSurfaceView?,
    styleCallbacks: ToolbarStyleCallbacks,
    settings: AppSettings,
    onSettingsChange: (AppSettings) -> Unit
) {
    val drawingTools = listOf(
        EditorTool.LINE to "Línea",
        EditorTool.ARROW to "Flecha",
        EditorTool.DOUBLE_ARROW to "Doble flecha",
        EditorTool.RECTANGLE to "Rectángulo",
        EditorTool.ELLIPSE to "Elipse",
        EditorTool.COORDINATE_AXIS to "Ejes",
        EditorTool.SPLINE to "Spline"
    )

    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        drawingTools.forEach { (tool, label) ->
            SectionToolChip(
                label = label,
                icon = tool.icon,
                selected = ui.tool == tool,
                onClick = { ui.tool = tool; surface?.applyTool(tool) }
            )
        }
    }

    VerticalDivider(modifier = Modifier.height(28.dp))

    // Guías y Reglas
    var guideExpanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(
            onClick = { guideExpanded = true },
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            modifier = Modifier.height(38.dp)
        ) {
            Icon(Icons.Filled.SquareFoot, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Guía: ${settings.guideKind.name.lowercase().capitalize()}", fontSize = 13.sp)
        }
        DropdownMenu(expanded = guideExpanded, onDismissRequest = { guideExpanded = false }) {
            GuideKind.values().forEach { g ->
                DropdownMenuItem(
                    text = { Text(g.name.lowercase().capitalize()) },
                    onClick = {
                        surface?.placeGuide(g)
                        onSettingsChange(settings.copy(guideKind = g))
                        guideExpanded = false
                    }
                )
            }
        }
    }

    ToolbarColorPopup(styleCallbacks = styleCallbacks)
}

@Composable
private fun MatematicasSectionContent(
    ui: EditorUiState,
    pane: PaneState,
    surface: DrawingSurfaceView?,
    onOpenTechnicalSymbols: () -> Unit,
    onOpenPeriodicTable: () -> Unit,
    onOpenScientificCalculator: () -> Unit
) {
    // LaTeX Tradicional
    OutlinedButton(
        onClick = {
            ui.texPlacement = com.nexopp.render.Placement(pane.currentPage, 100.0, 100.0)
        },
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        modifier = Modifier.height(38.dp)
    ) {
        Icon(Icons.Filled.Functions, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text("LaTeX Tradicional", fontSize = 13.sp)
    }

    OutlinedButton(
        onClick = onOpenTechnicalSymbols,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        modifier = Modifier.height(38.dp)
    ) {
        Text("∫ Símbolos Técnicos", fontSize = 13.sp)
    }

    OutlinedButton(
        onClick = onOpenPeriodicTable,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        modifier = Modifier.height(38.dp)
    ) {
        Text("⚗ Tabla Periódica", fontSize = 13.sp)
    }
}

@Composable
private fun GraficasSectionContent(
    onOpenPlotter: () -> Unit,
    surface: DrawingSurfaceView?,
    pane: PaneState
) {
    Button(
        onClick = onOpenPlotter,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
        modifier = Modifier.height(38.dp)
    ) {
        Icon(Icons.AutoMirrored.Filled.ShowChart, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text("Trazador de Funciones Vectorial", fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }

    VerticalDivider(modifier = Modifier.height(28.dp))

    // Preajustes rápidos de funciones STEM
    val presets = listOf(
        "sin(x)" to "sen(x)",
        "cos(x)" to "cos(x)",
        "x^2" to "x²",
        "exp(-x^2)" to "Gauss"
    )

    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        presets.forEach { (formula, name) ->
            OutlinedButton(
                onClick = {
                    val elements = com.nexopp.stem.FunctionPlotter.generatePlotElements(
                        formula = formula,
                        originX = 250.0,
                        originY = 250.0
                    )
                    surface?.insertElements(elements)
                },
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                modifier = Modifier.height(38.dp)
            ) {
                Text(name, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun TextoSectionContent(
    ui: EditorUiState,
    surface: DrawingSurfaceView?,
    styleCallbacks: ToolbarStyleCallbacks
) {
    SectionToolChip(
        label = "Texto",
        icon = Icons.Filled.TextFields,
        selected = ui.tool == EditorTool.TEXT,
        onClick = { ui.tool = EditorTool.TEXT; surface?.applyTool(EditorTool.TEXT) }
    )

    VerticalDivider(modifier = Modifier.height(28.dp))

    // Rich Text Format: Negrita, Cursiva, Subrayado (conectado al TextElement seleccionado y a textDefaults)
    val selectedText = surface?.selectedTextElement()
    val selectedFd = selectedText?.let { FontDescription.parse(it.font) }

    val isBold = selectedFd?.bold ?: ui.textDefaults.bold
    val isItalic = selectedFd?.italic ?: ui.textDefaults.italic
    val isUnderline = selectedText?.let { it.extraAttrs["underline"] == "true" } ?: ui.textDefaults.underline
    val selectedFontSize = selectedText?.let { it.size.roundToInt() } ?: ui.textDefaults.size.roundToInt()
    var fontSizeMenuExpanded by remember { mutableStateOf(false) }

    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(
            onClick = {
                if (surface?.selectedTextElement() != null) {
                    surface.toggleTextBold()
                }
                ui.textDefaults.bold = !isBold
            },
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(if (isBold) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
        ) {
            Icon(Icons.Filled.FormatBold, contentDescription = "Negrita", modifier = Modifier.size(20.dp))
        }

        IconButton(
            onClick = {
                if (surface?.selectedTextElement() != null) {
                    surface.toggleTextItalic()
                }
                ui.textDefaults.italic = !isItalic
            },
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(if (isItalic) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
        ) {
            Icon(Icons.Filled.FormatItalic, contentDescription = "Cursiva", modifier = Modifier.size(20.dp))
        }

        IconButton(
            onClick = {
                if (surface?.selectedTextElement() != null) {
                    surface.toggleTextUnderline()
                }
                ui.textDefaults.underline = !isUnderline
            },
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(if (isUnderline) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
        ) {
            Icon(Icons.Filled.FormatUnderlined, contentDescription = "Subrayado", modifier = Modifier.size(20.dp))
        }
    }

    VerticalDivider(modifier = Modifier.height(28.dp))

    // Selector Numérico de Tamaño de Fuente
    Box {
        OutlinedButton(
            onClick = { fontSizeMenuExpanded = true },
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            modifier = Modifier.height(38.dp)
        ) {
            Text("$selectedFontSize pt", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(4.dp))
            Icon(Icons.Filled.ArrowDropDown, contentDescription = null, modifier = Modifier.size(18.dp))
        }
        DropdownMenu(expanded = fontSizeMenuExpanded, onDismissRequest = { fontSizeMenuExpanded = false }) {
            TEXT_FONT_SIZES.forEach { size ->
                DropdownMenuItem(
                    text = { Text("$size pt", fontWeight = if (size == selectedFontSize) FontWeight.Bold else FontWeight.Normal) },
                    trailingIcon = {
                        if (size == selectedFontSize) Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    },
                    onClick = {
                        if (surface?.selectedTextElement() != null) {
                            surface.setTextFontSize(size.toDouble())
                        }
                        ui.textDefaults.size = size.toDouble()
                        fontSizeMenuExpanded = false
                    }
                )
            }
        }
    }

    VerticalDivider(modifier = Modifier.height(28.dp))

    OutlinedButton(
        onClick = { ui.showSpeechToText = true },
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
        modifier = Modifier.height(38.dp)
    ) {
        Icon(Icons.Filled.Mic, contentDescription = "Dictar por voz", modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text("Dictar", fontSize = 13.sp)
    }

    ToolbarColorPopup(styleCallbacks = styleCallbacks)
}

@Composable
private fun InsertarSectionContent(
    ui: EditorUiState,
    pane: PaneState,
    surface: DrawingSurfaceView?,
    onOpenTableDialog: () -> Unit,
    onOpenTechnicalSymbols: () -> Unit,
    onOpenInsertLinkDialog: () -> Unit,
    onPickImage: () -> Unit
) {
    Button(
        onClick = onPickImage,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        modifier = Modifier.height(38.dp)
    ) {
        Icon(Icons.Filled.Image, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text("Imagen", fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }

    OutlinedButton(
        onClick = { ui.showBrowserDialog = true },
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        modifier = Modifier.height(38.dp)
    ) {
        Icon(Icons.Filled.Public, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text("Navegador Web", fontSize = 13.sp)
    }

    OutlinedButton(
        onClick = onOpenTableDialog,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        modifier = Modifier.height(38.dp)
    ) {
        Icon(Icons.Filled.TableChart, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text("Tabla Interactiva", fontSize = 13.sp)
    }

    OutlinedButton(
        onClick = { ui.texPlacement = com.nexopp.render.Placement(pane.currentPage, 100.0, 100.0) },
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        modifier = Modifier.height(38.dp)
    ) {
        Icon(Icons.Filled.Functions, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text("Fórmula LaTeX", fontSize = 13.sp)
    }

    OutlinedButton(
        onClick = onOpenTechnicalSymbols,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        modifier = Modifier.height(38.dp)
    ) {
        Icon(Icons.Filled.Category, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text("Símbolo Técnico", fontSize = 13.sp)
    }

    OutlinedButton(
        onClick = onOpenInsertLinkDialog,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        modifier = Modifier.height(38.dp)
    ) {
        Icon(Icons.Filled.Link, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text("Enlace Web", fontSize = 13.sp)
    }
}

@Composable
private fun PaginasSectionContent(
    pane: PaneState,
    surface: DrawingSurfaceView?,
    onAddPageQuick: () -> Unit,
    onOpenPageManager: () -> Unit,
    onOpenStructure: () -> Unit,
    onOpenAttachments: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.clickable { onOpenPageManager() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Icon(Icons.Filled.AutoStories, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
            Spacer(Modifier.width(6.dp))
            Text(
                "Página ${pane.currentPage + 1} de ${pane.pageCount.coerceAtLeast(1)}",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }

    Button(
        onClick = onAddPageQuick,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        modifier = Modifier.height(38.dp)
    ) {
        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text("Añadir Página", fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }

    OutlinedButton(
        onClick = { surface?.removePage() },
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
        modifier = Modifier.height(38.dp)
    ) {
        Icon(Icons.Filled.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text("Eliminar Página", fontSize = 13.sp)
    }

    OutlinedButton(
        onClick = onOpenStructure,
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
        modifier = Modifier.height(38.dp)
    ) {
        Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text("Índice / TOC", fontSize = 13.sp)
    }

    OutlinedButton(
        onClick = onOpenAttachments,
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
        modifier = Modifier.height(38.dp)
    ) {
        Icon(Icons.Filled.AttachFile, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text("Adjuntos", fontSize = 13.sp)
    }
}

@Composable
private fun AudioSectionContent(
    audio: AudioUiState,
    onOpenSpeechToText: () -> Unit
) {
    Button(
        onClick = { audio.onToggleRecord() },
        colors = ButtonDefaults.buttonColors(
            containerColor = if (audio.recording) Color(0xFFD32F2F) else MaterialTheme.colorScheme.primaryContainer,
            contentColor = if (audio.recording) Color.White else MaterialTheme.colorScheme.onPrimaryContainer
        ),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
        modifier = Modifier.height(38.dp)
    ) {
        Icon(
            if (audio.recording) Icons.Filled.Stop else Icons.Filled.Mic,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            if (audio.recording) "Detener Grabación" else "Grabar Audio (Offline)",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }

    if (audio.playing) {
        OutlinedButton(
            onClick = { audio.onStopPlayback() },
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
            modifier = Modifier.height(38.dp)
        ) {
            Icon(Icons.Filled.StopCircle, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Detener Reproducción", fontSize = 13.sp)
        }
    }

    OutlinedButton(
        onClick = onOpenSpeechToText,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        modifier = Modifier.height(38.dp)
    ) {
        Icon(Icons.Filled.RecordVoiceOver, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text("Dictado (Texto)", fontSize = 13.sp)
    }
}

@Composable
private fun ExportarSectionContent(
    onOpenExportDialog: () -> Unit
) {
    Button(
        onClick = onOpenExportDialog,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
        modifier = Modifier.height(38.dp)
    ) {
        Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text("Exportar y Compartir...", fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }

    Text(
        "PDF Vectorial, Imágenes PNG/JPEG, XOPP, Markdown y Backups.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun AjustesSectionContent(
    onOpenSettings: () -> Unit,
    settings: AppSettings,
    onSettingsChange: (AppSettings) -> Unit
) {
    Button(
        onClick = onOpenSettings,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
        modifier = Modifier.height(38.dp)
    ) {
        Icon(Icons.Filled.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text("Abrir Ajustes Completos", fontSize = 13.sp)
    }

    VerticalDivider(modifier = Modifier.height(28.dp))

    CompactSwitchRow(
        label = "Rechazo de palma",
        checked = !settings.fingerDraws,
        onCheckedChange = { onSettingsChange(settings.copy(fingerDraws = !it)) }
    )
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun SectionToolChip(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
