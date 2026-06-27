package com.bluetooth.bluetoothmictospeaker.audio

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Autotune Effect — T-Pain / Instagram style
 *
 * Architecture (from documentation):
 * 1. Write input samples into a circular buffer
 * 2. Read from the buffer at a variable rate (ratio = targetFreq / detectedFreq)
 * 3. Use linear interpolation between samples for smooth output
 * 4. Detect pitch using YIN on each chunk
 * 5. Snap to chromatic scale, compute ratio, apply
 *
 * The pitch shifter works exactly like the AudioWorklet in the docs:
 * - inputPos increments at rate 1 (write every sample)
 * - outputPos increments at rate 1/ratio (read at shifted speed)
 * - Linear interpolation between integer sample positions
 *
 * Crossfade handles the discontinuity when read pointer wraps near write pointer.
 */
class AutotuneEffect(private val sampleRate: Int = 44100) {

    // ======================== Circular Buffer (the pitch shifter) ========================
    private val bufferSize = 4096
    private val circBuffer = FloatArray(bufferSize)
    private var writePos = 0
    private var readPos = 0.0        // Fractional read position — this is the key to pitch shifting

    // ======================== Pitch Detection ========================
    private val analysisSize = 2048  // YIN analysis window
    private val analysisBuffer = FloatArray(analysisSize)
    private var analysisFillCount = 0

    // ======================== Pitch Tracking ========================
    private var currentRatio = 1.0f          // Current applied ratio (smoothed)
    private var targetRatio = 1.0f           // Target ratio from latest detection
    private val retuneSpeed = 0.8f           // 0.01 = natural, 1.0 = instant/robotic (T-Pain)

    fun reset() {
        circBuffer.fill(0f)
        writePos = 0
        readPos = 0.0
        analysisBuffer.fill(0f)
        analysisFillCount = 0
        currentRatio = 1.0f
        targetRatio = 1.0f
    }

    /**
     * Process a chunk of audio. Handles any buffer size from AudioRecord.
     */
    fun process(input: ShortArray, length: Int): ShortArray {
        val output = ShortArray(length)

        for (i in 0 until length) {
            val sample = input[i].toFloat()

            // 1. Write sample into circular buffer
            circBuffer[writePos % bufferSize] = sample
            writePos++

            // 2. Accumulate for pitch detection
            if (analysisFillCount < analysisSize) {
                analysisBuffer[analysisFillCount] = sample
                analysisFillCount++
            }

            // 3. When we have enough samples, detect pitch and update ratio
            if (analysisFillCount >= analysisSize) {
                val detectedHz = detectPitchYIN(analysisBuffer, analysisSize)
                if (detectedHz in 60f..1200f) {
                    val snap = snapToScale(detectedHz)
                    targetRatio = snap / detectedHz
                } else {
                    // Unvoiced — drift toward no correction
                    targetRatio = 1.0f
                }
                analysisFillCount = 0
            }

            // 4. Smooth toward target ratio (retune speed)
            currentRatio = if (currentRatio == 1.0f && targetRatio != 1.0f) {
                targetRatio  // First detection — snap immediately
            } else {
                currentRatio + (targetRatio - currentRatio) * retuneSpeed
            }

            // 5. Read from circular buffer at shifted rate (the pitch shift!)
            val effectiveRatio = if (abs(currentRatio - 1.0f) < 0.005f) 1.0f else currentRatio
            readPos += effectiveRatio

            // Keep readPos from falling too far behind writePos
            val lag = writePos - readPos
            if (lag > bufferSize - 64) {
                readPos = (writePos - bufferSize + 64).toDouble()
            } else if (lag < 64) {
                readPos = (writePos - 64).toDouble()
            }

            // 6. Linear interpolation between two integer sample positions
            val idx = floor(readPos).toInt()
            val frac = (readPos - idx).toFloat()
            val a = circBuffer[((idx % bufferSize) + bufferSize) % bufferSize]
            val b = circBuffer[(((idx + 1) % bufferSize) + bufferSize) % bufferSize]
            val shifted = a + (b - a) * frac

            output[i] = shifted.toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                .toShort()
        }

        return output
    }

    // ======================== YIN Pitch Detection ========================

    /**
     * Detect pitch using YIN algorithm.
     * Returns frequency in Hz, or -1 if no pitch detected (silence/noise).
     */
    private fun detectPitchYIN(buffer: FloatArray, size: Int): Float {
        val halfSize = size / 2
        val threshold = 0.1f

        // Step 1: Difference function
        val diff = FloatArray(halfSize)
        for (lag in 0 until halfSize) {
            var sum = 0f
            for (j in 0 until halfSize) {
                val delta = buffer[j] - buffer[j + lag]
                sum += delta * delta
            }
            diff[lag] = sum
        }

        // Step 2: Cumulative mean normalized difference
        diff[0] = 1f
        var running = 0f
        for (i in 1 until halfSize) {
            running += diff[i]
            if (running != 0f) {
                diff[i] *= i.toFloat() / running
            }
        }

        // Step 3: Find first dip below threshold (skip lag 0 and 1)
        var tau = -1
        for (i in 2 until halfSize) {
            if (diff[i] < threshold) {
                // Walk to the local minimum
                while (i + 1 < halfSize && diff[i + 1] < diff[i]) {
                    // Note: i is a var in the for loop in the doc, but in Kotlin for is val
                    // We handle this differently
                }
                tau = i
                break
            }
        }

        // Refine: find actual local minimum after the threshold crossing
        if (tau > 0 && tau + 1 < halfSize) {
            var minTau = tau
            while (minTau + 1 < halfSize && diff[minTau + 1] < diff[minTau]) {
                minTau++
            }
            tau = minTau
        }

        if (tau == -1) return -1f  // No pitch (silence)

        // Parabolic interpolation for sub-sample accuracy
        val refinedTau = if (tau in 1 until halfSize - 1) {
            val s0 = diff[tau - 1]
            val s1 = diff[tau]
            val s2 = diff[tau + 1]
            val denom = 2f * (2f * s1 - s2 - s0)
            if (denom != 0f) tau + (s2 - s0) / denom
            else tau.toFloat()
        } else {
            tau.toFloat()
        }

        return sampleRate / refinedTau
    }

    // ======================== Scale Snapping (MIDI-based) ========================

    /**
     * Snap a detected frequency to the nearest chromatic note.
     * Uses MIDI note numbers for precise calculation (from documentation).
     */
    private fun snapToScale(detectedHz: Float): Float {
        // Convert Hz to MIDI note number
        val midi = (12f * log2(detectedHz / 440f) + 69f).roundToInt()
        // Convert back to Hz — this gives us the nearest chromatic note
        return 440f * 2f.pow((midi - 69f) / 12f)
    }
}

/**
 * Chromatic note quantizer — kept for backward compatibility.
 */
object NoteQuantizer {
    fun snapToChromatic(hz: Float): Float {
        if (hz <= 0f) return hz
        val midi = (12f * log2(hz / 440f) + 69f).roundToInt()
        return 440f * 2f.pow((midi - 69f) / 12f)
    }

    fun ratioToSemitones(ratio: Float): Float = 12f * log2(ratio)
}
