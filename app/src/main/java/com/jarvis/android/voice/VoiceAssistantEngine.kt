package com.jarvis.android.voice

import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.os.Bundle
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager
import com.jarvis.android.overlay.OrbVisualState

class VoiceAssistantEngine(
    private val context: Context,
    private val onResult: (String) -> Unit,
    private val onStateChanged: (OrbVisualState) -> Unit,
    private val onError: (String) -> Unit
) : TextToSpeech.OnInitListener {

    private val recognizer: SpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
    private val tts: TextToSpeech = TextToSpeech(context, this)
    private var isListening = false
    private var isReady = false

    init {
        recognizer.setRecognitionListener(object : android.speech.RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                isListening = true
                onStateChanged(OrbVisualState.LISTENING)
            }

            override fun onBeginningOfSpeech() = Unit
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() {
                isListening = false
                onStateChanged(OrbVisualState.THINKING)
            }

            override fun onError(error: Int) {
                // Timeouts and no-match errors can arrive without onEndOfSpeech, so always reset here.
                isListening = false
                val message = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                    SpeechRecognizer.ERROR_CLIENT -> "Speech recognizer unavailable"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is required"
                    SpeechRecognizer.ERROR_NETWORK -> "Network error while listening"
                    SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recognizer is busy"
                    SpeechRecognizer.ERROR_SERVER -> "Speech service error"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected"
                    else -> "Speech recognition error"
                }
                // Qualified so this calls the constructor callback (String), not this listener's onError(Int).
                this@VoiceAssistantEngine.onError(message)
                onStateChanged(OrbVisualState.ERROR)
            }

            override fun onResults(results: Bundle?) {
                isListening = false
                val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    ?.trim()
                    .orEmpty()
                if (text.isNotBlank()) {
                    onResult(text)
                    onStateChanged(OrbVisualState.SUCCESS)
                } else {
                    onStateChanged(OrbVisualState.IDLE)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) = Unit
            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        })
    }

    fun hasPermission(): Boolean = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.RECORD_AUDIO
    ) == PackageManager.PERMISSION_GRANTED

    fun startListening() {
        if (!hasPermission()) {
            onError("Microphone permission is required to listen.")
            return
        }
        if (isListening) return
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onError("Speech recognition is not available on this device.")
            return
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1800L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1800L)
        }
        recognizer.startListening(intent)
    }

    fun stopListening() {
        if (isListening) {
            recognizer.stopListening()
            isListening = false
        }
    }

    fun speak(text: String) {
        if (text.isBlank()) return
        if (isReady) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "jarvis_response")
            onStateChanged(OrbVisualState.SPEAKING)
        }
    }

    override fun onInit(status: Int) {
        isReady = status == TextToSpeech.SUCCESS
        if (isReady) {
            onStateChanged(OrbVisualState.IDLE)
        } else {
            onError("Text-to-speech is unavailable on this device.")
        }
    }

    fun release() {
        stopListening()
        recognizer.destroy()
        tts.stop()
        tts.shutdown()
    }
}
