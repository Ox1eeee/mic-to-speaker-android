# Voice Effects — Market Research & Implementation Plan

---

## PLAN 1: Trending Voice Effects in the Market (2024-2025)

### Current App Status
Your app currently has **8 effects**: None, Robot, Chipmunk, Deep Voice, Echo, Alien, Radio, Princess, Autotune

### Top Competitors & Their Effects

| App | Downloads | Effects Count |
|-----|-----------|---------------|
| Voice Changer with Effects (Baviux) | **160M+** | 50+ |
| Vocalis AI Voice Changer | 1M+ | 49+ |
| VocalFX | Growing | 50+ |
| Voloco | 10M+ | 20+ (music-focused) |
| Video Voice Changer | 10M+ | 60+ |

---

### TIER 1: Must-Have Effects (Highest User Demand)

These are in **every** top voice changer app and users expect them:

| # | Effect | Why It's Trending | Difficulty |
|---|--------|-------------------|------------|
| 1 | **Helium** | #1 most searched fun effect, viral on TikTok | Easy (pitch up + formant) |
| 2 | **Monster/Demon** | Horror content, Halloween, gaming | Easy (pitch down + distortion) |
| 3 | **Telephone** | Retro/nostalgic, content creation | Easy (band-pass filter) |
| 4 | **Cave/Cathedral Reverb** | Cinematic feel, singing practice | Medium (long reverb) |
| 5 | **Underwater** | Unique sound, very popular in memes | Easy (low-pass + wobble) |
| 6 | **Megaphone** | Protest/rally aesthetic, gaming | Easy (distortion + band-pass) |
| 7 | **Baby Voice** | Fun/cute content, pranks | Easy (pitch up + slight formant) |
| 8 | **Ghost/Whisper** | Horror content, ASMR | Medium (reverb + pitch layers) |
| 9 | **Walkie-Talkie** | Military/spy aesthetic, gaming | Easy (band-pass + noise) |
| 10 | **Darth Vader** | Iconic, always in demand | Medium (pitch down + resonance) |

---

### TIER 2: Trending & Differentiating Effects

These make apps stand out and drive downloads:

| # | Effect | Why It's Trending | Difficulty |
|---|--------|-------------------|------------|
| 11 | **Vocoder/Synth Voice** | Music production, EDM content | Hard |
| 12 | **Choir/Harmony** | Singing apps, spiritual content | Hard (multi-voice) |
| 13 | **Drunk** | Comedy content, viral pranks | Easy (wobble + slight pitch) |
| 14 | **Old Man/Grandpa** | Comedy, aging filter trend | Medium (pitch + tremolo) |
| 15 | **Astronaut/Space** | Sci-fi content, space aesthetic | Medium (radio + reverb + flanger) |
| 16 | **8-Bit/Retro Game** | Gaming content, nostalgia trend | Medium (bitcrusher + quantize) |
| 17 | **Zombie** | Horror, gaming content | Easy (pitch down + distortion + reverb) |
| 18 | **Stadium/Concert** | Sports content, hype videos | Medium (large reverb + crowd) |
| 19 | **Cyborg** | Sci-fi, futuristic aesthetic | Medium (vocoder + robot mix) |
| 20 | **T-Rex/Giant** | Fun content, kids love it | Easy (very low pitch + reverb) |

---

### TIER 3: AI-Powered & Next-Gen (2025 Trend)

These are the cutting-edge features driving new downloads:

| # | Effect | Why It's Trending | Difficulty |
|---|--------|-------------------|------------|
| 21 | **Gender Swap (Male↔Female)** | #1 trending AI effect, massive demand | Hard (formant + pitch) |
| 22 | **Celebrity Voice Style** | Viral on TikTok/Reels, massive engagement | Very Hard (AI/ML) |
| 23 | **Singing Voice (Music Mode)** | Turn speech into song, viral content | Very Hard |
| 24 | **Noise Reduction/Clean** | Utility, podcasters, streamers | Medium |
| 25 | **Custom Effect Builder** | Power users, uniqueness | Medium (UI work) |

---

### What Users Want Most (Based on Reviews & Trends)

1. **Fun/Entertainment**: Helium, Monster, Baby, Drunk, Backwards
2. **Content Creation**: Autotune, Reverb, Echo, Radio, Megaphone
3. **Gaming/Streaming**: Robot, Alien, Darth Vader, Demon, Cyborg
4. **Horror/Pranks**: Ghost, Zombie, Monster, Demon, Whisper
5. **Music/Singing**: Autotune, Harmony, Reverb, Cathedral, Vocoder
6. **Environments**: Cave, Underwater, Stadium, Space, Telephone

---

## PLAN 2: Implementation Guide for Each New Effect

### Priority Order (implement in this sequence for maximum impact)

---

### Phase 1: Quick Wins (1-2 days each, Easy DSP)

