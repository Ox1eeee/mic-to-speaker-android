package com.bluetooth.bluetoothmictospeaker.audio

import kotlin.math.abs
import kotlin.math.log2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

class EffectProcessor(private val sampleRate: Int = 44100) {

    // Echo/Reverb state
    private var echoBuffer: FloatArray = FloatArray(sampleRate) // 1 second delay buffer
    private var echoWriteIndex = 0

    // Separate pitch shift accumulators per context to avoid cross-contamination
    private val mainPitchAccum = DoubleArray(1)       // Chipmunk, DeepVoice
    private val alienPitchAccum = DoubleArray(1)      // Alien
    private val princessPitchAccum = DoubleArray(1)   // Princess main pitch
    private val autotuneEffect = AutotuneEffect(sampleRate)

    // Robot effect state
    private var robotPhase = 0.0
    private val robotFrequency = 100.0 // Hz for ring modulation

    // Radio filter state (must persist across buffer chunks)
    private var radioHighPassPrev = 0f
    private var radioHighPassOut = 0f
    private var radioLowPassOut = 0f

    // Princess chorus state — modulated delay line (smoother than pitch-shift chorus)
    private var chorusDelayBuffer = FloatArray(sampleRate)
    private var chorusWriteIndex = 0
    private var chorusPhase = 0.0

    // Princess shimmer reverb state — persistent across buffers for smooth tail
    private var shimmerBuffer = FloatArray(sampleRate)
    private var shimmerWriteIndex = 0

    var echoMix: Float = 0.3f

    // Crossfade length in samples for pitch shift buffer edges
    private val crossfadeSamples = 64

    fun applyEffect(buffer: ShortArray, size: Int, effect: VoiceEffect): ShortArray {
        return when (effect) {
            is VoiceEffect.None -> buffer.copyOf(size)
            is VoiceEffect.Robot -> applyRobot(buffer, size)
            is VoiceEffect.Chipmunk -> applyPitchShift(buffer, size, 2.0f, mainPitchAccum)
            is VoiceEffect.DeepVoice -> applyPitchShift(buffer, size, 0.6f, mainPitchAccum)
            is VoiceEffect.Echo -> applyEcho(buffer, size, 0.8f)
            is VoiceEffect.Alien -> applyAlien(buffer, size)
            is VoiceEffect.Radio -> applyRadio(buffer, size)
            is VoiceEffect.Princess -> applyPrincess(buffer, size)
            is VoiceEffect.Autotune -> applyAutotune(buffer, size)
        }
    }

    fun reset() {
        echoBuffer = FloatArray(sampleRate)
        echoWriteIndex = 0
        mainPitchAccum[0] = 0.0
        alienPitchAccum[0] = 0.0
        princessPitchAccum[0] = 0.0
        robotPhase = 0.0
        radioHighPassPrev = 0f
        radioHighPassOut = 0f
        radioLowPassOut = 0f
        chorusDelayBuffer = FloatArray(sampleRate)
        chorusWriteIndex = 0
        chorusPhase = 0.0
        shimmerBuffer = FloatArray(sampleRate)
        shimmerWriteIndex = 0
        autotuneEffect.reset()
    }

