package com.bluetooth.bluetoothmictospeaker.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.sin

/**
 * Plays the short synthesized voice tones used on the first onboarding screen.
 * Mirrors the iOS AudioPreviewManager: 1.5 s, 44.1 kHz mono 16-bit, no audio focus.
 */
class OnboardingTonePlayer {

    private val cache = HashMap<String, ShortArray>()
    private var track: AudioTrack? = null

    fun play(key: String) {
        stop()
        val pcm = cache.getOrPut(key) { generate(key) }
        track = runCatching {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(SAMPLE_RATE)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(pcm.size * 2)
                .build()
                .apply {
                    write(pcm, 0, pcm.size)
                    play()
                }
        }.getOrNull()
    }

    fun stop() {
        track?.let {
            runCatching { it.stop() }
            it.release()
        }
        track = null
    }

    fun release() = stop()

    companion object {
        private const val SAMPLE_RATE = 44_100
        private const val SAMPLE_COUNT = 66_150
        private const val FADE = 2_205
        private const val BASE_FREQ = 200f
        private const val TWO_PI = (2 * PI).toFloat()
        private const val PI_F = PI.toFloat()

        fun generate(key: String): ShortArray {
            val f = BASE_FREQ
            val samples = FloatArray(SAMPLE_COUNT)
            val echoDelay = (0.15f * SAMPLE_RATE).toInt()
            for (i in 0 until SAMPLE_COUNT) {
                val time = i / SAMPLE_RATE.toFloat()
                var s = when (key) {
                    "Robot" -> if (sin(time * f * TWO_PI) > 0) 0.3f else -0.3f
                    "Chipmunk" -> sin(time * (f * 2.5f) * TWO_PI) * 0.4f
                    "Demon" -> {
                        val df = f * 0.25f
                        sin(time * df * TWO_PI) * 0.6f +
                            sin(time * df * 3 * PI_F) * 0.25f +
                            sin(time * df * 5 * PI_F) * 0.15f
                    }
                    "Echo Chamber" -> {
                        val v = sin(time * f * TWO_PI) * 0.4f
                        val j = i - echoDelay
                        v + if (j >= 0) samples[j] * 0.5f else 0f
                    }
                    "Helium" -> sin(time * (f * 3.0f) * TWO_PI) * 0.35f
                    "Ghost" -> {
                        val mod = sin(time * 2 * TWO_PI) * 0.3f + 0.7f
                        sin(time * (f * 1.3f) * TWO_PI) * 0.35f * mod
                    }
                    else -> sin(time * f * TWO_PI) * 0.3f
                }
                if (i < FADE) {
                    s *= i / FADE.toFloat()
                } else if (i > SAMPLE_COUNT - FADE) {
                    s *= (SAMPLE_COUNT - i) / FADE.toFloat()
                }
                samples[i] = s
            }
            return ShortArray(SAMPLE_COUNT) { (samples[it].coerceIn(-1f, 1f) * 32767).toInt().toShort() }
        }
    }
}
