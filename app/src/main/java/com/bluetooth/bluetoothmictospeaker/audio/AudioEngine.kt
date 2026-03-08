package com.bluetooth.bluetoothmictospeaker.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.util.Log

class AudioEngine {

    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null
    private var processingThread: Thread? = null

    @Volatile
    private var isRunning = false

    @Volatile
    var volume: Float = 1.0f

    @Volatile
    var currentEffect: VoiceEffect = VoiceEffect.None

    val effectProcessor = EffectProcessor()

    private val sampleRate = 44100
    private val channelInConfig = AudioFormat.CHANNEL_IN_MONO
    private val channelOutConfig = AudioFormat.CHANNEL_OUT_MONO
    private val encoding = AudioFormat.ENCODING_PCM_16BIT
    private var bufferSize = 0

    var onAmplitudeUpdate: ((Float) -> Unit)? = null

    fun setup(): Boolean {
        return try {
            bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelInConfig, encoding)
            if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
                Log.e(TAG, "Invalid buffer size: $bufferSize")
                return false
            }

            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelInConfig,
                encoding,
                bufferSize * 2
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord failed to initialize")
                return false
            }

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
                        .setEncoding(encoding)
                        .setChannelMask(channelOutConfig)
                        .build()
                )
                .setTransferMode(AudioTrack.MODE_STREAM)
                .setBufferSizeInBytes(bufferSize * 2)
                .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
                .build()

            if (audioTrack?.state != AudioTrack.STATE_INITIALIZED) {
                Log.e(TAG, "AudioTrack failed to initialize")
                return false
            }

            Log.d(TAG, "Audio engine setup complete. Buffer size: $bufferSize")
            true
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied: ${e.message}")
            false
        } catch (e: Exception) {
            Log.e(TAG, "Setup failed: ${e.message}")
            false
        }
    }

    fun start() {
        if (isRunning) return

        if (audioRecord == null || audioTrack == null) {
            if (!setup()) return
        }

        isRunning = true
        effectProcessor.reset()
        audioRecord?.startRecording()
        audioTrack?.play()

        processingThread = Thread {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO)
            val buffer = ShortArray(bufferSize)

            while (isRunning) {
                val read = audioRecord?.read(buffer, 0, bufferSize) ?: 0
                if (read > 0) {
                    // Calculate amplitude for visualization
                    var maxAmplitude = 0
                    for (i in 0 until read) {
                        val abs = kotlin.math.abs(buffer[i].toInt())
                        if (abs > maxAmplitude) maxAmplitude = abs
                    }
                    val normalizedAmplitude = maxAmplitude / 32768f
                    onAmplitudeUpdate?.invoke(normalizedAmplitude)

                    // Apply voice effect
                    val processed = effectProcessor.applyEffect(buffer, read, currentEffect)

                    // Apply volume
                    if (volume != 1.0f) {
                        for (i in 0 until read) {
                            processed[i] = (processed[i] * volume).toInt()
                                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                                .toShort()
                        }
                    }

                    audioTrack?.write(processed, 0, read)
                }
            }
        }
        processingThread?.start()
        Log.d(TAG, "Audio engine started")
    }

    fun stop() {
        isRunning = false
        processingThread?.join(1000)
        processingThread = null

        try {
            audioRecord?.stop()
            audioTrack?.stop()
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Error stopping audio: ${e.message}")
        }
        Log.d(TAG, "Audio engine stopped")
    }

    fun release() {
        stop()
        audioRecord?.release()
        audioTrack?.release()
        audioRecord = null
        audioTrack = null
        Log.d(TAG, "Audio engine released")
    }

    fun isActive(): Boolean = isRunning

    fun getSampleRate(): Int = sampleRate

    companion object {
        private const val TAG = "AudioEngine"
    }
}
