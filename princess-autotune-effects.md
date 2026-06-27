# Princess & Autotune Voice Effects — Implementation Guide

> Extends `android-mic-speaker-plan.md` — adds 2 new viral effects to the existing DSP pipeline.

---

## Overview

| Effect | What It Does | Instagram Vibe |
|--------|-------------|----------------|
| **Princess** | High pitch shift + soft shimmer reverb + gentle sparkle chorus | Fairy/cute feminine voice |
| **Autotune** | Chromatic pitch quantization snapping voice to nearest musical note | T-Pain / viral singing effect |

Both plug directly into your existing `applyEffect()` dispatcher and `VoiceEffect` sealed class.

---

## Step 1 — Add to `VoiceEffect.kt`

```kotlin
sealed class VoiceEffect(val label: String, val emoji: String) {
    // ... your existing effects ...
    object Princess  : VoiceEffect("Princess", "👸")
    object Autotune  : VoiceEffect("Autotune", "🎵")
}
```

---

## Step 2 — Princess Effect

**DSP Chain:** `Pitch Shift (+600¢)` → `Chorus/Shimmer` → `Light Reverb`

### How It Works
- Shifts pitch up by **+6 semitones** (~600 cents) — higher than Chipmunk but softer
- Adds a **chorus** (slight detuned copy mixed in) for a "sparkle" shimmer
- Applies a **short bright reverb** for an airy, dreamy tail

### Implementation in `AudioEngine.kt`

```kotlin
fun applyPrincessEffect(input: ShortArray, length: Int): ShortArray {
    // Step 1: Pitch shift up +6 semitones
    val pitched = pitchShift(input, length, semitones = 6f)

    // Step 2: Chorus — mix original pitched with slightly detuned copy (+15 cents)
    val chorusCopy = pitchShift(pitched, pitched.size, semitones = 0.15f)
    val chorused = ShortArray(pitched.size) { i ->
        ((pitched[i] * 0.75f) + (chorusCopy[i] * 0.35f)).toInt().toShort()
    }

    // Step 3: Light shimmer reverb (short decay ~200ms, bright tone)
    return applyReverb(chorused, decayFactor = 0.25f, delayMs = 80)
}
```

### `pitchShift()` helper (OLA-based, reuse from existing Chipmunk/DeepVoice):

```kotlin
fun pitchShift(input: ShortArray, length: Int, semitones: Float): ShortArray {
    val ratio = 2f.pow(semitones / 12f)   // e.g. +6 semitones = 1.4983x
    val output = ShortArray(length)
    for (i in output.indices) {
        val srcIndex = (i / ratio).toInt()
        output[i] = if (srcIndex < length) input[srcIndex] else 0
    }
    return output
}
```

### `applyReverb()` helper (simple feedback delay):

```kotlin
fun applyReverb(input: ShortArray, decayFactor: Float, delayMs: Int): ShortArray {
    val delaySamples = (sampleRate * delayMs / 1000f).toInt()
    val output = input.copyOf()
    for (i in delaySamples until output.size) {
        output[i] = (output[i] + output[i - delaySamples] * decayFactor)
            .toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
    }
    return output
}
```

---

## Step 3 — Autotune Effect

**DSP Chain:** `Fundamental Frequency Detection (ZCR)` → `Snap to Nearest Note` → `Pitch Correct`

### How It Works
1. **Detect pitch** of incoming audio using Zero-Crossing Rate (ZCR) — fast, low-latency
2. **Snap** the detected pitch to the nearest chromatic note frequency
3. **Re-pitch** the buffer so it plays back at the snapped frequency

### Chromatic Note Table

```kotlin
// 12 notes per octave, A4 = 440Hz reference
val noteFrequencies = (0..8).flatMap { octave ->
    listOf(
        130.81f, 138.59f, 146.83f, 155.56f, 164.81f, 174.61f,
        185.0f, 196.0f, 207.65f, 220.0f, 233.08f, 246.94f
    ).map { it * (1 shl octave) / 2 }  // scale across octaves
}.filter { it in 80f..1200f }  // keep human voice range only
```