#### 1. Helium Voice
```
Technique: Pitch shift UP (1.8x) + slight formant preservation
DSP Chain: Pitch shift factor 1.8 with linear interpolation
Similar to: Chipmunk but less extreme (1.8x vs 2.0x)
```

#### 2. Monster/Demon
```
Technique: Pitch shift DOWN (0.5x) + soft distortion + short reverb
DSP Chain: 
  1. Pitch shift at 0.5 factor
  2. Soft clip distortion: out = tanh(input * 2.0)
  3. Short reverb (decay 0.3s)
```

#### 3. Telephone
```
Technique: Band-pass filter (300Hz - 3400Hz) + slight distortion
DSP Chain:
  1. High-pass filter at 300Hz (1st order IIR)
  2. Low-pass filter at 3400Hz (1st order IIR)  
  3. Soft saturation: out = input * 1.5, then clip
  4. Reduce bit depth slightly (multiply by 0.8)
```

#### 4. Underwater
```
Technique: Strong low-pass filter + slow LFO modulating cutoff (wobble)
DSP Chain:
  1. Low-pass filter at 500Hz (2nd order)
  2. LFO at 0.5Hz modulates filter cutoff ±200Hz
  3. Slight chorus effect (5ms delay, LFO modulated)
```

#### 5. Megaphone
```
Technique: Band-pass (500Hz-4000Hz) + hard distortion + slight feedback
DSP Chain:
  1. High-pass at 500Hz
  2. Low-pass at 4000Hz
  3. Hard clip distortion: clamp(input * 3.0, -0.8, 0.8)
  4. Boost mids (+6dB at 2kHz)
```

#### 6. Baby Voice
```
Technique: Pitch shift UP (1.5x) — gentler than Chipmunk
DSP Chain: Pitch shift factor 1.5 with linear interpolation
Note: Reuse existing pitch shift code with factor 1.5
```

#### 7. Walkie-Talkie
```
Technique: Band-pass (1000Hz-3000Hz) + noise + slight distortion
DSP Chain:
  1. High-pass at 1000Hz
  2. Low-pass at 3000Hz
  3. Add white noise at -20dB
  4. Soft clip distortion
  5. Reduce sample rate slightly (downsample 2x, upsample 2x)
```

#### 8. Drunk
```
Technique: Slow pitch wobble (LFO on pitch) + slight slur (variable speed)
DSP Chain:
  1. LFO at 0.3Hz modulates pitch shift (0.95 - 1.05)
  2. Slight reverb (short room)
  3. Random micro-stutters every 500ms
```

#### 9. Zombie
```
Technique: Pitch down (0.7x) + growl distortion + reverb
DSP Chain:
  1. Pitch shift at 0.7
  2. Distortion with asymmetric clipping
  3. Medium reverb (0.5s decay)
  4. Low-pass at 3000Hz
```

#### 10. T-Rex/Giant
```
Technique: Very low pitch (0.4x) + heavy reverb + sub-bass boost
DSP Chain:
  1. Pitch shift at 0.4
  2. Boost frequencies below 200Hz
  3. Long reverb (1s decay)
```

---

### Phase 2: Medium Complexity (2-3 days each)

#### 11. Cave/Cathedral Reverb
```
Technique: Long convolution-style reverb using multiple delay lines
DSP Chain:
  1. Schroeder reverb with 4 comb filters + 2 all-pass filters
  2. Comb delays: 1116, 1188, 1277, 1356 samples (at 44100Hz)
  3. All-pass delays: 225, 556 samples
  4. Feedback: 0.84 (cave) or 0.92 (cathedral)
  5. Wet/dry mix: 0.6
Implementation:
  - Use parallel comb filters → sum → series allpass filters
  - Each comb: output[n] = input[n] + feedback * buffer[readPos]
  - Each allpass: output[n] = -g*input[n] + buffer[readPos] + g*output[n]
```

#### 12. Ghost/Whisper
```
Technique: Breathy noise layer + pitch up octave (faint) + long reverb
DSP Chain:
  1. Original signal reduced to 30% volume
  2. Pitched-up copy (+12 semitones) at 20% volume
  3. White noise shaped by input envelope at 15% volume
  4. Long reverb (2s decay) on everything
  5. Slight chorus for ethereal feel
```

#### 13. Darth Vader
```
Technique: Pitch down + resonant breathing + ring modulation
DSP Chain:
  1. Pitch shift at 0.75
  2. Ring modulation at 30Hz (very subtle, 20% mix)
  3. Band-pass emphasis at 100-200Hz (chest resonance)
  4. Short reverb (helmet reflection ~50ms)
  5. Slight compression (normalize peaks)
```

