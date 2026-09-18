// --- EditorDialogs.kt ---
package com.nexopp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexopp.format.FontDescription
import com.nexopp.format.SaveFormat
import com.nexopp.render.ImportPdfMode
import kotlin.math.roundToInt

private val TEXT_FAMILIES = listOf("Sans", "Serif", "Monospace")

@Composable
fun SaveAsDialog(
    initialFormat: SaveFormat,
    onConfirm: (filename: String, format: SaveFormat) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf("document.xopp") }
    var format by remember { mutableStateOf(initialFormat) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Guardar como") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre del archivo") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Formato", style = MaterialTheme.typography.labelMedium)
                FormatOption(
                    selected = format == SaveFormat.ORIGINAL,
                    title = "Original (gzip)",
                    subtitle = "Archivo estándar de Xournal++; cualquier PDF de fondo se mantiene enlazado por su ubicación.",
                    onClick = { format = SaveFormat.ORIGINAL },
                )
                FormatOption(
                    selected = format == SaveFormat.ZIPPED,
                    title = "Comprimido (un solo archivo)",
                    subtitle = "Un archivo portable con el PDF integrado.",
                    onClick = { format = SaveFormat.ZIPPED },
                )
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(name, format) }) { Text("Guardar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@Composable
fun ImportPdfDialog(
    merging: Boolean,
    onConfirm: (ImportPdfMode) -> Unit,
    onDismiss: () -> Unit,
) {
    var mode by remember { mutableStateOf(ImportPdfMode.APPEND) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Importar PDF") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FormatOption(
                    selected = mode == ImportPdfMode.REPLACE,
                    title = "Reemplazar",
                    subtitle = "Las páginas del PDF se convertirán en este documento, reemplazando las actuales.",
                    onClick = { mode = ImportPdfMode.REPLACE },
                )
                FormatOption(
                    selected = mode == ImportPdfMode.APPEND,
                    title = "Añadir al final",
                    subtitle = if (merging)
                        "Añade las páginas del PDF después de las abiertas, fusionándolas con el PDF de fondo actual."
                    else "Añade las páginas del PDF después de las que ya están abiertas.",
                    onClick = { mode = ImportPdfMode.APPEND },
                )
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(mode) }) { Text("Elegir PDF…") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@Composable
fun FormatOption(
    selected: Boolean,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        RadioButton(selected = selected, onClick = onClick, enabled = enabled)
        val alpha = if (enabled) 1f else 0.38f
        Column(modifier = Modifier.alpha(alpha)) {
            Text(title)
            Text(subtitle, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun TextBoxDialog(
    title: String,
    initialContent: String,
    initialFamily: String,
    initialBold: Boolean,
    initialItalic: Boolean,
    initialUnderline: Boolean = false,
    initialSize: Double,
    initialColor: Int,
    palette: ColorPaletteState,
    onConfirm: (content: String, family: String, bold: Boolean, italic: Boolean, underline: Boolean, sizePt: Double, colorArgb: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var content by remember { mutableStateOf(initialContent) }
    var family by remember { mutableStateOf(initialFamily) }
    var bold by remember { mutableStateOf(initialBold) }
    var italic by remember { mutableStateOf(initialItalic) }
    var underline by remember { mutableStateOf(initialUnderline) }
    var size by remember { mutableStateOf(initialSize.toFloat().coerceIn(TEXT_SIZE_MIN, TEXT_SIZE_MAX)) }
    var colorArgb by remember { mutableStateOf(initialColor) }
    var editingColor by remember { mutableStateOf(false) }
    val focusRequester = remember { androidx.compose.ui.focus.FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    androidx.compose.runtime.LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(150)
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    CustomColorEditor(visible = editingColor, palette = palette, onDismiss = { editingColor = false })
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    singleLine = false,
                    minLines = 3,
                    label = { Text("Texto") },
                    textStyle = TextStyle(
                        fontSize = 16.sp,
                        color = androidx.compose.ui.graphics.Color(colorArgb)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    FontFamilyPicker(family = family, onFamily = { family = it })
                    FilterChip(selected = bold, onClick = { bold = !bold }, label = { Text("Negrita") })
                    FilterChip(selected = italic, onClick = { italic = !italic }, label = { Text("Cursiva") })
                    FilterChip(selected = underline, onClick = { underline = !underline }, label = { Text("Subrayado") })
                }
                Text("Tamaño: ${size.roundToInt()} pt", style = MaterialTheme.typography.labelMedium)
                Slider(
                    value = size,
                    onValueChange = { size = it },
                    valueRange = TEXT_SIZE_MIN..TEXT_SIZE_MAX,
                )
                ColorPaletteRows(
                    selected = colorArgb,
                    palette = palette,
                    onPick = { c -> colorArgb = c; palette.note(c) },
                    onEditCustom = { editingColor = true },
                )
            }
        },
        confirmButton = {
            androidx.compose.material3.Button(onClick = { onConfirm(content, family, bold, italic, underline, size.toDouble(), colorArgb) }) {
                Text("Insertar Texto")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@Composable
fun FontFamilyPicker(family: String, onFamily: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { open = true }) { Text(family) }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            for (f in TEXT_FAMILIES) {
                DropdownMenuItem(text = { Text(f) }, onClick = { onFamily(f); open = false })
            }
        }
    }
}