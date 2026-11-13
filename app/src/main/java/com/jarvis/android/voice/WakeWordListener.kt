package com.jarvis.android.voice

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sqrt

class WakeWordListener(
    private val context: Context,
    private val onWake: () -> Unit,
    private val onError: (String) -> Unit
) {
    private var recorder: AudioRecord? = null
    private var running = false
    private val handler = Handler(Looper.getMainLooper())
    private var lastTriggerAt = 0L
    private val minRmsDb = 20.0
    private val cooldownMs = 3000L

    fun start() {
        if (running) return

        try {
            val sampleRate = 16000
            val channelConfig = AudioFormat.CHANNEL_IN_MONO
            val audioFormat = AudioFormat.ENCODING_PCM_16BIT
            val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
                .coerceAtLeast(2048)

            recorder = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )

            if (recorder?.state != AudioRecord.STATE_INITIALIZED) {
                onError("Wake-word microphone could not initialize.")
                return
            }

            running = true
            recorder?.startRecording()
            readLoop()
        } catch (e: SecurityException) {
            onError("Microphone permission is required for wake-word detection.")
        } catch (e: IllegalStateException) {
            onError("Wake-word listener failed to start on this device.")
        }
    }

    fun stop() {
        running = false
        recorder?.let {
            try { it.stop() } catch (_: IllegalStateException) {}
            it.release()
        }
        recorder = null
    }

    private fun readLoop() {
        if (!running) return

        val bufferSize = recorder?.bufferSizeInFrames ?: 0
        if (bufferSize <= 0) {
            running = false
            onError("Wake-word listener has no usable buffer.")
            return
        }

        val buffer = ShortArray((bufferSize / 2).coerceAtLeast(1024))
        val read = recorder?.read(buffer, 0, buffer.size) ?: 0

        if (read > 0) {
            val rms = buffer.copyOf(read).fold(0.0) { acc, sample ->
                acc + sample.toDouble().pow(2.0)
            } / read
            val db = if (rms > 0.0) 20.0 * log10(sqrt(rms)) else 0.0

            val now = System.currentTimeMillis()
            if (db > minRmsDb && now - lastTriggerAt > cooldownMs) {
                lastTriggerAt = now
                handler.post(onWake)
            }
        }

        handler.postDelayed({ readLoop() }, 50L)
    }
}