### Pitch Detection via ZCR

```kotlin
fun detectPitchZCR(buffer: ShortArray, length: Int): Float {
    var crossings = 0
    for (i in 1 until length) {
        if ((buffer[i - 1] < 0) != (buffer[i] < 0)) crossings++
    }
    val durationSec = length.toFloat() / sampleRate
    return crossings / (2f * durationSec)   // returns Hz
}
```

### Snap to Nearest Note

```kotlin
fun snapToNearestNote(detectedHz: Float): Float {
    if (detectedHz < 80f) return detectedHz  // silence / unvoiced, skip
    return noteFrequencies.minByOrNull { abs(it - detectedHz) } ?: detectedHz
}
```

### Full Autotune Apply Function

```kotlin
fun applyAutotuneEffect(input: ShortArray, length: Int): ShortArray {
    val detectedHz = detectPitchZCR(input, length)
    val targetHz   = snapToNearestNote(detectedHz)

    if (detectedHz < 80f) return input  // unvoiced frame — pass through

    val ratio = targetHz / detectedHz
    val semitones = 12f * log2(ratio)   // convert ratio to semitones
    return pitchShift(input, length, semitones)
}
```

---

## Step 4 — Wire into `applyEffect()` Dispatcher

```kotlin
fun applyEffect(buffer: ShortArray, length: Int, effect: VoiceEffect): ShortArray {
    return when (effect) {
        is VoiceEffect.None      -> buffer
        is VoiceEffect.Robot     -> applyRobotEffect(buffer, length)
        is VoiceEffect.Chipmunk  -> pitchShift(buffer, length, semitones = 8f)
        is VoiceEffect.DeepVoice -> pitchShift(buffer, length, semitones = -4f)
        is VoiceEffect.Echo      -> applyReverb(buffer, decayFactor = 0.6f, delayMs = 250)
        is VoiceEffect.Alien     -> applyAlienEffect(buffer, length)
        is VoiceEffect.Radio     -> applyBandpassFilter(buffer, length, 300f, 3400f)
        // ✅ NEW
        is VoiceEffect.Princess  -> applyPrincessEffect(buffer, length)
        is VoiceEffect.Autotune  -> applyAutotuneEffect(buffer, length)
    }
}
```

---

## Step 5 — Add to Home Screen Chip List

```kotlin
// In HomeScreen.kt — your effect chip list
val effects = listOf(
    VoiceEffect.None,
    VoiceEffect.Robot,
    VoiceEffect.Chipmunk,
    VoiceEffect.DeepVoice,
    VoiceEffect.Echo,
    VoiceEffect.Alien,
    VoiceEffect.Radio,
    VoiceEffect.Princess,   // 👸 NEW
    VoiceEffect.Autotune    // 🎵 NEW
)
```

---

## Step 6 — Monetization Gating (Optional)

Both effects are premium — gate them with your existing subscription check:

```kotlin
val isPremium = subscriptionViewModel.isSubscribed.collectAsState()

EffectChip(
    effect = VoiceEffect.Princess,
    locked = !isPremium.value,
    onClick = { if (isPremium.value) selectEffect(it) else showPaywall() }
)
```

---

## Quality Tips

| Concern | Fix |
|---------|-----|
| Autotune sounds robotic | Process only voiced frames (ZCR > 80Hz), pass unvoiced through |
| Princess pitch sounds harsh | Blend 80% pitched + 20% dry for softness |
| Latency spike on Autotune | Cap pitch correction ratio to ±4 semitones per frame |
| Chorus artifacts | Keep chorus mix wet ≤ 35% |

---

## Files Changed Summary

| File | Change |
|------|--------|
| `VoiceEffect.kt` | Add `Princess` and `Autotune` objects |
| `AudioEngine.kt` | Add `applyPrincessEffect()`, `applyAutotuneEffect()`, helper functions |
| `HomeScreen.kt` | Add 2 new chips to the effects list |
| `PaywallScreen.kt` | Optionally highlight as "New & Trending" effects |
