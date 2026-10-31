# LiveMic Onboarding: Android (Kotlin) Implementation Instructions

These instructions describe the **3 onboarding screens used in the iOS app** so the Android app (Kotlin) can match them screen by screen. The values come straight from the iOS source:

- `Bluetooth Mic Speaker/OnboardingView.swift`: screen layout, behaviour, button style, background bars
- `Bluetooth Mic Speaker/OnboardingAnimations.swift`: all animated components
- `Bluetooth Mic Speaker/AudioPreviewManager.swift`: tap sounds on Screen 1

The code samples use **Jetpack Compose**. If the Android onboarding uses XML Views, the specs are the same: draw with a custom `View` + `Canvas`, and drive time with `Choreographer` / `ValueAnimator`.

---

## 0. Global rules (apply to all 3 screens)

### 0.1 Flow
| Page | Screen | Continue button goes to |
|---|---|---|
| 0 | Screen 1: LiveMic hero (tap mic to hear voices) | Page 1 |
| 1 | Screen 2: Connect to Any Speaker | Page 2 |
| 2 | Screen 3: Karaoke Night | Finish onboarding |

- **No swiping** between pages and **no page dots**. Only the Continue button moves forward.
- Only the current page is composed. When a page leaves, all its timers, coroutines and sounds stop.
- **Finish onboarding** = save `hasSeenOnboarding = true` (DataStore / SharedPreferences) and open the main screen.
  - **No** permission requests during onboarding. The mic permission is asked the first time the user taps the mic on the main screen.
  - **No** rating / review prompt during onboarding.
- Android back button (iOS has none): recommended behaviour is page 2 → page 1 → page 0 → leave the app. Keep `pageIndex` in `rememberSaveable` so rotation or process death doesn't reset it.

### 0.2 Units, insets
- 1 iOS pt = **1 dp**; font sizes = **sp**.
- iOS draws edge-to-edge and measures top/bottom spacing from the screen edge. On Android, go edge-to-edge and use:
  - Top: `Modifier.statusBarsPadding()` + the gap from the spec minus ~24 dp (so content never collides with the status bar).
  - Bottom: `Modifier.navigationBarsPadding()` + 16 dp under the Continue button (iOS uses 24 from the screen edge).
- Must fit on small phones (≈ 360×640 dp) **without scrolling**. Gaps marked "flex (min X)" absorb the difference.

### 0.3 Flexible gaps
In the iOS layout most vertical gaps are flexible: each has a minimum and the leftover height is shared **equally** between all flexible gaps on that screen. In Compose:

```kotlin
@Composable
fun ColumnScope.FlexGap(min: Dp) {
    Spacer(Modifier.height(min))          // guaranteed minimum
    Spacer(Modifier.weight(1f))           // equal share of leftover space
}
```

### 0.4 Colours
Shared background for all 3 pages: **vertical gradient** `#2A1A4A` (top) → `#161022` (bottom), full screen.

| Token | Hex | Notes |
|---|---|---|
| `NeonCyan` | `#00F2FF` | brand gradient start |
| `NeonPurple` | `#8A2BE3` | brand gradient end |
| `Accent` | `#5B13EC` | button shadow |
| `SysCyan` | `#64D2FF` | iOS system cyan (dark) |
| `SysPurple` | `#BF5AF2` | iOS system purple (dark) |
| `SysYellow` | `#FFD60A` | |
| `SysOrange` | `#FF9F0A` | |
| `SysGreen` | `#30D158` | |
| `SysTeal` | `#40C8E0` | |
| `SysIndigo` | `#5E5CE6` | |
| `SysRed` | `#FF453A` | Robot glitch layer |
| `Robot` | `#33FF99` | |
| `Demon` | `#FF3340` | |
| `DemonDeep` | `#730099` | Demon secondary |
| `DemonText2` | `#66004D` | Demon "HELLO" gradient bottom |
| `Echo` | `#598CFF` | |
| `Helium` | `#FF73CC` | |
| `Ghost` | `#CCD9FF` | |
| `StageTop` | `#120829` | |
| Greys | `#0D0D0D` `#121212` `#141414` `#1A1A1A` `#2E2E2E` `#333333` `#4D4D4D` `#595959` | speaker / phone drawing |

"White 35%" means `Color.White.copy(alpha = 0.35f)`, and likewise for any other "colour XX%".

**"Material" backgrounds.** iOS `.ultraThinMaterial` is a frosted glass effect. On Android use `Color.White.copy(alpha = 0.10f)`. If you want the real frosted look, add a backdrop blur on API 31+; it isn't required.

### 0.5 Typography
| iOS | Android |
|---|---|
| headline (17) bold | 17 sp, `FontWeight.Bold` |
| subheadline (15) | 15 sp |
| footnote (13) semibold | 13 sp, `SemiBold` |
| caption (12) | 12 sp |
| caption2 (11) semibold | 11 sp, `SemiBold` |
| weight heavy / black | `W800` / `W900` |
| design `.rounded` | a rounded font (e.g. Nunito); fallback `FontFamily.Default` |
| design `.monospaced` | `FontFamily.Monospace` |
| design `.serif` | `FontFamily.Serif` |

Gradient text: `TextStyle(brush = Brush.horizontalGradient(...))`.

