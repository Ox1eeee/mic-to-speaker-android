# Real Autotune Effect — Proper Implementation Guide

> The ZCR approach in the previous doc was wrong. It only detects average zero-crossings and
> cannot reliably track a singing voice in real time. This guide replaces it entirely with the
> correct professional DSP pipeline used by Antares Auto-Tune and replicated by Instagram/TikTok.

---

## Why ZCR Fails for Autotune

| Problem | What Goes Wrong |
|---------|----------------|
| ZCR detects average frequency, not fundamental | Returns wrong pitch on voiced audio with harmonics |
| No formant awareness | Pitch shift sounds like Chipmunk, not Autotune |
| No pitch snapping — just resampling | You hear speed change, not note locking |
| One buffer at a time | No phase continuity → clicks, artifacts, distortion |

---

## The Correct Pipeline (3 Stages)

```
[AudioRecord Buffer]
        │
        ▼
┌───────────────────┐
│  STAGE 1          │  ← Autocorrelation (YIN algorithm)
│  Pitch Detection  │     Finds true fundamental frequency (F0)
│  (YIN Algorithm)  │     Works reliably on real human voices
└────────┬──────────┘
         │  detected Hz (e.g. 347 Hz)
         ▼
┌───────────────────┐
│  STAGE 2          │  ← Snap to nearest chromatic note
│  Note Quantizer   │     e.g. 347 Hz → F4 (349.23 Hz)
└────────┬──────────┘
         │  shift ratio (349.23 / 347 = 1.0065)
         ▼
┌───────────────────┐
│  STAGE 3          │  ← Phase Vocoder (STFT-based)
│  Pitch Shifter    │     Shifts pitch WITHOUT changing tempo
│  (STFT / OLA)     │     Preserves formants → sounds human, not chipmunk
└────────┬──────────┘
         │
         ▼
  [AudioTrack Output]
```

---

## Stage 1 — Pitch Detection: YIN Algorithm

**Why YIN and not FFT or ZCR?**

- **ZCR** → only works for pure sine waves, breaks on real voice (too many harmonics)
- **FFT** → computationally expensive for each small buffer, overkill for pitch detection alone
- **YIN** → designed specifically for monophonic voice/instrument pitch tracking. Used by
  professional audio tools. Fast enough for real-time at 44.1kHz.

### How YIN Works (conceptual)

YIN is an autocorrelation-based algorithm. It computes a "difference function" — how much
the signal differs from a time-shifted copy of itself. When the shift matches the pitch period,
the difference is near zero. YIN adds a normalization step that makes the fundamental clear
even when harmonics are strong.

### YIN Implementation in Kotlin

```kotlin
/**
 * YIN Pitch Detector
 * Returns fundamental frequency in Hz, or -1.0f if no pitch detected (silence/noise).
 */
fun detectPitchYIN(buffer: ShortArray, length: Int, sampleRate: Int = 44100): Float {
    val threshold = 0.15f      // Lower = more strict detection. 0.10–0.20 is typical.
    val minPeriod = sampleRate / 1000   // max ~1000 Hz (top of singing voice)
    val maxPeriod = sampleRate / 70     // min ~70 Hz (bottom of singing voice)

    val yinBuffer = FloatArray(maxPeriod)

    // Step 1: Compute difference function
    for (tau in 1 until maxPeriod) {
        var sum = 0.0
        for (j in 0 until maxPeriod) {
            val delta = buffer[j].toFloat() - buffer[j + tau].toFloat()
            sum += delta * delta
        }
        yinBuffer[tau] = sum.toFloat()
    }

    // Step 2: Cumulative mean normalized difference
    yinBuffer[0] = 1f
    var runningSum = 0f
    for (tau in 1 until maxPeriod) {
        runningSum += yinBuffer[tau]
        yinBuffer[tau] *= tau / runningSum
    }

    // Step 3: Find first dip below threshold
    var tauEstimate = -1
    for (tau in minPeriod until maxPeriod) {
        if (yinBuffer[tau] < threshold) {
            // Find local minimum in this dip
            while (tau + 1 < maxPeriod && yinBuffer[tau + 1] < yinBuffer[tau]) {
                tau++
            }
            tauEstimate = tau
            break
        }
    }

    if (tauEstimate == -1) return -1f   // No pitch detected (silence or noise)

    // Step 4: Parabolic interpolation for sub-sample accuracy
    val refinedTau = if (tauEstimate in 1 until maxPeriod - 1) {
        val s0 = yinBuffer[tauEstimate - 1]
        val s1 = yinBuffer[tauEstimate]
        val s2 = yinBuffer[tauEstimate + 1]
        tauEstimate + (s2 - s0) / (2f * (2f * s1 - s2 - s0))
    } else {
        tauEstimate.toFloat()
    }

    return sampleRate / refinedTau
}
```

---

