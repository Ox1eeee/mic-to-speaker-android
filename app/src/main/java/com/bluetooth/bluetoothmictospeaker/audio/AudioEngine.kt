package com.bluetooth.bluetoothmictospeaker.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.util.Log

class AudioEngine {

    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null
    private var processingThread: Thread? = null

    // Lock to prevent concurrent start/stop races
    private val lock = Object()

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
            // Release any existing resources first to avoid leaks
            releaseResources()

            bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelInConfig, encoding)
            if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
                Log.e(TAG, "Invalid buffer size: $bufferSize")
                return false
            }

            val record = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelInConfig,
                encoding,
                bufferSize * 2
            )

            if (record.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord failed to initialize")
                record.release()
                return false
            }

            val track = AudioTrack.Builder()
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

            if (track.state != AudioTrack.STATE_INITIALIZED) {
                Log.e(TAG, "AudioTrack failed to initialize")
                record.release()
                track.release()
                return false
            }

            audioRecord = record
            audioTrack = track

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
        synchronized(lock) {
            if (isRunning) return

            if (audioRecord == null || audioTrack == null) {
                if (!setup()) return
            }

            isRunning = true
            effectProcessor.reset()

            try {
                audioRecord?.startRecording()
                audioTrack?.play()
            } catch (e: IllegalStateException) {
                Log.e(TAG, "Error starting audio: ${e.message}")
                isRunning = false
                return
            }

            processingThread = Thread({
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO)
                val buffer = ShortArray(bufferSize)

                while (isRunning) {
                    try {
                        val record = audioRecord ?: break
                        val track = audioTrack ?: break

                        val read = record.read(buffer, 0, bufferSize)
                        if (read <= 0 || !isRunning) continue

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

                        if (isRunning) {
                            track.write(processed, 0, read)
                        }
                    } catch (e: Exception) {
                        if (isRunning) {
                            Log.e(TAG, "Error in processing loop: ${e.message}")
                        }
                        break
                    }
                }
            }, "AudioProcessingThread")
            processingThread?.start()
            Log.d(TAG, "Audio engine started")
        }
    }

    fun stop() {
        synchronized(lock) {
            if (!isRunning) return

            isRunning = false

            // Stop AudioRecord first to unblock the read() call in the processing thread
            try {
                audioRecord?.stop()
            } catch (e: IllegalStateException) {
                Log.w(TAG, "AudioRecord stop error: ${e.message}")
            }

            // Now join the thread — it should exit quickly since read() will return an error
            try {
                processingThread?.join(2000)
            } catch (e: InterruptedException) {
                Log.w(TAG, "Thread join interrupted")
            }
            processingThread = null

            // Stop AudioTrack after thread is done
            try {
                audioTrack?.stop()
            } catch (e: IllegalStateException) {
                Log.w(TAG, "AudioTrack stop error: ${e.message}")
            }

            Log.d(TAG, "Audio engine stopped")
        }
    }

    fun release() {
        stop()
        releaseResources()
        Log.d(TAG, "Audio engine released")
    }

    private fun releaseResources() {
        try {
            audioRecord?.release()
        } catch (e: Exception) {
            Log.w(TAG, "AudioRecord release error: ${e.message}")
        }
        try {
            audioTrack?.release()
        } catch (e: Exception) {
            Log.w(TAG, "AudioTrack release error: ${e.message}")
        }
        audioRecord = null
        audioTrack = null
    }

    fun isActive(): Boolean = isRunning

    fun getSampleRate(): Int = sampleRate

    companion object {
        private const val TAG = "AudioEngine"
    }
}