### 0.6 Icons (iOS SF Symbol → Android)
| iOS | Android (Material Icons Extended) |
|---|---|
| `mic.fill` | `Icons.Filled.Mic` |
| `hifispeaker.fill` | `Icons.Filled.Speaker` |
| `antenna.radiowaves.left.and.right` | `Icons.Filled.CellTower` |
| `checkmark.circle.fill` | `Icons.Filled.CheckCircle` |
| `arrow.right` | `Icons.AutoMirrored.Filled.ArrowForward` |
| `sparkles` | `Icons.Filled.AutoAwesome` |
| `bolt.fill` | `Icons.Filled.Bolt` |
| `star.fill` / `star.leadinghalf.filled` | `Icons.Filled.Star` / `Icons.AutoMirrored.Filled.StarHalf` |
| `wave.3.right` | **draw 3 arcs in a Canvas** (see Screen 2) |

Emoji render in Android's emoji style (Noto), not Apple's. That's expected. Use `androidx.emoji2` so they render consistently on older devices.

### 0.7 Animation helpers

**Continuous clock.** iOS uses a frame-by-frame timeline. On Android, use a frame-synced seconds counter:

```kotlin
@Composable
fun rememberSeconds(paused: Boolean = false): State<Float> = produceState(0f, paused) {
    if (paused) return@produceState
    val start = withFrameNanos { it } - (value * 1e9f).toLong()
    while (true) withFrameNanos { value = (it - start) / 1_000_000_000f }
}
```
Every formula below uses `t` = seconds from this clock. `frac(x)` = `x - floor(x)`.

**Springs.** iOS `spring(response: R, dampingFraction: D)` → Compose `spring(dampingRatio = D, stiffness = (2π/R)²)`:

| iOS | Compose `spring(...)` |
|---|---|
| response 0.8, damping 0.75 | `dampingRatio = 0.75f, stiffness = 61.7f` |
| response 0.5, damping 0.7 | `0.7f, 157.9f` |
| response 0.5 (default damping 0.825) | `0.825f, 157.9f` |
| response 0.45, damping 0.7 | `0.7f, 195f` |
| response 0.45 (default 0.825) | `0.825f, 195f` |
| response 0.4, damping 0.7 | `0.7f, 246.7f` |
| response 0.35, damping 0.65 | `0.65f, 322f` |

**Easing.** `easeInOut` = `CubicBezierEasing(0.42f, 0f, 0.58f, 1f)`; `easeOut` = `CubicBezierEasing(0f, 0f, 0.58f, 1f)`.

**Glows.** "Glow colour X, radius R" (iOS coloured shadow) → `Modifier.shadow(elevation = R.dp, shape, ambientColor = X, spotColor = X)` on API 28+, or draw a radial-gradient halo behind the element. **Blur** (`Modifier.blur`) only works on API 31+; on older devices skip it.

**Haptics** (`LocalView.current.performHapticFeedback(...)`):
| iOS | Android |
|---|---|
| light impact (Screen 1 mic tap) | `HapticFeedbackConstants.KEYBOARD_TAP` |
| selection (Screen 3 effect change) | `HapticFeedbackConstants.CLOCK_TICK` (`SEGMENT_TICK` on API 34+) |
| success (Screen 2 connected) | `HapticFeedbackConstants.CONFIRM` (API 30+), else `LONG_PRESS` |

**Reduce Motion.** If the user has turned on "Remove animations" (`Settings.Global.ANIMATOR_DURATION_SCALE == 0f`), freeze every continuous clock (`paused = true`, `t = 0`):
- Sound rings, floating notes and the shimmer are hidden.
- Speakers don't beat, and karaoke lyric lines are shown fully highlighted.
- Entrance and state-change springs still run.

### 0.8 Primary "Continue" button (identical on all 3 screens)
- Height **56**, full width with **24** horizontal margin, pill shape (`CircleShape`/`RoundedCornerShape(50)`).
- Fill: horizontal gradient `NeonCyan → NeonPurple`. Shadow: `Accent` 35%, blur 18, y-offset 10.
- Label "**Continue**", 17 sp **Bold**, white.
- Pressed: scale 0.98, alpha 0.9.
- **Screen 1 only:** shimmer overlay (see 1.11).

---

## 1. Screen 1: LiveMic hero ("tap the mic")

**Goal:** show the value in the first 3 seconds. The mic is surrounded by orbiting effect emojis, and a speech bubble says "Hello" in a different voice. **Tapping the mic plays that voice's sound.**

### 1.1 Layout (top → bottom)
Behind everything: the **Visualizer Bars Background** (1.2) at alpha **0.6**, full bleed.

1. Flex gap (min 52 from top edge; see 0.2).
2. **Header**, Column, spacing **6**, horizontal padding **24**, centered:
   - "**LiveMic**": 22 sp, W800, rounded font, horizontal gradient `NeonCyan → NeonPurple`.
   - "**Your Phone is now a Mic**": 28 sp Bold white, **1 line**, auto-shrink to min 75% (Compose `BasicText(autoSize = …)` or measure and step down). *(iOS says "iPhone"; use "Phone" on Android.)*
   - "**30 voice effects, live on any Bluetooth speaker.**": 15 sp, white 75%, centered.
   - **Social proof row** (top padding 4): see 1.10.
