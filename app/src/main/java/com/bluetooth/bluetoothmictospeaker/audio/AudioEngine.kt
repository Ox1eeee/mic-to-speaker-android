package com.bluetooth.bluetoothmictospeaker.audio

import android.util.Log

class AudioEngine {

    // Lock to prevent concurrent start/stop races
    private val lock = Object()

    @Volatile
    private var isRunning = false

    @Volatile
    var volume: Float = 1.0f
        set(value) {
            field = value
            NativeAudioBridge.nativeSetVolume(value)
        }

    @Volatile
    var currentEffect: VoiceEffect = VoiceEffect.None
        set(value) {
            field = value
            NativeAudioBridge.nativeSetEffect(VoiceEffect.nativeId(value))
        }

    var echoMix: Float = 0.3f
        set(value) {
            field = value
            NativeAudioBridge.nativeSetEchoMix(value)
        }

    var onAmplitudeUpdate: ((Float) -> Unit)? = null

    // Amplitude polling thread
    private var amplitudeThread: Thread? = null
    @Volatile
    private var pollAmplitude = false

    init {
        NativeAudioBridge.nativeCreate()
    }

    fun setup(): Boolean {
        // Native engine is created in init, nothing else needed
        return true
    }

    fun start() {
        synchronized(lock) {
            if (isRunning) return

            try {
                val success = NativeAudioBridge.nativeStart()
                if (!success) {
                    Log.e(TAG, "Native audio engine failed to start")
                    return
                }
                isRunning = true

                // Start amplitude polling for UI visualization
                pollAmplitude = true
                amplitudeThread = Thread({
                    while (pollAmplitude) {
                        try {
                            val amp = NativeAudioBridge.nativeGetAmplitude()
                            onAmplitudeUpdate?.invoke(amp)
                            Thread.sleep(33) // ~30fps update
                        } catch (e: InterruptedException) {
                            break
                        }
                    }
                }, "AmplitudePollThread")
                amplitudeThread?.start()

                Log.d(TAG, "Audio engine started (Oboe native)")
            } catch (e: Exception) {
                Log.e(TAG, "Error starting audio: ${e.message}")
            }
        }
    }

    fun stop() {
        synchronized(lock) {
            if (!isRunning) return
            isRunning = false

            pollAmplitude = false
            try {
                amplitudeThread?.join(500)
            } catch (_: InterruptedException) {}
            amplitudeThread = null

            try {
                NativeAudioBridge.nativeStop()
            } catch (e: Exception) {
                Log.w(TAG, "Error stopping native audio: ${e.message}")
            }

            Log.d(TAG, "Audio engine stopped")
        }
    }

    fun release() {
        stop()
        try {
            NativeAudioBridge.nativeDestroy()
        } catch (e: Exception) {
            Log.w(TAG, "Error destroying native audio: ${e.message}")
        }
        // Recreate for potential restart
        NativeAudioBridge.nativeCreate()
        Log.d(TAG, "Audio engine released")
    }

    fun isActive(): Boolean = isRunning

    fun getSampleRate(): Int = NativeAudioBridge.nativeGetSampleRate()

    companion object {
        private const val TAG = "AudioEngine"
    }
}
