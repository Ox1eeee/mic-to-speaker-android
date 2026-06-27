package com.bluetooth.bluetoothmictospeaker.audio

import android.util.Log

object NativeAudioBridge {

    private const val TAG = "NativeAudioBridge"

    init {
        try {
            System.loadLibrary("mic_to_speaker_native")
            Log.i(TAG, "Native library loaded successfully")
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "Failed to load native library: ${e.message}")
        }
    }

    external fun nativeCreate()
    external fun nativeStart(): Boolean
    external fun nativeStop()
    external fun nativeDestroy()
    external fun nativeSetVolume(volume: Float)
    external fun nativeSetEffect(effectId: Int)
    external fun nativeSetEchoMix(mix: Float)
    external fun nativeIsRunning(): Boolean
    external fun nativeGetAmplitude(): Float
    external fun nativeGetSampleRate(): Int
}
