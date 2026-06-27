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

    // Underwater LFO state
    private var underwaterPhase = 0.0

    // Drunk wobble state
    private var drunkPhase = 0.0
    private var drunkPitchAccum = DoubleArray(1)

    // Giant effect accumulator
    private var giantPitchAccum = DoubleArray(1)

    // Monster reverb buffer
    private var monsterReverbBuffer = FloatArray(sampleRate)
    private var monsterReverbIndex = 0

    // Zombie reverb buffer
    private var zombieReverbBuffer = FloatArray(sampleRate)
    private var zombieReverbIndex = 0

    // Cave/Cathedral Schroeder reverb — 4 comb filters + 2 allpass filters
    private val combDelays = intArrayOf(1116, 1188, 1277, 1356)
    private val combBuffers = Array(4) { FloatArray(combDelays[it] + 1) }
    private val combIndices = IntArray(4)
    private val allpassDelays = intArrayOf(225, 556)
    private val allpassBuffers = Array(2) { FloatArray(allpassDelays[it] + 1) }
    private val allpassIndices = IntArray(2)

    // Ghost whisper state
    private var ghostPitchAccum = DoubleArray(1)
    private var ghostReverbBuffer = FloatArray(sampleRate * 2)  // 2s for long reverb
    private var ghostReverbIndex = 0

    // Darth Vader state
    private var vaderPitchAccum = DoubleArray(1)
    private var vaderRingPhase = 0.0
    private var vaderReverbBuffer = FloatArray(sampleRate)
    private var vaderReverbIndex = 0

    // Old Man state
    private var oldManPitchAccum = DoubleArray(1)
    private var oldManTremoloPhase = 0.0

    // Astronaut/Space state
    private var astronautFlangerBuffer = FloatArray(sampleRate)
    private var astronautFlangerIndex = 0
    private var astronautFlangerPhase = 0.0
    private var astronautReverbBuffer = FloatArray(sampleRate * 2)
    private var astronautReverbIndex = 0

    // 8-Bit state
    private var eightBitHoldSample: Short = 0
    private var eightBitHoldCounter = 0

    // Stadium reverb — long decay with early reflections
    private val stadiumEarlyDelays = intArrayOf(882, 1984, 2955)  // 20ms, 45ms, 67ms at 44100
    private val stadiumEarlyBuffers = Array(3) { FloatArray(stadiumEarlyDelays[it] + 1) }
    private val stadiumEarlyIndices = IntArray(3)
    private var stadiumLateBuffer = FloatArray(sampleRate * 3)  // 3s for very long reverb
    private var stadiumLateIndex = 0

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
            is VoiceEffect.Helium -> applyPitchShift(buffer, size, 1.8f, mainPitchAccum)
            is VoiceEffect.Monster -> applyMonster(buffer, size)
            is VoiceEffect.Telephone -> applyTelephone(buffer, size)
            is VoiceEffect.Underwater -> applyUnderwater(buffer, size)
            is VoiceEffect.Megaphone -> applyMegaphone(buffer, size)
            is VoiceEffect.Baby -> applyPitchShift(buffer, size, 1.5f, mainPitchAccum)
            is VoiceEffect.WalkieTalkie -> applyWalkieTalkie(buffer, size)
            is VoiceEffect.Drunk -> applyDrunk(buffer, size)
            is VoiceEffect.Zombie -> applyZombie(buffer, size)
            is VoiceEffect.Giant -> applyGiant(buffer, size)
            is VoiceEffect.Cave -> applyCave(buffer, size)
            is VoiceEffect.Ghost -> applyGhost(buffer, size)
            is VoiceEffect.DarthVader -> applyDarthVader(buffer, size)
            is VoiceEffect.OldMan -> applyOldMan(buffer, size)
            is VoiceEffect.Astronaut -> applyAstronaut(buffer, size)
            is VoiceEffect.EightBit -> applyEightBit(buffer, size)
            is VoiceEffect.Stadium -> applyStadium(buffer, size)
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
        underwaterPhase = 0.0
        drunkPhase = 0.0
        drunkPitchAccum[0] = 0.0
        giantPitchAccum[0] = 0.0
        monsterReverbBuffer = FloatArray(sampleRate)
        monsterReverbIndex = 0
        zombieReverbBuffer = FloatArray(sampleRate)
        zombieReverbIndex = 0
        // Phase 2 resets
        for (i in combBuffers.indices) { combBuffers[i].fill(0f); combIndices[i] = 0 }
        for (i in allpassBuffers.indices) { allpassBuffers[i].fill(0f); allpassIndices[i] = 0 }
        ghostPitchAccum[0] = 0.0
        ghostReverbBuffer = FloatArray(sampleRate * 2)
        ghostReverbIndex = 0
        vaderPitchAccum[0] = 0.0
        vaderRingPhase = 0.0
        vaderReverbBuffer = FloatArray(sampleRate)
        vaderReverbIndex = 0
        oldManPitchAccum[0] = 0.0
        oldManTremoloPhase = 0.0
        astronautFlangerBuffer = FloatArray(sampleRate)
        astronautFlangerIndex = 0
        astronautFlangerPhase = 0.0
        astronautReverbBuffer = FloatArray(sampleRate * 2)
        astronautReverbIndex = 0
        eightBitHoldSample = 0
        eightBitHoldCounter = 0
        for (i in stadiumEarlyBuffers.indices) { stadiumEarlyBuffers[i].fill(0f); stadiumEarlyIndices[i] = 0 }
        stadiumLateBuffer = FloatArray(sampleRate * 3)
        stadiumLateIndex = 0
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
    // Monster/Demon — Pitch down + distortion + short reverb
    // =============================================

    private fun applyMonster(buffer: ShortArray, size: Int): ShortArray {
        // Step 1: Pitch shift down
        val pitched = applyPitchShift(buffer, size, 0.5f, mainPitchAccum)

        // Step 2: Soft clip distortion + short reverb
        val output = ShortArray(size)
        val delaySamples = (sampleRate * 0.08f).toInt()  // 80ms reverb
        val feedback = 0.3f

        for (i in 0 until size) {
            val sample = pitched[i].toFloat()

            // Soft distortion: tanh approximation
            val gained = sample * 2.0f / 32768f
            val distorted = (gained / (1f + abs(gained))) * 32768f

            // Short reverb
            val readIdx = (monsterReverbIndex - delaySamples + monsterReverbBuffer.size) % monsterReverbBuffer.size
            val delayed = monsterReverbBuffer[readIdx]
            val mixed = distorted + delayed * feedback
            monsterReverbBuffer[monsterReverbIndex] = mixed
            monsterReverbIndex = (monsterReverbIndex + 1) % monsterReverbBuffer.size

            output[i] = mixed.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return output
    }

    // =============================================
    // Telephone — Band-pass (300-3400Hz) + saturation
    // =============================================

    private fun applyTelephone(buffer: ShortArray, size: Int): ShortArray {
        val output = ShortArray(size)
        val dt = 1.0f / sampleRate

        // High-pass at 300Hz
        val rcHp = 1.0f / (2.0f * Math.PI.toFloat() * 300f)
        val alphaHp = rcHp / (rcHp + dt)

        // Low-pass at 3400Hz
        val rcLp = 1.0f / (2.0f * Math.PI.toFloat() * 3400f)
        val alphaLp = dt / (rcLp + dt)

        var hpPrev = 0f
        var hpOut = 0f
        var lpOut = 0f

        for (i in 0 until size) {
            val sample = buffer[i].toFloat()

            // High-pass
            hpOut = alphaHp * (hpOut + sample - hpPrev)
            hpPrev = sample

            // Low-pass
            lpOut += alphaLp * (hpOut - lpOut)

            // Saturation + bit reduction for phone crunch
            val gained = lpOut * 2.0f
            val saturated = gained / (1f + abs(gained / 25000f))
            val crushed = (saturated.toInt() / 64 * 64).toFloat()  // Slight bit crush

            output[i] = crushed.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return output
    }

    // =============================================
    // Underwater — Low-pass filter + LFO wobble
    // =============================================

    private fun applyUnderwater(buffer: ShortArray, size: Int): ShortArray {
        val output = ShortArray(size)
        val lfoRate = 0.5  // Hz — slow wobble
        val lfoPhaseInc = 2.0 * Math.PI * lfoRate / sampleRate

        var lpOut = 0f

        for (i in 0 until size) {
            val sample = buffer[i].toFloat()

            // LFO modulates cutoff between 300-700Hz
            val lfo = sin(underwaterPhase).toFloat()
            val cutoff = 500f + lfo * 200f
            val rc = 1.0f / (2.0f * Math.PI.toFloat() * cutoff)
            val dt = 1.0f / sampleRate
            val alpha = dt / (rc + dt)

            // Low-pass filter
            lpOut += alpha * (sample - lpOut)

            // Slight volume reduction for muffled feel
            val muffled = lpOut * 0.7f

            underwaterPhase += lfoPhaseInc
            if (underwaterPhase > 2.0 * Math.PI) underwaterPhase -= 2.0 * Math.PI

            output[i] = muffled.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return output
    }

    // =============================================
    // Megaphone — Band-pass (500-4000Hz) + hard distortion
    // =============================================

    private fun applyMegaphone(buffer: ShortArray, size: Int): ShortArray {
        val output = ShortArray(size)
        val dt = 1.0f / sampleRate

        // High-pass at 500Hz
        val rcHp = 1.0f / (2.0f * Math.PI.toFloat() * 500f)
        val alphaHp = rcHp / (rcHp + dt)

        // Low-pass at 4000Hz
        val rcLp = 1.0f / (2.0f * Math.PI.toFloat() * 4000f)
        val alphaLp = dt / (rcLp + dt)

        var hpPrev = 0f
        var hpOut = 0f
        var lpOut = 0f

        for (i in 0 until size) {
            val sample = buffer[i].toFloat()

            // High-pass
            hpOut = alphaHp * (hpOut + sample - hpPrev)
            hpPrev = sample

            // Low-pass
            lpOut += alphaLp * (hpOut - lpOut)

            // Hard clip distortion — aggressive
            val gained = lpOut * 3.0f
            val clipped = gained.coerceIn(-22000f, 22000f)

            output[i] = clipped.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return output
    }

    // =============================================
    // Walkie-Talkie — Narrow band-pass + noise + distortion
    // =============================================

    private fun applyWalkieTalkie(buffer: ShortArray, size: Int): ShortArray {
        val output = ShortArray(size)
        val dt = 1.0f / sampleRate

        // Narrow band-pass: 1000-3000Hz
        val rcHp = 1.0f / (2.0f * Math.PI.toFloat() * 1000f)
        val alphaHp = rcHp / (rcHp + dt)
        val rcLp = 1.0f / (2.0f * Math.PI.toFloat() * 3000f)
        val alphaLp = dt / (rcLp + dt)

        var hpPrev = 0f
        var hpOut = 0f
        var lpOut = 0f

        for (i in 0 until size) {
            val sample = buffer[i].toFloat()

            // High-pass
            hpOut = alphaHp * (hpOut + sample - hpPrev)
            hpPrev = sample

            // Low-pass
            lpOut += alphaLp * (hpOut - lpOut)

            // Add noise
            val noise = (Math.random().toFloat() - 0.5f) * 800f

            // Soft distortion
            val gained = (lpOut + noise) * 2.0f
            val distorted = gained / (1f + abs(gained / 20000f))

            // Downsample effect (hold every 2nd sample)
            val crushed = if (i % 2 == 0) distorted else output.getOrNull(i - 1)?.toFloat() ?: distorted

            output[i] = crushed.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return output
    }

    // =============================================
    // Drunk — Slow pitch wobble via LFO
    // =============================================

    private fun applyDrunk(buffer: ShortArray, size: Int): ShortArray {
        val output = ShortArray(size)
        val lfoRate = 0.3  // Hz — very slow wobble
        val lfoPhaseInc = 2.0 * Math.PI * lfoRate / sampleRate
        val wobbleDepth = 0.05f  // ±5% pitch variation

        for (i in 0 until size) {
            // LFO modulates pitch factor between 0.95 - 1.05
            val lfo = sin(drunkPhase).toFloat()
            val factor = 1.0f + lfo * wobbleDepth

            val srcIndex = drunkPitchAccum[0]
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

            drunkPitchAccum[0] += factor
            if (drunkPitchAccum[0] >= size) {
                drunkPitchAccum[0] -= size
            }

            drunkPhase += lfoPhaseInc
            if (drunkPhase > 2.0 * Math.PI) drunkPhase -= 2.0 * Math.PI
        }
        return output
    }

    // =============================================
    // Zombie — Pitch down + growl distortion + reverb
    // =============================================

    private fun applyZombie(buffer: ShortArray, size: Int): ShortArray {
        // Step 1: Pitch shift down
        val pitched = applyPitchShift(buffer, size, 0.7f, mainPitchAccum)

        // Step 2: Asymmetric distortion (growl) + medium reverb
        val output = ShortArray(size)
        val delaySamples = (sampleRate * 0.15f).toInt()  // 150ms reverb
        val feedback = 0.4f

        for (i in 0 until size) {
            val sample = pitched[i].toFloat()

            // Asymmetric clipping — positive clips harder (growl character)
            val gained = sample * 1.8f
            val distorted = if (gained > 0) {
                min(gained, 18000f)
            } else {
                max(gained, -25000f)
            }

            // Medium reverb
            val readIdx = (zombieReverbIndex - delaySamples + zombieReverbBuffer.size) % zombieReverbBuffer.size
            val delayed = zombieReverbBuffer[readIdx]
            val mixed = distorted + delayed * feedback
            zombieReverbBuffer[zombieReverbIndex] = mixed
            zombieReverbIndex = (zombieReverbIndex + 1) % zombieReverbBuffer.size

            // Low-pass to darken the sound
            val darkened = mixed * 0.85f

            output[i] = darkened.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return output
    }

    // =============================================
    // Giant/T-Rex — Very low pitch + reverb + bass boost
    // =============================================

    private fun applyGiant(buffer: ShortArray, size: Int): ShortArray {
        // Step 1: Very low pitch (0.4x)
        val pitched = applyPitchShift(buffer, size, 0.4f, giantPitchAccum)

        // Step 2: Bass boost + long reverb
        val output = ShortArray(size)
        val delaySamples = (sampleRate * 0.25f).toInt()  // 250ms (large space)
        val feedback = 0.35f
        var lpOut = 0f
        val dt = 1.0f / sampleRate
        val rcLp = 1.0f / (2.0f * Math.PI.toFloat() * 300f)
        val alphaLp = dt / (rcLp + dt)

        for (i in 0 until size) {
            val sample = pitched[i].toFloat()

            // Bass boost: extract and amplify low freqs
            lpOut += alphaLp * (sample - lpOut)
            val boosted = sample + lpOut * 0.5f

            // Long reverb
            val readIdx = (monsterReverbIndex - delaySamples + monsterReverbBuffer.size) % monsterReverbBuffer.size
            val delayed = monsterReverbBuffer[readIdx]
            val mixed = boosted + delayed * feedback
            monsterReverbBuffer[monsterReverbIndex] = mixed
            monsterReverbIndex = (monsterReverbIndex + 1) % monsterReverbBuffer.size

            output[i] = mixed.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return output
    }

    // =============================================
    // Cave/Cathedral — Schroeder reverb (4 comb + 2 allpass)
    // =============================================

    private fun applyCave(buffer: ShortArray, size: Int): ShortArray {
        val output = ShortArray(size)
        val feedback = 0.84f
        val wetMix = 0.6f
        val dryMix = 0.4f

        for (i in 0 until size) {
            val input = buffer[i].toFloat() / 32768f

            // Parallel comb filters
            var combSum = 0f
            for (c in 0 until 4) {
                val delay = combDelays[c]
                val buf = combBuffers[c]
                val readIdx = (combIndices[c] - delay + buf.size) % buf.size
                val delayed = buf[readIdx]
                val combOut = input + feedback * delayed
                buf[combIndices[c]] = combOut
                combIndices[c] = (combIndices[c] + 1) % buf.size
                combSum += delayed
            }
            combSum /= 4f

            // Series allpass filters
            var allpassOut = combSum
            for (a in 0 until 2) {
                val delay = allpassDelays[a]
                val buf = allpassBuffers[a]
                val readIdx = (allpassIndices[a] - delay + buf.size) % buf.size
                val delayed = buf[readIdx]
                val apInput = allpassOut
                allpassOut = -0.5f * apInput + delayed
                buf[allpassIndices[a]] = apInput + 0.5f * delayed
                allpassIndices[a] = (allpassIndices[a] + 1) % buf.size
            }

            // Mix dry + wet
            val mixed = (input * dryMix + allpassOut * wetMix) * 32768f

            output[i] = mixed.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return output
    }

    // =============================================
    // Ghost/Whisper — breathy noise + octave up ghost + long reverb
    // =============================================

    private fun applyGhost(buffer: ShortArray, size: Int): ShortArray {
        // Pitched-up octave copy (faint)
        val octaveUp = applyPitchShift(buffer, size, 2.0f, ghostPitchAccum)

        val output = ShortArray(size)
        val delaySamples = (sampleRate * 0.8f).toInt()  // 800ms long reverb
        val feedback = 0.5f

        for (i in 0 until size) {
            val dry = buffer[i].toFloat() * 0.30f           // Original at 30%
            val pitched = octaveUp[i].toFloat() * 0.20f     // Octave up at 20%

            // White noise shaped by input envelope
            val envelope = abs(buffer[i].toFloat()) / 32768f
            val noise = (Math.random().toFloat() - 0.5f) * 32768f * 0.15f * envelope

            val combined = dry + pitched + noise

            // Long reverb
            val readIdx = (ghostReverbIndex - delaySamples + ghostReverbBuffer.size) % ghostReverbBuffer.size
            val delayed = ghostReverbBuffer[readIdx]
            val mixed = combined + delayed * feedback
            ghostReverbBuffer[ghostReverbIndex] = mixed
            ghostReverbIndex = (ghostReverbIndex + 1) % ghostReverbBuffer.size

            output[i] = mixed.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return output
    }

    // =============================================
    // Darth Vader — pitch down + subtle ring mod + chest resonance + helmet reverb
    // =============================================

    private fun applyDarthVader(buffer: ShortArray, size: Int): ShortArray {
        // Pitch shift down
        val pitched = applyPitchShift(buffer, size, 0.75f, vaderPitchAccum)

        val output = ShortArray(size)
        val ringFreq = 30.0  // Very low ring mod
        val ringPhaseInc = 2.0 * Math.PI * ringFreq / sampleRate
        val helmDelaySamples = (sampleRate * 0.05f).toInt()  // 50ms helmet reflection
        val feedback = 0.2f

        // Band emphasis filter state (100-200Hz resonance)
        val dt = 1.0f / sampleRate
        val rcHp = 1.0f / (2.0f * Math.PI.toFloat() * 100f)
        val alphaHp = rcHp / (rcHp + dt)
        val rcLp = 1.0f / (2.0f * Math.PI.toFloat() * 200f)
        val alphaLp = dt / (rcLp + dt)
        var hpPrev = 0f
        var hpOut = 0f
        var lpOut = 0f

        for (i in 0 until size) {
            val sample = pitched[i].toFloat()

            // Subtle ring modulation (20% mix)
            val ringMod = sin(vaderRingPhase).toFloat()
            val modulated = sample * (0.8f + 0.2f * ringMod)
            vaderRingPhase += ringPhaseInc
            if (vaderRingPhase > 2.0 * Math.PI) vaderRingPhase -= 2.0 * Math.PI

            // Extract chest resonance (100-200Hz band)
            hpOut = alphaHp * (hpOut + modulated - hpPrev)
            hpPrev = modulated
            lpOut += alphaLp * (hpOut - lpOut)

            // Boost resonance and add back
            val resonanceBoost = modulated + lpOut * 0.4f

            // Short helmet reverb
            val readIdx = (vaderReverbIndex - helmDelaySamples + vaderReverbBuffer.size) % vaderReverbBuffer.size
            val delayed = vaderReverbBuffer[readIdx]
            val mixed = resonanceBoost + delayed * feedback
            vaderReverbBuffer[vaderReverbIndex] = mixed
            vaderReverbIndex = (vaderReverbIndex + 1) % vaderReverbBuffer.size

            output[i] = mixed.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return output
    }

    // =============================================
    // Old Man — slight pitch down + tremolo + thin EQ
    // =============================================

    private fun applyOldMan(buffer: ShortArray, size: Int): ShortArray {
        // Pitch shift down slightly
        val pitched = applyPitchShift(buffer, size, 0.85f, oldManPitchAccum)

        val output = ShortArray(size)
        val tremoloRate = 5.5  // Hz — vocal tremor
        val tremoloPhaseInc = 2.0 * Math.PI * tremoloRate / sampleRate
        val tremoloDepth = 0.15f

        // Thin voice: cut bass below 150Hz (high-pass)
        val dt = 1.0f / sampleRate
        val rcHp = 1.0f / (2.0f * Math.PI.toFloat() * 150f)
        val alphaHp = rcHp / (rcHp + dt)
        var hpPrev = 0f
        var hpOut = 0f

        for (i in 0 until size) {
            val sample = pitched[i].toFloat()

            // High-pass to thin the voice
            hpOut = alphaHp * (hpOut + sample - hpPrev)
            hpPrev = sample

            // Tremolo
            val tremoloMod = 1.0f - tremoloDepth + tremoloDepth * sin(oldManTremoloPhase).toFloat()
            oldManTremoloPhase += tremoloPhaseInc
            if (oldManTremoloPhase > 2.0 * Math.PI) oldManTremoloPhase -= 2.0 * Math.PI

            val tremoloed = hpOut * tremoloMod

            // Add very slight crackle
            val crackle = if (Math.random() < 0.002) (Math.random().toFloat() - 0.5f) * 2000f else 0f

            output[i] = (tremoloed + crackle).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return output
    }

    // =============================================
    // Astronaut/Space — radio filter + flanger + long reverb + static
    // =============================================

    private fun applyAstronaut(buffer: ShortArray, size: Int): ShortArray {
        val output = ShortArray(size)
        val dt = 1.0f / sampleRate

        // Band-pass 500-5000Hz
        val rcHp = 1.0f / (2.0f * Math.PI.toFloat() * 500f)
        val alphaHp = rcHp / (rcHp + dt)
        val rcLp = 1.0f / (2.0f * Math.PI.toFloat() * 5000f)
        val alphaLp = dt / (rcLp + dt)
        var hpPrev = 0f
        var hpOut = 0f
        var lpOut = 0f

        // Flanger params
        val flangerRate = 0.1  // Hz — very slow
        val flangerPhaseInc = 2.0 * Math.PI * flangerRate / sampleRate
        val flangerBaseDelay = 88  // ~2ms
        val flangerDepth = 44     // ~1ms modulation

        // Long reverb
        val reverbDelaySamples = (sampleRate * 0.6f).toInt()  // 600ms
        val reverbFeedback = 0.45f

        for (i in 0 until size) {
            val sample = buffer[i].toFloat()

            // Band-pass
            hpOut = alphaHp * (hpOut + sample - hpPrev)
            hpPrev = sample
            lpOut += alphaLp * (hpOut - lpOut)

            // Write to flanger buffer
            astronautFlangerBuffer[astronautFlangerIndex] = lpOut

            // Read from flanger with modulated delay
            val mod = (sin(astronautFlangerPhase) * flangerDepth).toInt()
            val flangerReadIdx = (astronautFlangerIndex - flangerBaseDelay - mod + astronautFlangerBuffer.size) % astronautFlangerBuffer.size
            val flanged = (lpOut + astronautFlangerBuffer[flangerReadIdx]) * 0.5f

            astronautFlangerIndex = (astronautFlangerIndex + 1) % astronautFlangerBuffer.size
            astronautFlangerPhase += flangerPhaseInc
            if (astronautFlangerPhase > 2.0 * Math.PI) astronautFlangerPhase -= 2.0 * Math.PI

            // Intermittent static bursts
            val static = if (Math.random() < 0.005) (Math.random().toFloat() - 0.5f) * 3000f else 0f

            val withStatic = flanged + static

            // Long reverb
            val readIdx = (astronautReverbIndex - reverbDelaySamples + astronautReverbBuffer.size) % astronautReverbBuffer.size
            val delayed = astronautReverbBuffer[readIdx]
            val mixed = withStatic + delayed * reverbFeedback
            astronautReverbBuffer[astronautReverbIndex] = mixed
            astronautReverbIndex = (astronautReverbIndex + 1) % astronautReverbBuffer.size

            output[i] = mixed.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return output
    }

    // =============================================
    // 8-Bit/Retro Game — sample rate reduction + bit crush
    // =============================================

    private fun applyEightBit(buffer: ShortArray, size: Int): ShortArray {
        val output = ShortArray(size)
        val holdLength = 6  // Hold each sample for 6 frames (~7.35kHz effective)
        val bitLevels = 8   // Quantize to 8 levels (3-bit)
        val step = (65536 / bitLevels)  // Step size for quantization

        for (i in 0 until size) {
            eightBitHoldCounter++
            if (eightBitHoldCounter >= holdLength) {
                eightBitHoldCounter = 0
                // Quantize to fewer bit levels
                val sample = buffer[i].toInt() + 32768  // Shift to unsigned
                val quantized = (sample / step) * step - 32768  // Quantize and shift back
                eightBitHoldSample = quantized.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }
            output[i] = eightBitHoldSample
        }
        return output
    }

    // =============================================
    // Stadium/Concert — pre-delay + very long Schroeder reverb + early reflections
    // =============================================

    private fun applyStadium(buffer: ShortArray, size: Int): ShortArray {
        val output = ShortArray(size)
        val preDelaySamples = (sampleRate * 0.08f).toInt()  // 80ms pre-delay
        val lateFeedback = 0.92f  // Very long decay
        val wetMix = 0.5f
        val dryMix = 0.5f

        for (i in 0 until size) {
            val input = buffer[i].toFloat()

            // Early reflections (sum of 3 taps)
            var earlySum = 0f
            for (e in 0 until 3) {
                val buf = stadiumEarlyBuffers[e]
                val delay = stadiumEarlyDelays[e]
                val readIdx = (stadiumEarlyIndices[e] - delay + buf.size) % buf.size
                earlySum += buf[readIdx] * 0.3f
                buf[stadiumEarlyIndices[e]] = input
                stadiumEarlyIndices[e] = (stadiumEarlyIndices[e] + 1) % buf.size
            }

            // Late reverb with pre-delay
            val lateInput = input + earlySum * 0.5f
            val lateReadIdx = (stadiumLateIndex - preDelaySamples - (sampleRate * 0.5f).toInt() + stadiumLateBuffer.size) % stadiumLateBuffer.size
            val lateDelayed = stadiumLateBuffer[lateReadIdx]
            val lateMixed = lateInput + lateDelayed * lateFeedback

            // Damping (slight low-pass on feedback to simulate air absorption)
            val damped = lateMixed * 0.97f
            stadiumLateBuffer[stadiumLateIndex] = damped
            stadiumLateIndex = (stadiumLateIndex + 1) % stadiumLateBuffer.size

            // Final mix
            val mixed = input * dryMix + (earlySum + lateDelayed * 0.3f) * wetMix

            output[i] = mixed.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
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
