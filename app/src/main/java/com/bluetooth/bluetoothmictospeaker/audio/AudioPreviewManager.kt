package com.bluetooth.bluetoothmictospeaker.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sin

class AudioPreviewManager(private val context: Context) {

    private var audioTrack: AudioTrack? = null
    private var previewJob: Job? = null
    private val sampleRate = 44100
    private val effectProcessor = EffectProcessor(sampleRate)

    fun playEffectPreview(effect: VoiceEffect) {
        stopPreview()

        previewJob = CoroutineScope(Dispatchers.IO).launch {
            val bufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )

            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(sampleRate)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioTrack?.play()
            effectProcessor.reset()

            // Generate a synthetic voice-like tone (2 seconds)
            val totalSamples = sampleRate * 2
            val chunkSize = bufferSize / 2
            var sampleIndex = 0

            while (isActive && sampleIndex < totalSamples) {
                val remaining = (totalSamples - sampleIndex).coerceAtMost(chunkSize)
                val buffer = ShortArray(remaining)

                // Generate a rich synthetic tone simulating speech formants
                for (i in 0 until remaining) {
                    val t = (sampleIndex + i).toFloat() / sampleRate
                    val fundamental = sin(2.0 * Math.PI * 150.0 * t).toFloat()
                    val formant1 = 0.6f * sin(2.0 * Math.PI * 400.0 * t).toFloat()
                    val formant2 = 0.3f * sin(2.0 * Math.PI * 850.0 * t).toFloat()
                    val formant3 = 0.15f * sin(2.0 * Math.PI * 2400.0 * t).toFloat()

                    // Amplitude envelope (fade in/out)
                    val envelope = when {
                        t < 0.1f -> t / 0.1f
                        t > 1.9f -> (2.0f - t) / 0.1f
                        else -> 1.0f
                    }

                    val sample = (fundamental + formant1 + formant2 + formant3) * envelope * 8000f
                    buffer[i] = sample.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }

                // Apply effect
                val processed = effectProcessor.applyEffect(buffer, remaining, effect)
                audioTrack?.write(processed, 0, remaining)
                sampleIndex += remaining
            }

            audioTrack?.stop()
        }
    }

    fun stopPreview() {
        previewJob?.cancel()
        previewJob = null
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (_: IllegalStateException) { }
        audioTrack = null
        effectProcessor.reset()
    }

    fun release() {
        stopPreview()
    }
}