    // --- Pitch Shift (resampling with linear interpolation + crossfade) ---
    // Each caller passes its own accumulator to avoid state corruption.
    private fun applyPitchShift(
        buffer: ShortArray, size: Int, factor: Float, accumulator: DoubleArray
    ): ShortArray {
        val output = ShortArray(size)

        for (i in 0 until size) {
            val srcIndex = accumulator[0]
            val srcIndexInt = srcIndex.toInt()
            val frac = (srcIndex - srcIndexInt).toFloat()

            if (srcIndexInt >= 0 && srcIndexInt < size - 1) {
                // Linear interpolation between adjacent samples
                val sample = buffer[srcIndexInt] * (1f - frac) + buffer[srcIndexInt + 1] * frac
                output[i] = sample.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            } else if (srcIndexInt >= 0 && srcIndexInt < size) {
                output[i] = buffer[srcIndexInt]
            } else {
                // Source exhausted — fade to zero instead of looping (avoids click)
                output[i] = 0
            }

            accumulator[0] += factor

            // When source pointer runs past buffer end, wrap with crossfade
            if (accumulator[0] >= size) {
                accumulator[0] -= size

                // Apply crossfade around the wrap point to smooth the discontinuity
                val fadeStart = max(0, i - crossfadeSamples / 2)
                val fadeEnd = min(size - 1, i + crossfadeSamples / 2)
                for (j in fadeStart..fadeEnd) {
                    val progress = (j - fadeStart).toFloat() / (fadeEnd - fadeStart).coerceAtLeast(1)
                    // At the wrap point, briefly dip volume to mask the glitch
                    val fadeGain = if (j <= i) progress else 1f - (1f - progress) * 0.5f
                    output[j] = (output[j] * fadeGain).toInt()
                        .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }
            }
        }

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

        // High-pass: RC = 1/(2*PI*300), alpha = RC/(RC + dt)
        val dtHp = 1.0f / sampleRate
        val rcHp = 1.0f / (2.0f * Math.PI.toFloat() * 300f)
        val alphaHp = rcHp / (rcHp + dtHp)

        // Low-pass: RC = 1/(2*PI*3400), alpha = dt/(RC + dt)
        val rcLp = 1.0f / (2.0f * Math.PI.toFloat() * 3400f)
        val alphaLp = dtHp / (rcLp + dtHp)

        for (i in 0 until size) {
            val sample = buffer[i].toFloat()

            // High-pass filter (remove below 300 Hz)
            radioHighPassOut = alphaHp * (radioHighPassOut + sample - radioHighPassPrev)
            radioHighPassPrev = sample

            // Low-pass filter (remove above 3400 Hz)
            radioLowPassOut += alphaLp * (radioHighPassOut - radioLowPassOut)

            // Soft clipping saturation for AM radio character
            val gained = radioLowPassOut * 2.5f
            val processed = (gained / (1f + abs(gained / 20000f))).coerceIn(-30000f, 30000f)

            output[i] = processed.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return output
    }

    // --- Alien (Pitch shift + ring mod + reverb) ---
    private fun applyAlien(buffer: ShortArray, size: Int): ShortArray {
        // First apply pitch shift up (using its own accumulator)
        val pitched = applyPitchShift(buffer, size, 1.3f, alienPitchAccum)

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

    // =============================================
    // Princess Effect — fairy/cute feminine voice
    // Chain: Pitch shift +6 semitones → Modulated chorus → Shimmer reverb
    // =============================================

    private fun applyPrincess(buffer: ShortArray, size: Int): ShortArray {
        // Step 1: Pitch shift up +6 semitones (own accumulator — no conflicts)
        val pitchFactor = 2f.pow(6f / 12f)  // +6 semitones ≈ 1.498
        val pitched = applyPitchShift(buffer, size, pitchFactor, princessPitchAccum)

        // Step 2: Blend 80% pitched + 20% dry for softness
        val blended = ShortArray(size) { i ->
            ((pitched[i] * 0.80f) + (buffer[i] * 0.20f))
                .toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }

        // Step 3: Smooth modulated delay-line chorus (replaces harsh pitch-shift chorus)
        val chorused = applyChorus(blended, size)

        // Step 4: Persistent shimmer reverb with smooth tail across buffers
        return applyShimmerReverb(chorused, size)
    }

    /**
     * Modulated delay-line chorus for Princess sparkle effect.
     * Uses a circular delay buffer with sinusoidal modulation — much smoother than
     * pitch-shifting a copy because it avoids buffer-wrap artifacts entirely.
     */
    private fun applyChorus(input: ShortArray, size: Int): ShortArray {
        val output = ShortArray(size)
        val chorusRate = 1.5     // Hz — slow LFO for gentle shimmer
        val chorusDepth = 25     // samples — modulation depth
        val baseDelay = 40       // samples — base delay offset
        val wetMix = 0.30f       // chorus wet ≤ 35% (quality tip)
        val dryMix = 0.80f

        for (i in 0 until size) {
            // Write current sample into circular delay buffer
            chorusDelayBuffer[chorusWriteIndex] = input[i].toFloat()

            // Read from modulated position
            val modulation = (sin(chorusPhase) * chorusDepth).toInt()
            val readIndex = ((chorusWriteIndex - baseDelay - modulation + chorusDelayBuffer.size)
                % chorusDelayBuffer.size)
            val delayed = chorusDelayBuffer[readIndex]

            // Mix dry + wet
            val mixed = input[i] * dryMix + delayed * wetMix
            output[i] = mixed.toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()

            chorusWriteIndex = (chorusWriteIndex + 1) % chorusDelayBuffer.size
            chorusPhase += 2.0 * Math.PI * chorusRate / sampleRate
            if (chorusPhase > 2.0 * Math.PI) chorusPhase -= 2.0 * Math.PI
        }
        return output
    }

    /**
     * Persistent shimmer reverb using a circular delay buffer.
     * Maintains state across buffer boundaries for a smooth, continuous tail
     * instead of per-buffer processing which cuts the reverb at chunk edges.
     */
    private fun applyShimmerReverb(input: ShortArray, size: Int): ShortArray {
        val output = ShortArray(size)
        val delayMs = 80
        val delaySamples = (sampleRate * delayMs / 1000f).toInt()
        val decayFactor = 0.25f

        for (i in 0 until size) {
            val dry = input[i].toFloat()

            // Read delayed sample from circular buffer
            val readIndex = (shimmerWriteIndex - delaySamples + shimmerBuffer.size) % shimmerBuffer.size
            val delayed = shimmerBuffer[readIndex]

            // Mix dry + reverb tail
            val mixed = dry + delayed * decayFactor

            // Write mixed signal back for feedback
            shimmerBuffer[shimmerWriteIndex] = mixed

            shimmerWriteIndex = (shimmerWriteIndex + 1) % shimmerBuffer.size

            output[i] = mixed.toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return output
    }

    // =============================================
    // Autotune Effect — Instagram / T-Pain style
    // =============================================

    private fun applyAutotune(buffer: ShortArray, size: Int): ShortArray {
        return autotuneEffect.process(buffer, size)
    }
}