3. Flex gap (min 8).
4. **Hero box**: full width, height **250**. Contains (layered, centred): Orbit (1.3), Mic button (1.4, offset y **+22**), Particle burst (1.5, offset y +22), Speech bubble (1.6, offset y **−58**).
5. **Hint text**: top padding 4, 13 sp SemiBold, white 80%. Before the first tap: "**Tap the mic to hear it 🔊**"; after: "**Tap again for another voice**". Crossfade on change.
6. **Voice transform row** (1.8): horizontal padding **40**, top padding **12**.
7. Flex gap (min 12).
8. **Value chips** row (1.9), spacing **8**, centred.
9. Flex gap (min 16).
10. **Continue** button (0.8) with shimmer (1.11); bottom spacing 24 (see 0.2).

### 1.2 Visualizer Bars Background
- **40** vertical capsules, width **4**, spacing **6**, centred horizontally and vertically. Total width 394 dp; wider screens show it centred, narrower screens clip it.
- Fill: vertical gradient, bottom `#00F2FF` 30% → top `SysPurple` 30%. Optional 0.5 blur. Whole layer alpha **0.7**, then multiplied by the screen's **0.6**.
- **Animation:** each bar `i` has two random heights, `a ∈ [40,160]` and `b ∈ [60,180]`. It animates `a → b → a` forever (reverse repeat) with **its own** random duration ∈ [0.8 s, 1.5 s], easeInOut, starting after a delay of `i × 0.05 s`. Use an `InfiniteTransition` per bar, or one clock with per-bar phase.

