package com.nexopp.audio

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import java.util.Locale

enum class SpeechState {
    IDLE,
    LISTENING,
    PROCESSING,
    COMPLETED,
    ERROR,
    CANCELLED
}

class SpeechToTextManager(
    private val context: Context,
    private val onCommitText: (String) -> Unit
) {
    var state by mutableStateOf(SpeechState.IDLE)
        private set

    var partialText by mutableStateOf("")
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    var soundLevel by mutableFloatStateOf(0f)
        private set

    private var speechRecognizer: SpeechRecognizer? = null
    private var currentSessionId: Long = 0L
    private var committed = false
    private val mainHandler = Handler(Looper.getMainLooper())

    fun isAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    fun hasPermission(): Boolean = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.RECORD_AUDIO
    ) == PackageManager.PERMISSION_GRANTED

    fun startListening() {
        if (!isAvailable()) {
            errorMessage = "El reconocimiento de voz no está disponible en este dispositivo."
            state = SpeechState.ERROR
            return
        }

        if (!hasPermission()) {
            errorMessage = "Se requiere el permiso de grabación de audio (RECORD_AUDIO)."
            state = SpeechState.ERROR
            return
        }

        // Increment session ID to invalidate any previous callbacks
        currentSessionId++
        val sessionId = currentSessionId
        committed = false
        partialText = ""
        errorMessage = null
        soundLevel = 0f

        destroyRecognizer()

        mainHandler.post {
            if (sessionId != currentSessionId) return@post
            try {
                val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
                speechRecognizer = recognizer

                recognizer.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        if (sessionId != currentSessionId) return
                        state = SpeechState.LISTENING
                    }

                    override fun onBeginningOfSpeech() {
                        if (sessionId != currentSessionId) return
                        state = SpeechState.LISTENING
                    }

                    override fun onRmsChanged(rmsdB: Float) {
                        if (sessionId != currentSessionId) return
                        soundLevel = rmsdB
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        if (sessionId != currentSessionId) return
                        state = SpeechState.PROCESSING
                    }

                    override fun onError(error: Int) {
                        if (sessionId != currentSessionId || committed) return
                        val msg = when (error) {
                            SpeechRecognizer.ERROR_AUDIO -> "Error de audio al grabar"
                            SpeechRecognizer.ERROR_CLIENT -> "Error del cliente de reconocimiento"
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permiso de micrófono denegado"
                            SpeechRecognizer.ERROR_NETWORK -> "Error de conexión de red"
                            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Tiempo de espera de red agotado"
                            SpeechRecognizer.ERROR_NO_MATCH -> "No se detectó ninguna palabra"
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "El servicio de reconocimiento está ocupado"
                            SpeechRecognizer.ERROR_SERVER -> "Error en el servidor de reconocimiento"
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No se detectó voz"
                            else -> "Error en el reconocimiento de voz ($error)"
                        }
                        errorMessage = msg
                        state = SpeechState.ERROR
                        destroyRecognizer()
                    }

                    override fun onResults(results: Bundle?) {
                        if (sessionId != currentSessionId || committed) return
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull()?.trim() ?: partialText.trim()
                        if (text.isNotBlank()) {
                            state = SpeechState.COMPLETED
                            commit(text, sessionId)
                        } else {
                            state = SpeechState.IDLE
                            destroyRecognizer()
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        if (sessionId != currentSessionId || committed) return
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull()?.trim()
                        if (!text.isNullOrBlank()) {
                            partialText = text
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                }

                state = SpeechState.LISTENING
                recognizer.startListening(intent)
            } catch (e: Exception) {
                if (sessionId != currentSessionId) return@post
                errorMessage = "Error al iniciar reconocimiento: ${e.message}"
                state = SpeechState.ERROR
                destroyRecognizer()
            }
        }
    }

    /**
     * Early commit triggered manually by user clicking "Insertar".
     */
    fun commitManual() {
        val text = partialText.trim()
        if (text.isNotBlank() && !committed) {
            commit(text, currentSessionId)
        }
        cancel()
    }

    private fun commit(text: String, sessionId: Long) {
        if (sessionId != currentSessionId || committed) return
        committed = true
        onCommitText(text)
        state = SpeechState.IDLE
        destroyRecognizer()
    }

    fun cancel() {
        currentSessionId++
        committed = true
        state = SpeechState.CANCELLED
        try {
            speechRecognizer?.cancel()
        } catch (_: Exception) {}
        destroyRecognizer()
        state = SpeechState.IDLE
    }

    fun destroy() {
        currentSessionId++
        committed = true
        destroyRecognizer()
        state = SpeechState.IDLE
    }

    private fun destroyRecognizer() {
        try {
            speechRecognizer?.destroy()
        } catch (_: Exception) {}
        speechRecognizer = null
    }
}
