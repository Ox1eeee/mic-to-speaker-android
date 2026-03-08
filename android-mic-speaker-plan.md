# Android Mic-to-Speaker App with Voice Effects — App Plan

## Overview

A real-time voice transformation Android app that captures microphone input and routes it to speakers (Bluetooth, wired, or built-in) with live voice effects applied. Users hear their voice transformed in real-time with effects like Robot, Chipmunk, Deep Voice, Echo, Alien, and Radio.

**Platform:** Android 8.0+ (API 26+)  
**Language:** Kotlin  
**Architecture:** MVVM  
**Last Updated:** March 2026

---

## Core Technologies

### 1. AudioRecord + AudioTrack (Android Native Audio)
Android's low-level audio APIs for real-time mic capture and speaker output.

- `AudioRecord` — Captures raw PCM audio from the microphone
- `AudioTrack` — Plays processed audio to the output device
- `AudioManager` — Manages audio routing (Bluetooth, speaker, earpiece)
- `AudioFocusRequest` — Handles audio focus and interruptions

### 2. MediaRecorder / AudioEffect (Android Audio Effects)
Built-in Android audio effects framework.

- `BassBoost` — Low-frequency enhancement (Deep Voice effect)
- `Equalizer` — Frequency shaping for Radio/Alien effects
- `PresetReverb` — Echo and reverb effects
- `Visualizer` — Real-time amplitude/waveform data for UI

### 3. Oboe (High-Performance Audio Library)
Google's C++ audio library (via JNI/NDK) for ultra-low latency audio processing.

- Sub-10ms latency on supported devices
- Automatic stream management
- Handles audio device changes gracefully
- Preferred for real-time voice effects

### 4. Jetpack Compose
Modern declarative UI framework for Android.

- `ViewModel` + `StateFlow` for reactive state management
- `remember` / `mutableStateOf` for local UI state
- Material 3 theming with custom gradients
- Animated composables for waveform visualization

### 5. Google Play Billing Library
Subscription and in-app purchase management.

- One-time purchase and subscription support
- Billing flow management
- Purchase verification and acknowledgment
- Subscription status querying

### 6. RevenueCat (Optional / Recommended)
Cross-platform subscription management (same as iOS version).

- Simplifies Play Billing integration
- Entitlement management
- Paywall A/B testing
- Purchase restoration

---

## Architecture (MVVM)

```
┌────────────────────────────────────────────┐
│                   UI Layer                  │
│  MainActivity, HomeScreen, OnboardingScreen │
│  PaywallScreen, SettingsScreen              │
└──────────────────┬─────────────────────────┘
                   │ observes StateFlow
                   ▼
┌────────────────────────────────────────────┐
│              ViewModel Layer                │
│  AudioViewModel, SubscriptionViewModel      │
│  OnboardingViewModel, RatingViewModel       │
└──────────────────┬─────────────────────────┘
                   │ calls
                   ▼
┌────────────────────────────────────────────┐
│               Domain / Model Layer          │
│  AudioEngine, VoiceEffect, AudioRouter      │
│  SubscriptionManager, PreferencesManager    │
└────────────────────────────────────────────┘
```

### File Structure

```
app/
├── src/main/
│   ├── java/com.yourapp.micspeaker/
│   │   ├── audio/
│   │   │   ├── AudioEngine.kt            # Core audio processing
│   │   │   ├── AudioViewModel.kt         # Audio state management
│   │   │   ├── VoiceEffect.kt            # Effect definitions & params
│   │   │   └── AudioPreviewManager.kt    # Onboarding audio previews
│   │   │
│   │   ├── ui/
│   │   │   ├── home/
│   │   │   │   ├── HomeScreen.kt         # Main app screen
│   │   │   │   └── WaveformVisualizer.kt # Animated waveform
│   │   │   ├── onboarding/
│   │   │   │   └── OnboardingScreen.kt   # 5-screen onboarding
│   │   │   ├── settings/
│   │   │   │   └── SettingsScreen.kt     # App settings
│   │   │   ├── paywall/
│   │   │   │   └── PaywallScreen.kt      # Subscription screen
│   │   │   └── theme/
│   │   │       ├── Theme.kt              # Material 3 theme
│   │   │       └── Color.kt              # Color palette
│   │   │
│   │   ├── billing/
│   │   │   ├── BillingManager.kt         # Play Billing / RevenueCat
│   │   │   └── SubscriptionViewModel.kt
│   │   │
│   │   ├── utils/
│   │   │   ├── PermissionHelper.kt       # Runtime permissions
│   │   │   ├── RatingManager.kt          # In-app review (Play API)
│   │   │   └── PreferencesManager.kt     # DataStore preferences
│   │   │
│   │   └── MainActivity.kt
│   │
│   └── res/
│       ├── raw/                          # Effect preview audio files
│       └── AndroidManifest.xml
```

