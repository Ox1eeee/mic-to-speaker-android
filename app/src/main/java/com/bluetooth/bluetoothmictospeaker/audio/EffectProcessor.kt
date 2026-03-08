package com.bluetooth.bluetoothmictospeaker.audio

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

class EffectProcessor(private val sampleRate: Int = 44100) {

    // Echo/Reverb state
    private var echoBuffer: FloatArray = FloatArray(sampleRate) // 1 second delay buffer
    private var echoWriteIndex = 0

    // Pitch shift state
    private var pitchAccumulator = 0.0

    // Robot effect state
    private var robotPhase = 0.0
    private val robotFrequency = 100.0 // Hz for ring modulation

    var echoMix: Float = 0.3f

    fun applyEffect(buffer: ShortArray, size: Int, effect: VoiceEffect): ShortArray {
        return when (effect) {
            is VoiceEffect.None -> buffer.copyOf(size)
            is VoiceEffect.Robot -> applyRobot(buffer, size)
            is VoiceEffect.Chipmunk -> applyPitchShift(buffer, size, 2.0f)
            is VoiceEffect.DeepVoice -> applyPitchShift(buffer, size, 0.6f)
            is VoiceEffect.Echo -> applyEcho(buffer, size, 0.8f)
            is VoiceEffect.Alien -> applyAlien(buffer, size)
            is VoiceEffect.Radio -> applyRadio(buffer, size)
        }
    }

    fun reset() {
        echoBuffer = FloatArray(sampleRate)
        echoWriteIndex = 0
        pitchAccumulator = 0.0
        robotPhase = 0.0
    }

    // --- Pitch Shift (simple resampling) ---
    private fun applyPitchShift(buffer: ShortArray, size: Int, factor: Float): ShortArray {
        val outputSize = (size / factor).toInt()
        val output = ShortArray(size)

        for (i in 0 until size) {
            val srcIndex = pitchAccumulator
            val srcIndexInt = srcIndex.toInt()
            val frac = (srcIndex - srcIndexInt).toFloat()

            if (srcIndexInt >= 0 && srcIndexInt < size - 1) {
                val sample = buffer[srcIndexInt] * (1f - frac) + buffer[srcIndexInt + 1] * frac
                output[i] = sample.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            } else if (srcIndexInt >= 0 && srcIndexInt < size) {
                output[i] = buffer[srcIndexInt]
            } else {
                output[i] = 0
            }

            pitchAccumulator += factor
            if (pitchAccumulator >= size) {
                pitchAccumulator -= size
            }
        }
        // Reset accumulator at block boundary to avoid drift
        pitchAccumulator = pitchAccumulator % size

        return output
    }

    // --- Echo / Reverb ---
    private fun applyEcho(buffer: ShortArray, size: Int, wetMix: Float): ShortArray {
        val output = ShortArray(size)
        val delayMs = 250
        val delaySamples = (sampleRate * delayMs / 1000)
        val feedback = 0.4f
        val effectiveWet = wetMix * echoMix / 0.3f // Scale by echoMix slider

        for (i in 0 until size) {
            val dry = buffer[i].toFloat()

            // Read from delay buffer
            val readIndex = (echoWriteIndex - delaySamples + echoBuffer.size) % echoBuffer.size
            val delayed = echoBuffer[readIndex]

            // Mix
            val mixed = dry + delayed * effectiveWet.coerceIn(0f, 0.95f)

            // Write back to delay buffer with feedback
            echoBuffer[echoWriteIndex] = dry + delayed * feedback

            echoWriteIndex = (echoWriteIndex + 1) % echoBuffer.size

            output[i] = mixed.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return output
    }

    // --- Robot (Ring Modulation + slight distortion) ---
    private fun applyRobot(buffer: ShortArray, size: Int): ShortArray {
        val output = ShortArray(size)
        val phaseIncrement = 2.0 * Math.PI * robotFrequency / sampleRate

        for (i in 0 until size) {
            val sample = buffer[i].toFloat()

            // Ring modulation with sine wave
            val modulator = sin(robotPhase).toFloat()
            var processed = sample * modulator

            // Add slight hard clipping for metallic feel
            processed = (processed * 1.5f).coerceIn(-24000f, 24000f)

            robotPhase += phaseIncrement
            if (robotPhase > 2.0 * Math.PI) robotPhase -= 2.0 * Math.PI

            output[i] = processed.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return output
    }

    // --- Radio (Bandpass filter + mild distortion) ---
    private fun applyRadio(buffer: ShortArray, size: Int): ShortArray {
        val output = ShortArray(size)

        // Simple IIR bandpass filter coefficients for 300-3400 Hz
        val lowCut = 300f / sampleRate
        val highCut = 3400f / sampleRate
        val rc1 = 1f / (2f * Math.PI.toFloat() * lowCut)
        val rc2 = 1f / (2f * Math.PI.toFloat() * highCut)
        val dt = 1f / sampleRate
        val alpha1 = dt / (rc1 + dt)
        val alpha2 = rc2 / (rc2 + dt)

        var highPassPrev = 0f
        var highPassOut = 0f
        var lowPassOut = 0f

        for (i in 0 until size) {
            val sample = buffer[i].toFloat()

            // High-pass filter (remove below 300 Hz)
            highPassOut = alpha2 * (highPassOut + sample - highPassPrev)
            highPassPrev = sample

            // Low-pass filter (remove above 3400 Hz)
            lowPassOut += alpha1 * (highPassOut - lowPassOut)

            // Add mild saturation for radio character
            var processed = lowPassOut * 1.8f
            processed = if (processed > 0) {
                min(processed, 20000f)
            } else {
                max(processed, -20000f)
            }

            output[i] = processed.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return output
    }

    // --- Alien (Pitch shift + ring mod + reverb) ---
    private fun applyAlien(buffer: ShortArray, size: Int): ShortArray {
        // First apply pitch shift up
        val pitched = applyPitchShift(buffer, size, 1.3f)

        // Then apply ring modulation at a different frequency
        val alienFreq = 150.0
        val phaseInc = 2.0 * Math.PI * alienFreq / sampleRate
        var phase = robotPhase

        val modulated = ShortArray(size)
        for (i in 0 until size) {
            val sample = pitched[i].toFloat()
            val mod = sin(phase).toFloat()
            val processed = sample * (0.6f + 0.4f * mod) // Partial ring mod
            phase += phaseInc
            if (phase > 2.0 * Math.PI) phase -= 2.0 * Math.PI
            modulated[i] = processed.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }

        // Apply light echo
        return applyEcho(modulated, size, 0.3f)
    }
}