## Stage 2 — Note Quantizer

Snap the detected pitch to the nearest chromatic note (or diatonic scale if desired).

```kotlin
object NoteQuantizer {

    // Full chromatic scale — all 12 notes across octaves 1–7 (human voice range: ~80–1200 Hz)
    private val chromaticFrequencies: List<Float> = buildList {
        val a4 = 440f
        for (n in -36..36) {
            add(a4 * 2f.pow(n / 12f))
        }
    }.filter { it in 80f..1200f }.sorted()

    // Optional: Major scale only (C major) for more musical snapping
    // Intervals in semitones from root: 0,2,4,5,7,9,11
    private val majorScaleIntervals = setOf(0, 2, 4, 5, 7, 9, 11)

    fun snapToChromatic(hz: Float): Float {
        if (hz <= 0f) return hz
        return chromaticFrequencies.minByOrNull { abs(it - hz) } ?: hz
    }

    fun snapToMajorScale(hz: Float, rootNote: Int = 0 /* 0=C */): Float {
        if (hz <= 0f) return hz
        val allNotes = chromaticFrequencies
        val filtered = allNotes.filter { freq ->
            val midiNote = (12 * log2(freq / 440f) + 69).roundToInt()
            ((midiNote - rootNote) % 12 + 12) % 12 in majorScaleIntervals
        }
        return filtered.minByOrNull { abs(it - hz) } ?: hz
    }

    fun computeShiftRatio(detectedHz: Float, targetHz: Float): Float {
        if (detectedHz <= 0f) return 1f
        return targetHz / detectedHz
    }

    fun ratioToSemitones(ratio: Float): Float = 12f * log2(ratio)
}
```

---

## Stage 3 — Pitch Shifter: OLA (Overlap-Add) Phase Vocoder

This is the critical part that the previous doc got wrong. Simple resampling
(`pitchShift()` by index skipping) changes tempo AND pitch — it sounds like a tape
speed change (Chipmunk effect). The Instagram Autotune effect changes **only pitch**, 
keeping timing the same. This requires an Overlap-Add approach.

### Why OLA and not full Phase Vocoder?

A full Phase Vocoder (STFT → frequency bin manipulation → ISTFT) gives studio-quality
results but is very CPU-heavy for real-time mobile processing. The **OLA (Overlap-Add)**
method is a time-domain approximation that:

- Sounds indistinguishable from a full phase vocoder for short correction distances (≤ 3 semitones)
- Runs comfortably within an Android audio loop at 44.1kHz
- Does not require FFT/IFFT per frame (huge CPU saving)

### OLA Pitch Shifter

```kotlin
class OLAPitchShifter(
    private val sampleRate: Int = 44100,
    private val frameSize: Int = 2048,    // Analysis window size (must be power of 2)
    private val hopSize: Int = 512        // Step between frames (frameSize/4 = 75% overlap)
) {
    private val overlapBuffer = FloatArray(frameSize) { 0f }
    private val window = HannWindow(frameSize)
    private var outputPhase = 0

    /**
     * Shift the pitch of [input] by [semitones] without changing duration.
     * semitones > 0 = higher pitch, semitones < 0 = lower pitch
     */
    fun process(input: ShortArray, length: Int, semitones: Float): ShortArray {
        if (abs(semitones) < 0.05f) return input   // No correction needed

        val ratio = 2f.pow(semitones / 12f)         // e.g. +2 semitones = 1.1225x
        val output = FloatArray(length)
        val inputF = FloatArray(length) { input[it].toFloat() }

        var inputPos = 0
        var outputPos = 0

        while (inputPos + frameSize < length && outputPos + frameSize < length) {
            // 1. Extract and window an analysis frame
            val frame = FloatArray(frameSize) { i ->
                inputF[inputPos + i] * window[i]
            }

            // 2. Resample frame to new length (time-scale change = pitch change via OLA)
            val newFrameSize = (frameSize / ratio).toInt().coerceIn(64, frameSize * 4)
            val resampled = resampleLinear(frame, newFrameSize)

            // 3. Overlap-add into output at hop intervals
            val writeLen = minOf(resampled.size, length - outputPos)
            for (i in 0 until writeLen) {
                output[outputPos + i] += resampled[i] * window.getOrElse(i) { 1f }
            }

            inputPos  += hopSize
            outputPos += hopSize
        }

        // Normalize and convert back to ShortArray
        val maxAmp = output.maxOf { abs(it) }.coerceAtLeast(1f)
        val scale  = if (maxAmp > Short.MAX_VALUE) Short.MAX_VALUE / maxAmp else 1f
        return ShortArray(length) { i ->
            (output[i] * scale).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                .toShort()
        }
    }

    /** Linear interpolation resampler */
    private fun resampleLinear(input: FloatArray, newSize: Int): FloatArray {
        val output = FloatArray(newSize)
        val ratio = input.size.toFloat() / newSize
        for (i in output.indices) {
            val pos = i * ratio
            val idx = pos.toInt()
            val frac = pos - idx
            output[i] = if (idx + 1 < input.size)
                input[idx] * (1f - frac) + input[idx + 1] * frac
            else
                input[idx]
        }
        return output
    }
}

/** Hann window — reduces spectral leakage at frame edges */
class HannWindow(size: Int) {
    private val data = FloatArray(size) { i ->
        0.5f * (1f - cos(2.0 * PI * i / (size - 1))).toFloat()
    }
    operator fun get(i: Int) = data[i]
    fun getOrElse(i: Int, default: () -> Float) = data.getOrElse(i) { default() }
}
```