#### 14. Old Man/Grandpa
```
Technique: Slight pitch down + tremolo (shaky voice) + thin EQ
DSP Chain:
  1. Pitch shift at 0.85
  2. Tremolo at 5-6Hz, depth 15% (vocal tremor)
  3. Cut bass below 150Hz (thin voice)
  4. Boost 2-3kHz slightly (nasality)
  5. Add very slight crackle noise
```

#### 15. Astronaut/Space
```
Technique: Radio filter + flanger + long reverb + slight static
DSP Chain:
  1. Band-pass (500Hz - 5000Hz) — radio quality
  2. Flanger: 2ms delay, LFO 0.1Hz, depth 1ms
  3. Long reverb (1.5s decay)
  4. Intermittent white noise bursts (radio static)
  5. Slight compression
```

#### 16. 8-Bit/Retro Game
```
Technique: Bitcrusher + sample rate reduction
DSP Chain:
  1. Reduce sample rate: hold each sample for 4-8 frames (effective 5.5-11kHz)
  2. Reduce bit depth: quantize to 8 levels (3-bit)
     Formula: out = floor(input / step) * step
  3. Optional: square wave synth tracking pitch
```

#### 17. Stadium/Concert
```
Technique: Very long reverb + pre-delay + slight chorus
DSP Chain:
  1. Pre-delay: 80ms (sound travels in large space)
  2. Very long Schroeder reverb (3s decay, feedback 0.95)
  3. Early reflections at 20ms, 45ms, 67ms
  4. Slight pitch modulation on reverb tail
  5. Wet/dry: 0.5
```

---

### Phase 3: Advanced Effects (3-5 days each)

#### 18. Gender Swap (Male → Female)
```
Technique: Pitch shift + formant shift (independent)
Challenge: Need to shift pitch WITHOUT shifting formants
DSP Chain:
  1. Detect pitch (reuse YIN from Autotune)
  2. For Male→Female: shift pitch UP 4-6 semitones
  3. Shift formants DOWN to compensate (keep vocal character)
  4. For Female→Male: opposite direction
Implementation Note:
  - Simple version: just pitch shift +5 semitones (reuse existing code)
  - Advanced version: PSOLA with independent formant control
```

#### 19. Vocoder
```
Technique: Use voice as modulator for synth carrier
DSP Chain:
  1. Split input into 8-16 frequency bands (bank of band-pass filters)
  2. Extract envelope of each band (rectify + low-pass at 50Hz)
  3. Generate carrier (sawtooth wave at detected pitch)
  4. Split carrier into same bands
  5. Multiply each carrier band by corresponding envelope
  6. Sum all bands
Simplified version:
  - Ring modulation at detected pitch frequency
  - Apply to filtered signal
```

#### 20. Choir/Harmony
```
Technique: Multiple pitch-shifted copies at harmonic intervals
DSP Chain:
  1. Detect pitch (use YIN)
  2. Create copy shifted +4 semitones (major 3rd)
  3. Create copy shifted +7 semitones (perfect 5th)
  4. Mix: original 50% + 3rd 30% + 5th 30%
  5. Apply slight detuning to each (±5 cents) for natural feel
  6. Slight reverb on the mix
  7. Each voice gets slight random delay (5-15ms) for width
```

---

### Implementation Architecture

```
VoiceEffect.kt — Add new sealed class entries
EffectProcessor.kt — Add applyXxx() methods for each effect
```

### Recommended Implementation Order (Maximum ROI)

| Week | Effects to Add | User Impact |
|------|---------------|-------------|
| Week 1 | Helium, Monster, Telephone, Baby | +4 fun effects, covers top demand |
| Week 2 | Underwater, Megaphone, Walkie-Talkie, Drunk | +4 unique effects |
| Week 3 | Cave Reverb, Ghost, Darth Vader | +3 cinematic effects |
| Week 4 | 8-Bit, Zombie, Giant, Old Man | +4 character effects |
| Week 5 | Astronaut, Stadium, Gender Swap | +3 advanced effects |
| Week 6 | Vocoder, Choir/Harmony | +2 music effects |

### Monetization Strategy

| Tier | Effects | Pricing |
|------|---------|---------|
| **Free** | None, Robot, Autotune, Helium, Echo | Hook users |
| **Pro** | All others (15+ effects) | Drive subscriptions |

---

### Technical Notes

- **All effects use the same pattern**: receive `ShortArray` buffer + size, return processed `ShortArray`
- **Reuse existing infrastructure**: `applyPitchShift()`, echo buffer, crossfade logic
- **Keep CPU low**: Simple IIR filters, avoid FFT where possible
- **State persistence**: All effects that use buffers/LFOs must maintain state across calls (like autotune fix)
- **Buffer size**: Audio engine provides ~3528 samples per call at 44100Hz mono

---

*Total new effects: 20*  
*Current effects: 8*  
*After implementation: 28 effects*  
*Competitive with top apps that have 30-50 effects*
