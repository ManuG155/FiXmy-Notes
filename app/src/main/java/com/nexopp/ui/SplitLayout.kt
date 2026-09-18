// Ruta: app/src/main/java/com/nexopp/ui/SplitLayout.kt
package com.nexopp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

private const val MIN_FRACTION = 0.15f
private val HANDLE_WIDTH = 28.dp
private val HANDLE_LINE = 2.dp

@Composable
fun SplitLayout(
    fraction: Float,
    onFraction: (Float) -> Unit,
    modifier: Modifier = Modifier,
    onClose: (() -> Unit)? = null,
    first: @Composable (Modifier) -> Unit,
    second: @Composable (Modifier) -> Unit,
) {
    BoxWithConstraints(modifier = modifier) {
        val totalPx = with(LocalDensity.current) { maxWidth.toPx() }.coerceAtLeast(1f)
        Row(modifier = Modifier.fillMaxSize()) {
            first(Modifier.fillMaxHeight().weight(fraction.coerceIn(MIN_FRACTION, 1f - MIN_FRACTION)))
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(HANDLE_WIDTH)
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .semantics { contentDescription = "Redimensionar vista dividida" }
                    .draggable(
                        orientation = Orientation.Horizontal,
                        state = rememberDraggableState { delta ->
                            onFraction((fraction + delta / totalPx).coerceIn(MIN_FRACTION, 1f - MIN_FRACTION))
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(HANDLE_LINE)
                        .background(MaterialTheme.colorScheme.outlineVariant),
                )
                if (onClose != null) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .size(28.dp)
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest, CircleShape)
                            .semantics { contentDescription = "Cerrar vista dividida" }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar división",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            second(Modifier.fillMaxHeight().weight((1f - fraction).coerceIn(MIN_FRACTION, 1f - MIN_FRACTION)))
        }
    }
}