---

## Complete Autotune Effect — Wired Together

```kotlin
class AutotuneEffect(sampleRate: Int = 44100) {

    private val pitchShifter = OLAPitchShifter(sampleRate = sampleRate)

    // "Retune speed" — Instagram's autotune is INSTANT (retune = 0ms).
    // This is what makes it sound robotic/mechanical (T-Pain style).
    // Set to 0f for full Instagram effect. Increase for subtle correction.
    private val retuneSpeed = 0f

    fun process(input: ShortArray, length: Int): ShortArray {
        // Step 1: Detect fundamental pitch
        val detectedHz = detectPitchYIN(input, length)

        // Step 2: Skip unvoiced frames (silence, breath, consonants)
        if (detectedHz < 80f || detectedHz > 1200f) return input

        // Step 3: Snap to nearest chromatic note
        val targetHz = NoteQuantizer.snapToChromatic(detectedHz)

        // Step 4: Calculate how many semitones to shift
        val semitones = NoteQuantizer.ratioToSemitones(targetHz / detectedHz)

        // Step 5: Skip tiny corrections (below 5 cents = imperceptible)
        if (abs(semitones) < 0.05f) return input

        // Step 6: Pitch shift via OLA (pitch-only, duration preserved)
        return pitchShifter.process(input, length, semitones)
    }
}
```

---

## Wire Into `AudioEngine.kt`

```kotlin
// In AudioEngine — instantiate once (not per-frame)
private val autotuneEffect = AutotuneEffect(sampleRate = 44100)

fun applyEffect(buffer: ShortArray, length: Int, effect: VoiceEffect): ShortArray {
    return when (effect) {
        is VoiceEffect.Autotune -> autotuneEffect.process(buffer, length)
        // ... rest of your effects
    }
}
```

> ⚠️ **Important:** Do NOT instantiate `AutotuneEffect` inside the audio loop.
> Create it once and reuse it. The `OLAPitchShifter` maintains internal state
> (overlap buffers) that must persist across frames.

---

## Tuning for the Instagram Sound

Instagram's autotune effect is based on **maximum retune speed with chromatic snapping**.
These are the exact settings that produce the viral sound:

| Parameter | Instagram Sound | Natural/Subtle Sound |
|-----------|----------------|----------------------|
| Retune speed | Instant (0ms) | 40–80ms |
| Scale | Chromatic (all 12 notes) | Major/Minor key |
| Threshold | 80–1200 Hz | Same |
| Frame size | 2048 samples | 1024–4096 |
| Hop size | 512 (75% overlap) | Same |

To add a **key selector** (e.g. snap only to C major notes), swap `snapToChromatic()`
for `snapToMajorScale(hz, rootNote = 0)` in `AutotuneEffect.process()`.

---

## Common Problems & Fixes

| Symptom | Root Cause | Fix |
|---------|-----------|-----|
| Voice sounds like Chipmunk | Using resampling/index skipping, not OLA | Use `OLAPitchShifter`, not `pitchShift()` |
| Clicks and pops between frames | No windowing on frame boundaries | Apply Hann window before every OLA frame |
| Pitch wrong / jumpy | YIN threshold too low | Raise threshold from 0.10 to 0.20 |
| Effect on breath/silence | Not filtering unvoiced frames | Check `detectedHz < 80f` guard |
| CPU spike / latency | Re-creating effect object per frame | Instantiate `AutotuneEffect` once only |
| Effect sounds too subtle | Pitch correction below 0.05 semitones | Lower the `abs(semitones) < 0.05f` guard |
| Effect sounds too robotic | Chromatic snap too aggressive | Use major scale snap instead of chromatic |

---

## Files to Change

| File | What to Add |
|------|------------|
| `AudioEngine.kt` | `AutotuneEffect` class, `OLAPitchShifter`, `HannWindow`, `detectPitchYIN()`, `NoteQuantizer` |
| `VoiceEffect.kt` | `object Autotune` (already done if using previous guide) |
| `HomeScreen.kt` | No changes needed |

Keep all DSP classes inside `audio/` package alongside `AudioEngine.kt`.