---

## Audio Engine Implementation

### 1. Audio Session Configuration

```kotlin
class AudioEngine {

    private lateinit var audioRecord: AudioRecord
    private lateinit var audioTrack: AudioTrack

    private val sampleRate = 44100       // CD quality
    private val channelIn = AudioFormat.CHANNEL_IN_MONO
    private val channelOut = AudioFormat.CHANNEL_OUT_STEREO
    private val encoding = AudioFormat.ENCODING_PCM_16BIT

    fun setupAudio() {
        val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelIn, encoding)

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            channelIn,
            encoding,
            bufferSize * 2
        )

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setEncoding(encoding)
                    .setChannelMask(channelOut)
                    .build()
            )
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setBufferSizeInBytes(bufferSize * 2)
            .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
            .build()
    }
}
```

### 2. Real-Time Audio Processing Loop

```kotlin
fun startProcessing(effect: VoiceEffect) {
    audioRecord.startRecording()
    audioTrack.play()

    processingThread = Thread {
        val buffer = ShortArray(bufferSize)

        while (isRunning) {
            val read = audioRecord.read(buffer, 0, bufferSize)
            val processed = applyEffect(buffer, read, effect)
            audioTrack.write(processed, 0, processed.size)
        }
    }
    processingThread.start()
}
```

### 3. Audio Graph (Conceptual)

```
No Effect:
Microphone → [AudioRecord] → [Buffer] → [AudioTrack] → Speaker

Effect Mode:
Microphone → [AudioRecord] → [Buffer] → [DSP Processing]
                                               ↓
                                        [Pitch Shift]
                                               ↓
                                        [Distortion]
                                               ↓
                                          [Reverb]
                                               ↓
                                        [AudioTrack] → Speaker
```

### 4. Bluetooth Audio Routing

```kotlin
fun routeToBluetoothSpeaker() {
    val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

    // Request audio focus
    val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
        .setAudioAttributes(AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .build())
        .build()

    audioManager.requestAudioFocus(focusRequest)

    // Route to Bluetooth A2DP if available
    if (audioManager.isBluetoothA2dpOn) {
        audioManager.startBluetoothSco()  // For Bluetooth mic if needed
        // A2DP is automatic for playback when connected
    }
}
```

---

## Voice Effects System

| Effect | Pitch Shift | Reverb | Distortion | Description |
|---|---|---|---|---|
| None | 0 | 0% | 0% | Clean passthrough |
| Robot | 0 | 20% | High | Metallic robotic tone |
| Chipmunk | +800 cents | 0% | 0% | High-pitched squeaky voice |
| Deep Voice | -400 cents | 10% | Low | Low, authoritative voice |
| Echo | 0 | 80% | 0% | Heavy reverb/echo effect |
| Alien | +300 cents | 30% | Medium | Otherworldly distortion |
| Radio | 0 | 5% | Medium | Bandpass filtered AM radio |

### Effect Implementation (DSP)

```kotlin
sealed class VoiceEffect {
    object None : VoiceEffect()
    object Robot : VoiceEffect()
    object Chipmunk : VoiceEffect()
    object DeepVoice : VoiceEffect()
    object Echo : VoiceEffect()
    object Alien : VoiceEffect()
    object Radio : VoiceEffect()
}

fun applyEffect(buffer: ShortArray, size: Int, effect: VoiceEffect): ShortArray {
    return when (effect) {
        is VoiceEffect.Chipmunk -> pitchShift(buffer, size, factor = 2.0f)
        is VoiceEffect.DeepVoice -> pitchShift(buffer, size, factor = 0.5f)
        is VoiceEffect.Echo -> applyReverb(buffer, size, wetMix = 0.8f)
        is VoiceEffect.Robot -> applyDistortion(buffer, size, gain = 3.0f)
        is VoiceEffect.Radio -> applyBandpassFilter(buffer, size, low = 300f, high = 3400f)
        is VoiceEffect.Alien -> pitchShift(applyDistortion(buffer, size, 1.5f), size, 1.3f)
        is VoiceEffect.None -> buffer
    }
}
```

---

## User Interface

### Home Screen Components