### 1.3 Orbit of effect emojis
- Fills the hero box (W × 250). Centre `c`; radius `r = min(W, 250)/2 − 22` (≈ 103).
- **Ring:** circle of radius `r`, stroke **1**, dash pattern **[3, 6]**, vertical gradient `SysCyan` 35% → `SysPurple` 35%.
- **12 emojis, in order:** `🤖 🦹 🐿️ 👽 😈 🎵 🏛️ 🚀 🎈 🦕 👻 📻`
- Position of emoji `i`: `angle = i/12 · 2π + t · 0.25` (radians, increasing clockwise on screen), `x = c.x + cos(angle)·r`, `y = c.y + sin(angle)·r`. The ring rotates continuously.
- Each emoji chip: **38** circle, emoji 22 sp.
  - **Inactive:** fill white 6%, border white 12% (1), scale **0.9**.
  - **Active** (emoji == current voice's emoji): fill voiceColor 30%, border voiceColor (2), scale **1.3**, glow voiceColor 80% r10.
  - Active state changes animate with the voice-change spring (1.12).
- Not touchable.
- **Entrance** (on first show, spring 0.8/0.75): scale 0.3 → 1, rotation −120° → 0°, alpha 0 → 1.

### 1.4 Mic button (centre, y +22)
Layers (bottom → top):
1. **Glow:** 150 circle, radial gradient `[voiceColor 45%, SysPurple 25%, transparent]`, from radius 30 to 75, blur 10. Pulses with scale `1 + 0.12·pulse`, where `pulse = (sin(2.2·t) + 1)/2`.
2. **Core:** 96 circle. Fill: linear gradient from top-left to bottom-right, `[white 18%, SysPurple 30%]`. Border 2, linear gradient TL→BR `[voiceColor, SysPurple]`. Glow voiceColor 60% r18.
3. **Mic icon:** 40 sp, white.
4. **Voice badge:** 36 circle, fill black 65%, border voiceColor 1.5, current voice emoji 20 sp. Offset **(+36, +34)** from the mic centre. When the voice changes, the old badge leaves and the new one enters with **scale + fade**.

Behaviour:
- Clickable with **no ripple**. Accessibility label: "Tap to transform your voice".
- **Entrance:** scale 0.4 → 1, alpha 0 → 1 (spring 0.8/0.75).

### 1.5 Particle burst (each tap)
- **14** dots, size **7**. Even index = voiceColor, odd = white.
- Dot `i` flies from the mic centre to radius **85** at angle `i/14 · 2π`. At the same time it scales 1 → 0.4 and fades 1 → 0.
- **0.7 s, easeOut.** Each tap restarts a fresh burst (key it on the tap count). Not touchable.

### 1.6 Speech bubble (centre, y −58)
- **Size:** height **60** including an **8** tall tail; min width **130**; padding 18 horizontal, 8 bottom (keeps the text above the tail).
- **Shape:** rounded rectangle, corner **16**, height `60 − 8`. Tail = triangle at the bottom centre, 16 wide (±8), 8 tall, pointing down at the mic.
- **Fill:** voiceColor 12%, then "material" (0.4) on top. **Border:** voiceColor 85%, 1.5. **Glow:** voiceColor 50% r10.
- **Content:** Styled Hello (1.7).
- **Voice-change transition:** new bubble enters with scale **0.6 → 1, anchored at the bottom centre** + fade-in; the old one leaves the same way, reversed.
- Not touchable. Entrance: alpha 0 → 1.

### 1.7 Styled "Hello" per voice
All text uses the voice colour unless stated. `t` = clock seconds.

| Voice | Text | Style & motion |
|---|---|---|
| **Robot** `#33FF99` | `HELLO.` | 24 sp Bold **Monospace**. Three stacked copies: `SysCyan` 70% offset x `−g`, `SysRed` 60% offset x `+g`, and the main colour on top. `g = 2.5` when `sin(23t) > 0.85`, else `0` (glitch flicker). |
| **Chipmunk** `SysYellow` | `hello!!` | Each letter separate, 22 sp W800 rounded. Letter `i` offset y = `sin(12t + 0.8i) · 4`, a bouncy wave. |
| **Demon** `#FF3340` | `HELLO` | 30 sp W900. Vertical gradient `#FF3340 → #66004D`. Shake: offset x `sin(37t)·1.5`, y `cos(29t)·1`. Glow `#FF3340` 80% r6. |
| **Echo** `#598CFF` | `Hello hello hello` | Row aligned to the text baseline, spacing 4: "Hello" 22 sp Bold 100%; "hello" 17 sp Bold 55%; "hello" 13 sp Bold 25%. No motion. |
| **Helium** `#FF73CC` | `hello~` | Each letter 22 sp SemiBold rounded. Letter `i`: offset y = `−|sin(3t + 0.6i)| · 7` (floats **up**), rotation = `sin(2t + i) · 8°`. |
| **Ghost** `#CCD9FF` | `hellooo…` | 22 sp SemiBold **Serif Italic**. `w = (sin(2.5t)+1)/2`; alpha `0.55 + 0.45w`; blur `0.3 + 0.7w` (API 31+); offset y `sin(1.5t)·3`. |

### 1.8 Voice transform row ("before → after")
A Row, spacing **12**, with three parts:
1. Column (spacing 4, `weight(1f)`): **Plain wave**, height 24; label "**Your voice**" 11 sp SemiBold white 60%.
2. Arrow icon: 14 sp Bold, white 70%.
3. Column (spacing 4, `weight(1f)`): **Effect waveform**, height 24, using the voice's wave style and **tinted** with the voice colour (1.13); label "`<emoji> <name>`", e.g. "🤖 Robot", 11 sp SemiBold voiceColor, crossfade on change.

**Plain wave** (Canvas):
- 20 bars, gap 3, `barW = (W − 3·19)/20`.
- For bar `i`: `x = i/19`, `level = 0.25 + 0.25·|sin(7x + 2t)|`, `h = max(3, H·level)`.
- Vertically centred, rounded ends (radius `barW/2`), white 35%.

### 1.9 Value chips
Three chips, left to right:
- `AutoAwesome` + "30 Effects"
- `Speaker` + "Any Speaker"
- `Bolt` + "Zero Setup"

Each chip: Row (spacing 5) with the icon (12 sp, Bold, `SysCyan`) and the title (12 sp, SemiBold, white, 1 line). Padding 10 horizontal / 7 vertical, pill shape, "material" fill, border white 15% (1).

Entrance: alpha 0 → 1.

### 1.10 Social proof row
- Row, spacing **6**:
  1. Stars: 4 × `Star` + 1 × `StarHalf`, 11 sp, `SysYellow`, spacing 1
  2. "**4.7**", 12 sp Bold white
  3. "**· Loved by 22K+ users**", 12 sp white 75%
- Accessibility: one element, "Rated 4.7 stars, loved by over 22 thousand users".
- **Android:** replace with the **real Google Play rating and user count**. Don't copy the iOS numbers unless they're also true on Play.

### 1.11 Continue button shimmer
- A light band moves across the button, clipped to the pill, and doesn't intercept taps.
- Band: width **35%** of the button, horizontal gradient `[transparent, white 45%, transparent]`.
- Cycle **3 s**: `phase = frac(t/3)`, `x = −0.35·W + min(1, phase/0.4) · 1.35·W`. So the band sweeps for 1.2 s, then waits off the right edge for 1.8 s.
- The button fades in with the entrance (alpha 0 → 1). The fade is short, so the button is usable straight away.

### 1.12 Behaviour & timing
- **Voices, in order (loops):** Robot → Chipmunk → Demon → Echo → Helium → Ghost → Robot. Starts on **Robot**.

| Voice | Colour | Emoji | Waveform style (1.13) | Sound key (1.14) |
|---|---|---|---|---|
| Robot | `#33FF99` | 🤖 | robot | "Robot" |
| Chipmunk | `SysYellow` | 🐿️ | chipmunk | "Chipmunk" |
| Demon | `#FF3340` | 😈 | demon | "Demon" |
| Echo | `#598CFF` | 🏛️ | echo | "Echo Chamber" |
| Helium | `#FF73CC` | 🎈 | chipmunk | "Helium" |
| Ghost | `#CCD9FF` | 👻 | echo | "Ghost" |

- **Entrance** (first show): `appeared = true` with spring **0.8 / 0.75**.
  - Header: alpha 0 → 1 and translationY 16 → 0.
  - Orbit, mic, bubble, chips and button animate as described in their sections.
- **Auto-cycle (silent):** wait **0.9 s**, then every **1.6 s**: if more than **3 s** have passed since the last tap, go to the next voice (spring **0.4 / 0.7**). No sound and no haptic.
- **Tap on the mic:**
  1. Remember the tap time; increment the tap count.
  2. Switch to the next voice (spring **0.35 / 0.65**).
  3. Light haptic (0.7).
  4. **Play the new voice's sound** (1.14).
  5. Particle burst (1.5).
- **Leaving the screen:** stop any sound playing and cancel the auto-cycle coroutine.

```kotlin
LaunchedEffect(Unit) {
    delay(900)
    while (isActive) {
        delay(1600)
        if (System.currentTimeMillis() - lastTapMs > 3000) voice = voice.next()
    }
}
```

### 1.13 Effect waveform (shared by Screen 1 and Screen 3)
Drawn on a Canvas:
- **32 bars**, gap **3**, `barW = (W − 3·31)/32`.
- For bar `i`:
  - `x = i/31`
  - `envelope = 0.35 + 0.65·sin(π·x)` (bars are taller in the middle)
  - `h = max(4, H · level(x,t) · envelope)`
- Each bar is vertically centred with rounded ends (radius `barW/2`).
- Bar fill: vertical gradient over the bar, `[tint ?: primary, secondary]`.

`level(x, t)` per style (all values 0…1):
```kotlin
fun level(style: Style, x: Double, t: Double): Double = when (style) {
    AUTOTUNE -> { val snap = floor(t * 4) / 4                  // pitch "snaps" 4×/s
                  val v = 0.5 + 0.5 * sin(x * 12 + snap * 5)
                  0.25 + 0.75 * v * (0.7 + 0.3 * sin(t * 6)) }
    ROBOT    -> { val tq = floor(t * 6) / 6                     // stepped / quantized
                  val v = abs(sin(x * 9 + tq * 4)) * 0.7 + abs(sin(x * 23 - tq * 3)) * 0.3
                  round(v * 4) / 4 * 0.85 + 0.15 }
    CHIPMUNK -> { val v = abs(sin(x * 30 + t * 18)) * 0.6 + abs(sin(x * 57 - t * 25)) * 0.4
                  0.15 + 0.7 * v }                              // fast & jittery
    DEMON    -> 0.3 + 0.7 * abs(sin(x * 4 + t * 2.2))           // slow & heavy
    ECHO     -> { val p = (t * 0.9) % 1.6                       // pulse + fading repeats
                  val v = exp(-abs(x - p) * 7) + 0.55 * exp(-abs(x - p + 0.3) * 7) +
                          0.3 * exp(-abs(x - p + 0.6) * 7)
                  0.12 + 0.85 * min(1.0, v) }
}
```

Primary and secondary colours per style:

| Style | Primary | Secondary |
|---|---|---|
| autotune | `SysCyan` | `SysPurple` |
| robot | `#33FF99` | `SysTeal` |
| chipmunk | `SysYellow` | `SysOrange` |
| demon | `#FF3340` | `#730099` |
| echo | `#598CFF` | `SysIndigo` |

On Screen 1 the primary is replaced by the voice colour (e.g. Helium is pink).

### 1.14 Tap sounds (synthesized, 1.5 s each)
The iOS app **generates** these tones in code; there are no audio files. Android generates the same PCM.

**Format:** 44 100 Hz, mono, 16-bit. `N = 66 150` samples.

**Formula:** `time = i / 44100f`, `f = 200 Hz` (base frequency). For each sample `i`, in this order:

| Key | `sample[i]` |
|---|---|
| "Robot" | `if (sin(time·f·2π) > 0) 0.3 else −0.3` (square wave) |
| "Chipmunk" | `sin(time·(2.5f)·2π) · 0.4` |
| "Demon" | `df = 0.25f`: `sin(time·df·2π)·0.6 + sin(time·df·3π)·0.25 + sin(time·df·5π)·0.15`. **Note: 3π and 5π, not 2π multiples.** Copy them exactly. |
| "Echo Chamber" | `v = sin(time·f·2π)·0.4`; `j = i − (0.15·44100)`; `sample = v + (if j ≥ 0: sample[j]·0.5 else 0)`. Uses the **already-faded** earlier sample. |
| "Helium" | `sin(time·(3.0f)·2π) · 0.35` |
| "Ghost" | `mod = sin(time·2·2π)·0.3 + 0.7`; `sin(time·(1.3f)·2π) · 0.35 · mod` |

**Fade (envelope), applied to each sample right after computing it:**
- 50 ms (2 205 samples) at each end.
- If `i < 2205`: `sample *= i/2205`.
- Else if `i > N − 2205`: `sample *= (N − i)/2205`.

**Output:** clamp to [−1, 1], then convert to `Short` (× 32767).

Playback rules (to match iOS):
- Starting a new sound **stops** the previous one.
- Play on the **media** stream (`AudioAttributes.USAGE_MEDIA`).
- **Don't request audio focus**, so the sound mixes with any other app's audio instead of pausing it.
- Generate once per key and cache it.

```kotlin
val track = AudioTrack.Builder()
    .setAudioAttributes(AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
    .setAudioFormat(AudioFormat.Builder()
        .setSampleRate(44100).setEncoding(AudioFormat.ENCODING_PCM_16BIT)
        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
    .setTransferMode(AudioTrack.MODE_STATIC)
    .setBufferSizeInBytes(pcm.size * 2)
    .build()
track.write(pcm, 0, pcm.size); track.play()   // release() the previous track first
```

---

## 2. Screen 2: Connect to Any Speaker

**Goal:** a phone (as the mic) pairs with a Bluetooth speaker **once**, then the speaker plays "live" on a loop. No bars background on this screen; only the shared gradient.

### 2.1 Layout
1. Flex gap (min 56).
2. **Header**, Column, spacing **10**, horizontal padding **24**, centred:
   - "**Connect to Any Speaker**": 30 sp Bold white, centred, shrink to 80% if needed.
   - "**Pair a Bluetooth speaker and your phone becomes a wireless mic.**": 15 sp white 75%, centred. *(iOS says "iPhone".)*
3. Flex gap (min 24).
4. **Connection scene** (2.2): height **230**, horizontal padding **24**.
5. Flex gap (min 24).
6. **Status pill** (2.9), centred.
7. Flex gap (min 8).
8. **Continue** button (0.8).

### 2.2 Connection scene
A Row (spacing **10**, vertically centred) with three parts:
1. **Phone** (2.3), fixed 72×136, with the **signal arcs** (2.4) overlaid on its right edge.
2. **Beam** (2.5): takes the remaining width, height **6**.
3. **Speaker stack**, 120 wide, all centred on top of each other:
   - **Sound rings** (2.6), 120×120
   - **Floating notes** (2.7), 120×200
   - **Speaker** (2.8), width 96

`live = (phase == CONNECTED)`.

### 2.3 Phone
- Body: 72×136, rounded corners **16**. Fill: vertical gradient `#2E2E2E → #121212`. Border **1.5**, gradient TL→BR `SysCyan 70% → SysPurple 70%`.
- Inside, a Column (spacing 10, centred):
  1. Notch: black capsule 22×6, top padding 8
  2. Flexible space
  3. Mic icon: 26 sp, vertical gradient `SysCyan → SysPurple`
  4. **Level bars**: 40×22
  5. Flexible space
- Glow `SysCyan`: alpha **0.45** when live, **0.15** before, radius 14.
- **Level bars:** 5 capsules, spacing 3, equal width, vertically centred, vertical gradient `SysCyan → SysPurple`.
  - Height = `22 · level`.
  - Live: `level = 0.3 + 0.7·|sin(6t + 1.3i)|`.
  - Not live: `level = 0.2` (still).

### 2.4 Signal arcs (iOS `wave.3.right`)
- Three arcs opening to the right (like a Wi-Fi symbol turned sideways), about 20 sp tall, `SysCyan`, drawn with rounded line caps.
- Placed on the phone's **right edge**, shifted **+26** to the right so they sit over the start of the gap.
- While **not live**: the arcs light up **one after another, inner → outer**, looping. Each arc's alpha goes 0.3 → 1 → 0.3, staggered ~0.25 s, full cycle ~1 s. This approximates iOS's built-in "variable colour" symbol animation.
- When live: fade to alpha 0.

### 2.5 Beam
- **Track:** full-width capsule, white 10%.
- **Fill:** capsule from the left, width `W · progress`, horizontal gradient `SysCyan → SysPurple`, glow `SysCyan` 70% r6.
- **While pairing** (`0 < progress < 1`, not live): a white dot (8 dp, glow `SysCyan` r4) at `x = max(0, W·progress − 8)`, riding the end of the fill.
- **When live (streaming):** three white dots (6 dp, glow `SysCyan` r4) flow left → right forever.
  - Dot `i`: `p = frac(0.8t + i/3)`, `x = p·(W − 6)`, `alpha = sin(π·p)`.

### 2.6 Sound rings (live only)
- 3 circles, base size 120, stroke **2**, colour `SysCyan`.
- Ring `i`: `p = frac(0.6t + i/3)`, `scale = 0.6 + 1.1p`, `alpha = (1 − p)·0.6`. The rings expand and fade out.
- Hidden (alpha 0) when not live. Not touchable.

### 2.7 Floating notes (live only)
- Box 120×200. Five notes: `♪ ♫ ♪ ♬ ♫`.
- Note `i`:
  - Font: `18 + 4·(i % 3)` sp Bold.
  - Colour: even `i` = `SysCyan`, odd `i` = `SysPurple`.
  - `p = frac(0.35t + 0.2i)`
  - `x = W·(0.2 + 0.15i) + 14·sin(2π·p + i)`
  - `y = H·0.5 − p·H·0.5`: rises from the middle to the top.
  - `alpha = sin(π·p)`
- Hidden when not live.

### 2.8 Bluetooth speaker (also used on Screen 3)
Parameters: width `w`, `accent`, `beatRate`, `isConnected`, `isPlaying`.

Thump value: `beat = if (isPlaying) max(0, sin(π·beatRate·t))^6 else 0`, a sharp pulse `beatRate` times per second.

**Body**
- `w` × **1.55w**, rounded corners **0.2w**.
- Fill: vertical gradient `#333333 → #121212`.
- Border: accent at **70%** when playing, **25%** otherwise, width 1.5.

**Parts**, in a Column (spacing **0.08w**, centred):
1. **Tweeter:** circle **0.2w**, radial gradient `#595959 → #1A1A1A` (radius 0.1w), ring white 15% (1).
2. **Woofer:** box **0.72w** (call it `wf`), layered:
   - Base circle `#0D0D0D`
   - Ring: accent at alpha `0.35 + 0.6·beat`, width 2
   - Cone: circle **0.78·wf**, radial gradient `#4D4D4D → #141414` (radius 0.4·wf), scale `1 + 0.12·beat`
   - Dust cap: circle **0.26·wf**, gradient TL→BR `accent 90% → SysPurple 80%`, scale `1 + 0.25·beat`, glow accent at alpha `beat`, r8
3. **LED:** circle `max(4, 0.07w)`. `accent` with glow r4 when connected, otherwise white 25% with no glow.

**Whole speaker:** scale `1 + 0.03·beat`; glow accent 35% r18 when playing.

**On Screen 2:** `w = 96`, accent `SysCyan`, beatRate **2**. `isConnected` and `isPlaying` are both `live`, so the LED is off and nothing moves until connected.

### 2.9 Status pill
- Row (spacing 8): phase icon in the phase colour, then the phase text (15 sp SemiBold white).
- Padding 18 horizontal / 12 vertical. Pill shape, "material" fill, border phaseColor 50% (1).
- Glow phaseColor 40% r10; **no glow** while searching.
- On a phase change: the icon swaps with scale + fade, the text crossfades, and everything animates with spring 0.45.

| Phase | Icon | Colour | Text |
|---|---|---|---|
| SEARCHING | `CellTower` | white | "Searching for speakers…" |
| FOUND | `Speaker` | `SysCyan` | "Found: Party Speaker" |
| CONNECTED | `CheckCircle` | `SysGreen` | "Connected — you're live!" |

### 2.10 Pairing sequence (runs **once** when the screen opens)
```kotlin
LaunchedEffect(Unit) {
    beam.animateTo(0.6f, tween(1400, easing = EaseInOut))      // 0.0 → 1.4 s
    phase = FOUND                                              // spring 0.5 (damping 0.825)
    launch { beam.animateTo(1f, tween(800, easing = EaseInOut)) }
    delay(900)                                                 // → 2.3 s
    phase = CONNECTED                                          // spring 0.5 / 0.7
    view.performHapticFeedback(CONFIRM)                        // success haptic
}
```
- After connecting, **don't loop the pairing**. The live animations (level bars, streaming dots, rings, notes, speaker beat) run until the screen is left.
- The Continue button works **at any time**; the user never has to wait.

---

## 3. Screen 3: Karaoke Night

**Goal:** show the real use case, singing karaoke through a speaker while effects switch. **Visuals and haptics only, no sound.**

### 3.1 Layout
1. Flex gap (min 56).
2. **Header**, Column, spacing **8**, horizontal padding **24**, centred:
   - "**Karaoke Night**": 30 sp Bold white, shrink to 80% if needed.
   - "**Sing live through any speaker and switch effects on the fly.**": 15 sp white 75%, centred.
3. Flex gap (min 20).
4. **Stage** (3.3): full width, horizontal padding **20**, height **flexible between 250 and 320** (`Modifier.heightIn(250.dp, 320.dp)` plus a share of the flex space).
5. Flex gap (min 16).
6. **Effect picker** (3.8), centred.
7. Flex gap (min 20).
8. **Continue** button (0.8). Tapping it finishes onboarding (0.1).

### 3.2 Effects (in order, loops)
| Effect | Emoji | Colour (primary) | Secondary | Speaker beatRate |
|---|---|---|---|---|
| Autotune | 🎵 | `SysCyan` | `SysPurple` | 2.0 |
| Robot | 🤖 | `#33FF99` | `SysTeal` | 2.4 |
| Chipmunk | 🐿️ | `SysYellow` | `SysOrange` | 3.2 |
| Demon | 😈 | `#FF3340` | `#730099` | 1.4 |
| Echo | 🏛️ | `#598CFF` | `SysIndigo` | 1.8 |

Starts on **Autotune**.

### 3.3 Stage container
- Rounded corners **24**, content clipped.
- Border 1.5, gradient TL→BR `effect.primary 80% → effect.secondary 50%`.
- Glow `effect.primary` 35% r20.
- All colour changes on the stage animate **easeInOut 0.4 s**.
- Layers: **Stage background** (3.4), then the **content column** (3.5).

### 3.4 Stage background
- **Base:** vertical gradient `#120829 → black`.
- **Spotlight cone shape:** a trapezoid. Top edge 8 wide at the top centre (±4); bottom edge full width.
- **Cone 1:**
  - Size: width **0.55W**, height **1.1H**, centred at (**0.25W**, **0.55H**), so its top sits at the stage top.
  - Rotation about its top centre: `sin(0.7t)·18 − 10` degrees.
  - Fill: vertical gradient `primary 55% → primary 0%`. Blur 10 (API 31+), **BlendMode.Screen**.
- **Cone 2:** same size, centred at **0.75W**. Rotation `cos(0.6t)·18 + 10` degrees. Fill `SysPurple 50% → 0%`, blur 10, Screen blend.
- **Crowd silhouette:** a band at the bottom, height **0.2H**, filled black 90%. **9 people**, with `w = W/9`:
  - `headR = 0.22w`, `cx = (i + 0.5)·w`
  - `bob = 3·sin(3t + 1.7i)`
  - `headY = bandTop + headR + 4 + bob + (i % 2)·5`
  - Head: circle of radius `headR` at `(cx, headY)`
  - Body: rounded rect, x = `cx − 0.4w`, y = `headY + 0.8·headR`, width `0.8w`, height down to the band bottom, corner radius `0.3w`

### 3.5 Stage content
Column, spacing **14**, top padding **16**:
1. **Top row** (horizontal padding 14):
   - Speaker (2.8) with `w = 36`, accent = effect primary, beatRate = effect beatRate, connected + playing.
   - Flexible space (min 8)
   - **Effect badge** (3.6)
   - Flexible space (min 8)
   - The same speaker again
2. **Effect waveform** (1.13): style = current effect, no tint, height **60**, horizontal padding 20.
3. **Karaoke lyrics** (3.7): horizontal padding 12.
4. Flexible space.

### 3.6 Effect badge
- Row (spacing 8): emoji 28 sp, then the name (18 sp Bold, rounded font, white, 1 line).
- Padding 16 horizontal / 8 vertical. Pill fill primary 25%, border primary 80% (1.5), glow primary 60% r10.
- On an effect change: the old badge leaves and the new one enters with **scale from 0.4 + fade**.

### 3.7 Karaoke lyrics
**Lines (looping):**
1. "Turn the music up"
2. "Sing it like a star"
3. "Any speaker, any room"
4. "The stage is yours"

**Timing:**
- Each line lasts **2.8 s**.
- `index = floor(t / 2.8) % 4`
- `progress = min(1, (t % 2.8) / (2.8 · 0.85))`. The line is fully highlighted at 85% of its time, then holds.

**Current line** (word by word):
- Split the line into words. Row, spacing 6, 20 sp W800 rounded font, 1 line (shrink to 60% if needed), glow primary 50% r6.
- Each word is drawn twice:
  - Base: white 40%.
  - On top: the same word with a horizontal gradient `[primary, secondary]`, **clipped from the left** to width `wordWidth · fill`.
- `fill_i = clamp(progress·wordCount − i, 0, 1)`, so the colour fills each word left to right in turn, like real karaoke.

**Next line**, under the current one (spacing 6): 15 sp SemiBold, white 35%, 1 line.

### 3.8 Effect picker
Row, spacing **10**, with the 5 effects. Each item is a Column (spacing 6):
- **Circle:** **50**, emoji 24 sp.
  - Selected: fill primary 30%, border primary (2), scale **1.1**, glow primary 70% r8.
  - Not selected: fill white 8%, border white 15% (1), scale 1.
- **Label:** 11 sp SemiBold, white 100% when selected or 55% otherwise, 1 line, never truncated.

Use no ripple, and set accessibility to "`<name>` effect" + selected state.

### 3.9 Behaviour & timing
- **Auto-cycle:** every **2.2 s**, if more than **5 s** have passed since the last chip tap, go to the next effect (spring **0.45 / 0.7**).
- **Chip tap:** remember the tap time, then select that effect (spring **0.4 / 0.7**). Auto-cycling pauses for 5 s.
- **Every** effect change (auto or tap) → **selection haptic** (0.7).
- **No sound** on this screen.
- The lyrics, spotlights, crowd and speakers run continuously and don't reset when the effect changes; only the colours and styles change.

```kotlin
LaunchedEffect(Unit) {
    while (isActive) {
        delay(2200)
        if (System.currentTimeMillis() - lastManualTapMs > 5000) effect = effect.next()
    }
}
LaunchedEffect(effect) { view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK) }
// The keyed effect also fires once on first composition; skip that first run if you
// don't want a tick when the screen opens (iOS only ticks on changes).
```

---

## 4. QA checklist (compare side by side with the iOS app)
- [ ] 3 pages only, no swipe, no page dots; every button says **Continue**. The last one finishes onboarding without asking for permissions or a review.
- [ ] **Screen 1:**
  - [ ] Entrance finishes in about 1 s.
  - [ ] The orbit rotates slowly and the active emoji is highlighted.
  - [ ] The bubble changes voice every ~1.6 s, silently.
  - [ ] Tapping the mic: next voice, sound plays, light haptic, particle burst. Auto-cycling pauses for 3 s.
  - [ ] The hint text changes after the first tap.
  - [ ] The shimmer sweeps across the button every 3 s.
- [ ] **Screen 2:**
  - [ ] Pairing happens once: Searching (beam to 60%) → Found at 1.4 s → Connected at 2.3 s with a success haptic.
  - [ ] Then the live loop runs: level bars, streaming dots, rings, notes and speaker thump.
  - [ ] Continue works immediately.
- [ ] **Screen 3:**
  - [ ] Effects auto-switch every 2.2 s, each with a selection haptic.
  - [ ] Tapping a chip selects it and pauses auto-switching for 5 s.
  - [ ] Badge, waveform, borders and spotlight colours follow the effect.
  - [ ] Lyrics highlight word by word.
- [ ] Fits a 360×640 dp phone without scrolling; nothing overlaps the status or navigation bars.
- [ ] With "Remove animations" on, the continuous motion is frozen (0.7).
- [ ] Leaving any page stops its timers and sound (check that CPU use drops on the main screen).
- [ ] Copy uses "phone", not "iPhone", and the social-proof numbers are the real Google Play ones.
