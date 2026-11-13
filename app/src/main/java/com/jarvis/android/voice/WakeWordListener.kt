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
    private var sampleRate = 16000
    private var channelConfig = AudioFormat.CHANNEL_IN_MONO
    private var audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

    fun start() {
        if (running) return
        val minBuffer = bufferSize.coerceAtLeast(2 * 16000)
        try {
            recorder = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                minBuffer
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
        recorder?.stop()
        recorder?.release()
        recorder = null
    }

    private fun readLoop() {
        if (!running) return
        val buffer = ShortArray(bufferSize.coerceAtLeast(2048) / 2)
        val read = recorder?.read(buffer, 0, buffer.size) ?: 0
        if (read > 0) {
            val rms = buffer.fold(0.0) { acc, sample -> acc + sample.toDouble().pow(2.0) } / read
            val level = if (rms > 0.0) 20.0 * log10(sqrt(rms)) else 0.0
            if (level > 20.0) {
                handler.post { onWake() }
            }
        }
        if (running) handler.post { readLoop() }
    }
}