- **Power Button** — Start/stop mic-to-speaker routing
- **Effect Chips** — Horizontally scrollable effect selector
- **Waveform Visualizer** — Real-time animated amplitude bar
- **Volume Slider** — 0–100% output volume control
- **Echo Slider** — 0–100% reverb/echo mix control
- **Audio Route Button** — Switch between Speaker / Bluetooth / Earpiece
- **Settings Icon** — Opens settings screen

### Design Language

- Material 3 (Material You) design system
- Dark theme optimized
- Purple/cyan gradient backgrounds (matching iOS version)
- Glassmorphism cards for effect chips
- Smooth spring animations via `Animatable`

---

## Subscription & Monetization

### Tiers

| Feature | Free | Premium |
|---|---|---|
| None effect | ✅ | ✅ |
| All 6 effects | ❌ | ✅ |
| Bluetooth routing | ✅ | ✅ |
| Ad-free | ❌ | ✅ |

### Plans

- **Monthly** — $2.99/month
- **Annual** — $17.99/year (save 50%)

### Google Play Billing Integration

```kotlin
class BillingManager(private val context: Context) {

    private lateinit var billingClient: BillingClient

    fun startConnection() {
        billingClient = BillingClient.newBuilder(context)
            .setListener { billingResult, purchases ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    purchases?.forEach { handlePurchase(it) }
                }
            }
            .enablePendingPurchases()
            .build()

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    querySubscriptions()
                }
            }
            override fun onBillingServiceDisconnected() { /* retry */ }
        })
    }
}
```

---

## Onboarding Flow

5-screen onboarding (skip-free):

1. **Welcome Screen** — App name, logo, tagline with animation
2. **How It Works** — Animated mic → phone → speaker diagram
3. **Voice Effects Preview** — Interactive effect demos with real audio samples
4. **Bluetooth Setup** — How to connect Bluetooth speakers
5. **Permission Request** — Mic + Bluetooth permission explanation & prompt

After onboarding → trigger In-App Review prompt.

---

## Rating System

Using **Google Play In-App Review API**:

```kotlin
fun requestReview(activity: Activity) {
    val manager = ReviewManagerFactory.create(activity)
    manager.requestReviewFlow().addOnCompleteListener { request ->
        if (request.isSuccessful) {
            manager.launchReviewFlow(activity, request.result)
        }
    }
}
```

**Trigger points:**
1. After completing onboarding
2. On the user's 2nd app launch
3. After using an effect for 5+ minutes

---

## Required Permissions (AndroidManifest.xml)

```xml
<!-- Microphone access -->
<uses-permission android:name="android.permission.RECORD_AUDIO" />

<!-- Bluetooth audio routing -->
<uses-permission android:name="android.permission.BLUETOOTH" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT"
    android:minSdkVersion="31" />

<!-- Keep audio running in background -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MICROPHONE"
    android:minSdkVersion="34" />

<!-- Internet for billing/RevenueCat -->
<uses-permission android:name="android.permission.INTERNET" />
```

> **Note:** `RECORD_AUDIO` must also be requested at runtime (Android 6.0+).

---

## Background Processing (Foreground Service)

To keep audio running when the app is backgrounded, a Foreground Service with a persistent notification is required:

```kotlin
class AudioForegroundService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Mic Speaker Active")
            .setContentText("Voice effect is running")
            .setSmallIcon(R.drawable.ic_mic)
            .build()

        startForeground(1, notification)
        audioEngine.startProcessing(currentEffect)
        return START_STICKY
    }
}
```

---

## Dependencies (build.gradle)

```kotlin
dependencies {
    // Jetpack Compose
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.0")

    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")

    // Google Play Billing
    implementation("com.android.billingclient:billing-ktx:6.2.0")

    // RevenueCat (optional, recommended)
    implementation("com.revenuecat.purchases:purchases:7.0.0")
    implementation("com.revenuecat.purchases:purchases-ui:7.0.0")

    // In-App Review
    implementation("com.google.android.play:review-ktx:2.0.1")

    // DataStore (preferences)
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // Oboe (low-latency audio via NDK, optional)
    implementation("com.google.oboe:oboe:1.8.0")
}
```

---

## Key Differences from iOS Version

| Feature | iOS | Android |
|---|---|---|
| Audio API | AVFoundation / AVAudioEngine | AudioRecord + AudioTrack / Oboe |
| UI Framework | SwiftUI | Jetpack Compose |
| Subscriptions | RevenueCat + StoreKit | RevenueCat + Google Play Billing |
| Background Audio | AVAudioSession | Foreground Service |
| Bluetooth | AVAudioSession options | AudioManager + BluetoothAdapter |
| Rating Prompt | SKStoreReviewController | Play In-App Review API |
| Language | Swift | Kotlin |
| Architecture | MVVM + Combine | MVVM + StateFlow/Coroutines |

