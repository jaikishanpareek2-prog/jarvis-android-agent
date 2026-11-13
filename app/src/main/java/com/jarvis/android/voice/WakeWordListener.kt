package com.jarvis.android.voice

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import kotlin.math.log10
import kotlin.math.sqrt

/**
 * Simple loudness based wake trigger. Microphone reads block, so they run on a worker thread;
 * callbacks are always delivered on the main thread.
 */
class WakeWordListener(
    private val context: Context,
    private val onWake: () -> Unit,
    private val onError: (String) -> Unit
) {
    @Volatile private var running = false
    private var recorder: AudioRecord? = null
    private var worker: Thread? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    // Level is 20 * log10(RMS of 16-bit samples). Quiet rooms sit around 35 to 45, so 20 fired constantly.
    private val minRmsDb = 60.0
    private val cooldownMs = 3000L

    fun start() {
        if (running) return

        try {
            val sampleRate = 16000
            val channelConfig = AudioFormat.CHANNEL_IN_MONO
            val audioFormat = AudioFormat.ENCODING_PCM_16BIT
            val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
                .coerceAtLeast(2048)

            val audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )

            if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
                audioRecord.release()
                onError("Wake-word microphone could not initialize.")
                return
            }

            recorder = audioRecord
            running = true
            audioRecord.startRecording()
            worker = Thread({ readLoop(audioRecord, bufferSize) }, "wake-word-listener").also { it.start() }
        } catch (e: SecurityException) {
            running = false
            releaseRecorder()
            onError("Microphone permission is required for wake-word detection.")
        } catch (e: IllegalStateException) {
            running = false
            releaseRecorder()
            onError("Wake-word listener failed to start on this device.")
        }
    }

    fun stop() {
        running = false
        // Stopping the recorder unblocks the pending read() so the worker can exit and release it.
        recorder?.let { try { it.stop() } catch (_: IllegalStateException) {} }
        worker = null
    }

    private fun readLoop(audioRecord: AudioRecord, bufferSizeBytes: Int) {
        val buffer = ShortArray((bufferSizeBytes / 2).coerceAtLeast(1024))
        var lastTriggerAt = 0L

        try {
            while (running) {
                val read = audioRecord.read(buffer, 0, buffer.size)
                if (read < 0) {
                    if (running) postError("Wake-word listener lost the microphone.")
                    break
                }
                if (read == 0) continue

                var sumSquares = 0.0
                for (i in 0 until read) {
                    val sample = buffer[i].toDouble()
                    sumSquares += sample * sample
                }
                val rms = sqrt(sumSquares / read)
                val db = if (rms > 0.0) 20.0 * log10(rms) else 0.0

                val now = System.currentTimeMillis()
                if (db > minRmsDb && now - lastTriggerAt > cooldownMs) {
                    lastTriggerAt = now
                    mainHandler.post { onWake() }
                }
            }
        } catch (_: IllegalStateException) {
            // Recorder was stopped while reading; nothing to report.
        } finally {
            running = false
            if (recorder === audioRecord) releaseRecorder()
        }
    }

    private fun postError(message: String) {
        mainHandler.post { onError(message) }
    }

    @Synchronized
    private fun releaseRecorder() {
        recorder?.let {
            try { it.stop() } catch (_: IllegalStateException) {}
            it.release()
        }
        recorder = null
    }
}
