package com.example.blackbox.sensor

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.log10
import kotlin.math.sqrt

data class AudioClassifiedEvent(
    val eventLabel: String,      // e.g. "LOUD_IMPACT", "RAISED_VOICE", "AMBIENT_NORMAL"
    val decibels: Double,
    val confidence: Float,
    val timestampMs: Long
)

/**
 * Real-time, on-device audio event classifier.
 *
 * CRITICAL PRIVACY PRINCIPLE:
 * Raw audio PCM buffers are analyzed frame-by-frame in RAM ONLY to derive decibel level
 * and classify sound events. RAW AUDIO SAMPLES ARE IMMEDIATELY OVERWRITTEN AND DISCARDED.
 * NO RAW AUDIO IS EVER WRITTEN TO DISK OR PERSISTED.
 */
class AudioClassifierCollector(private val context: Context) {

    private val sampleRate = 16000
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT

    @SuppressLint("MissingPermission")
    fun observeAudioEvents(): Flow<AudioClassifiedEvent> = callbackFlow {
        val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        val audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            channelConfig,
            audioFormat,
            minBufferSize.coerceAtLeast(2048)
        )

        if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
            close()
            return@callbackFlow
        }

        audioRecord.startRecording()

        val job = launch(Dispatchers.IO) {
            val buffer = ShortArray(1024)
            while (isActive) {
                val readSize = audioRecord.read(buffer, 0, buffer.size)
                if (readSize > 0) {
                    var sum = 0.0
                    for (i in 0 until readSize) {
                        sum += buffer[i] * buffer[i]
                    }
                    val rms = sqrt(sum / readSize)
                    val db = if (rms > 0) 20 * log10(rms) else 0.0

                    // Classify sound based on frame acoustic parameters
                    val (label, confidence) = when {
                        db > 85.0 -> "LOUD_IMPACT" to 0.92f
                        db > 70.0 -> "RAISED_VOICE" to 0.85f
                        else -> "AMBIENT_NORMAL" to 0.99f
                    }

                    // ZERO OUT RAW BUFFER IMMEDIATELY (Privacy Enforcement)
                    buffer.fill(0)

                    if (label != "AMBIENT_NORMAL" || System.currentTimeMillis() % 15000 < 1000) {
                        trySend(
                            AudioClassifiedEvent(
                                eventLabel = label,
                                decibels = db,
                                confidence = confidence,
                                timestampMs = System.currentTimeMillis()
                            )
                        )
                    }
                }
            }
        }

        awaitClose {
            job.cancel()
            try {
                audioRecord.stop()
                audioRecord.release()
            } catch (e: Exception) {
                // ignore
            }
        }
    }
}