---

## Complete Feature List

### Audio Features
- Real-time microphone to speaker routing
- 7 voice effects (None, Robot, Deep, Echo, Alien, Radio, Chipmunk)
- Adjustable volume control (0–100%)
- Adjustable echo/reverb control (0–100%)
- Real-time amplitude visualization
- Bluetooth audio support (A2DP high-quality)
- Wired headphone support
- Built-in speaker support
- Low-latency processing (~10ms with Oboe)
- 44.1kHz sample rate (CD quality)

### User Interface
- Material 3 design with dark theme
- Gradient backgrounds (purple/cyan)
- Animated waveform visualizer
- Smooth Compose animations
- Horizontal scrolling effect chips
- Audio output route switcher

### Onboarding
- 5-screen interactive onboarding
- Audio effect previews
- Permission explanations
- Skip-free flow

### Monetization
- Google Play Billing (monthly + annual)
- RevenueCat integration
- Free tier (None effect only)
- Premium tier (all effects + ad-free)
- Restore purchases support
- Paywall with feature comparison

### App Management
- In-App Review (Play API)
- Launch count tracking
- DataStore user preferences
- Audio route change notifications
- Runtime permission handling
- Foreground service for background audio

---

**Platform:** Android 8.0+ (API 26+)  
**Language:** Kotlin  
**Framework:** Jetpack Compose + AVFoundation equivalent (AudioRecord/AudioTrack)  
**Architecture:** MVVM with StateFlow & Coroutines

---

## Development Phases

### Phase 1 — Project Setup & Core Audio (Foundation)
**Goal:** Establish the project skeleton and get basic mic-to-speaker passthrough working.

- [ ] Create Android project with Kotlin, API 26+ min SDK
- [ ] Configure `build.gradle` with all dependencies (Compose, Lifecycle, DataStore, Oboe, etc.)
- [ ] Set up MVVM folder structure (`audio/`, `ui/`, `billing/`, `utils/`)
- [ ] Define `AndroidManifest.xml` with all required permissions
- [ ] Implement `AudioEngine.kt` — `AudioRecord` + `AudioTrack` setup
- [ ] Implement basic real-time audio loop (mic → buffer → speaker, no effects)
- [ ] Implement `AudioManager` audio focus handling
- [ ] Create `AudioForegroundService.kt` with persistent notification
- [ ] Add runtime permission request for `RECORD_AUDIO`
- [ ] Verify clean passthrough on built-in speaker & wired headphones

**Deliverable:** App captures mic input and plays it through the speaker in real-time with no effects.

---

### Phase 2 — Voice Effects DSP Engine
**Goal:** Build the voice effects processing pipeline.

- [ ] Define `VoiceEffect.kt` sealed class (None, Robot, Chipmunk, DeepVoice, Echo, Alien, Radio)
- [ ] Implement `applyEffect()` dispatcher function
- [ ] Implement pitch shifting algorithm (Chipmunk +800¢, DeepVoice -400¢, Alien +300¢)
- [ ] Implement reverb/echo effect (configurable wet mix 0–100%)
- [ ] Implement distortion effect (Robot, Alien)
- [ ] Implement bandpass filter (Radio: 300Hz–3400Hz)
- [ ] Integrate effects into the real-time audio loop
- [ ] Add volume control (0–100% gain)
- [ ] Add echo/reverb mix slider control
- [ ] Test all 7 effects for quality and latency

**Deliverable:** All voice effects work in real-time with adjustable volume and echo controls.

---

### Phase 3 — Bluetooth & Audio Routing
**Goal:** Support all audio output paths (Bluetooth, wired, built-in speaker).

- [ ] Implement `AudioRouter.kt` — detect available audio devices
- [ ] Add Bluetooth A2DP routing support
- [ ] Add `BLUETOOTH` / `BLUETOOTH_CONNECT` runtime permission handling (API 31+)
- [ ] Implement audio route switching (Speaker / Bluetooth / Earpiece)
- [ ] Handle audio device connect/disconnect events gracefully
- [ ] Handle `BluetoothSco` for Bluetooth mic scenarios
- [ ] Test with Bluetooth speaker, wired headphones, and built-in speaker

**Deliverable:** Audio routes correctly to any connected output device with seamless switching.

---

