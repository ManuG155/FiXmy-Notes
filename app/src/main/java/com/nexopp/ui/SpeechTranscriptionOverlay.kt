package com.nexopp.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.nexopp.audio.SpeechState
import com.nexopp.audio.SpeechToTextManager

@Composable
fun SpeechTranscriptionDialog(
    speechManager: SpeechToTextManager,
    onDismiss: () -> Unit,
) {
    LaunchedEffect(Unit) {
        speechManager.startListening()
    }

    DisposableEffect(Unit) {
        onDispose {
            speechManager.destroy()
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Dialog(onDismissRequest = {
        speechManager.cancel()
        onDismiss()
    }) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            tonalElevation = 6.dp,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Cabecera e icono animado
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .scale(if (speechManager.state == SpeechState.LISTENING) pulseScale else 1f)
                        .background(
                            when (speechManager.state) {
                                SpeechState.LISTENING -> MaterialTheme.colorScheme.primaryContainer
                                SpeechState.PROCESSING -> MaterialTheme.colorScheme.tertiaryContainer
                                SpeechState.ERROR -> MaterialTheme.colorScheme.errorContainer
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            },
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (speechManager.state == SpeechState.PROCESSING) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 3.dp
                        )
                    } else {
                        Icon(
                            imageVector = if (speechManager.state == SpeechState.ERROR) Icons.Filled.Close else Icons.Filled.Mic,
                            contentDescription = null,
                            tint = if (speechManager.state == SpeechState.ERROR) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Título de estado
                Text(
                    text = when (speechManager.state) {
                        SpeechState.LISTENING -> "Escuchando voz…"
                        SpeechState.PROCESSING -> "Procesando dictado…"
                        SpeechState.COMPLETED -> "¡Completado!"
                        SpeechState.ERROR -> "Error de dictado"
                        SpeechState.CANCELLED -> "Cancelado"
                        SpeechState.IDLE -> "Preparado"
                    },
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(Modifier.height(8.dp))

                // Mensaje de ayuda o error
                if (speechManager.errorMessage != null) {
                    Text(
                        text = speechManager.errorMessage.orEmpty(),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                } else {
                    Text(
                        text = "Habla con naturalidad. El texto se insertará automáticamente al terminar.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }

                Spacer(Modifier.height(16.dp))

                // Área de texto parcial
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 90.dp, max = 180.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(12.dp)
                        .verticalScroll(rememberScrollState()),
                    contentAlignment = if (speechManager.partialText.isEmpty()) Alignment.Center else Alignment.TopStart
                ) {
                    if (speechManager.partialText.isEmpty()) {
                        Text(
                            text = "Esperando palabras…",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            fontSize = 14.sp
                        )
                    } else {
                        Text(
                            text = speechManager.partialText,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Botones de acción
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            speechManager.cancel()
                            onDismiss()
                        },
                        modifier = Modifier.height(40.dp)
                    ) {
                        Text("Cancelar")
                    }

                    Spacer(Modifier.width(12.dp))

                    if (speechManager.state == SpeechState.ERROR) {
                        Button(
                            onClick = { speechManager.startListening() },
                            modifier = Modifier.height(40.dp)
                        ) {
                            Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Reintentar")
                        }
                    } else {
                        Button(
                            onClick = {
                                speechManager.commitManual()
                                onDismiss()
                            },
                            enabled = speechManager.partialText.isNotBlank(),
                            modifier = Modifier.height(40.dp)
                        ) {
                            Icon(Icons.Filled.Done, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Insertar")
                        }
                    }
                }
            }
        }
    }
}