### Phase 4 — UI: Theme, Home Screen & Visualizer
**Goal:** Build the main app UI with Jetpack Compose.

- [ ] Set up Material 3 theme (`Theme.kt`, `Color.kt`) — dark theme, purple/cyan gradients
- [ ] Build `HomeScreen.kt` layout:
  - [ ] Power button (start/stop)
  - [ ] Horizontally scrollable effect chips
  - [ ] Volume slider
  - [ ] Echo slider
  - [ ] Audio route button
  - [ ] Settings icon
- [ ] Build `WaveformVisualizer.kt` — real-time animated amplitude bars using `Visualizer` data
- [ ] Implement glassmorphism card style for effect chips
- [ ] Add smooth spring animations (`Animatable`)
- [ ] Wire `AudioViewModel.kt` — expose `StateFlow` for UI state (isRunning, currentEffect, volume, amplitude)
- [ ] Connect UI controls to `AudioEngine` via ViewModel

**Deliverable:** Fully functional and styled home screen controlling all audio features.

---

### Phase 5 — Onboarding Flow
**Goal:** Implement the 5-screen onboarding experience.

- [ ] Build `OnboardingScreen.kt` with pager-based navigation (skip-free)
  - [ ] Screen 1: Welcome — app name, logo, tagline with animation
  - [ ] Screen 2: How It Works — animated mic → phone → speaker diagram
  - [ ] Screen 3: Voice Effects Preview — interactive demos with pre-recorded audio samples
  - [ ] Screen 4: Bluetooth Setup — connection instructions
  - [ ] Screen 5: Permission Request — mic + Bluetooth explanation & prompt
- [ ] Create `AudioPreviewManager.kt` — plays effect demo audio from `res/raw/`
- [ ] Add pre-recorded audio sample files to `res/raw/`
- [ ] Implement `OnboardingViewModel.kt` — track completion state
- [ ] Store onboarding-completed flag in `PreferencesManager.kt` (DataStore)
- [ ] Show onboarding only on first launch

**Deliverable:** New users see a polished 5-screen onboarding before reaching the home screen.

---

### Phase 6 — Subscription & Paywall
**Goal:** Implement monetization with free/premium tiers.

- [ ] Implement `BillingManager.kt` — Google Play Billing connection, purchase flow, acknowledgment
- [ ] (Optional) Integrate RevenueCat SDK for simplified billing management
- [ ] Implement `SubscriptionViewModel.kt` — expose subscription state
- [ ] Build `PaywallScreen.kt` — feature comparison table, monthly/annual plan cards
- [ ] Gate premium effects behind subscription check (free = None effect only)
- [ ] Add "Restore Purchases" flow
- [ ] Configure products in Google Play Console (monthly $2.99, annual $17.99)
- [ ] Test with Google Play Billing test accounts

**Deliverable:** Working paywall with subscription purchase, restoration, and entitlement gating.

---

### Phase 7 — Settings, Rating & Polish
**Goal:** Final features, edge-case handling, and polish.

- [ ] Build `SettingsScreen.kt` — output device selector, about, restore purchases link
- [ ] Implement `RatingManager.kt` — Play In-App Review API
  - [ ] Trigger after onboarding completion
  - [ ] Trigger on 2nd app launch
  - [ ] Trigger after 5+ min of effect usage
- [ ] Implement launch count tracking in `PreferencesManager.kt`
- [ ] Handle audio interruptions (phone calls, other apps taking focus)
- [ ] Handle foreground service lifecycle correctly (API 34+ `FOREGROUND_SERVICE_MICROPHONE`)
- [ ] Add proper error handling and edge-case UX (no mic, permissions denied, etc.)
- [ ] Performance optimization — test latency, reduce buffer sizes where possible
- [ ] (Optional) Integrate Oboe (NDK) for sub-10ms latency on supported devices
- [ ] Final UI polish — animations, transitions, accessibility
- [ ] QA pass across multiple devices and Android versions

**Deliverable:** Production-ready app with all features complete, polished, and tested.

---

### Phase Summary

| Phase | Focus | Key Output |
|-------|-------|------------|
| **1** | Project Setup & Core Audio | Mic → Speaker passthrough |
| **2** | Voice Effects DSP | All 7 effects working in real-time |
| **3** | Bluetooth & Routing | Multi-device audio output |
| **4** | UI & Visualizer | Styled home screen with controls |
| **5** | Onboarding | 5-screen first-launch experience |
| **6** | Subscriptions | Paywall & premium gating |
| **7** | Settings, Rating & Polish | Production-ready release |
