# WaveEffects 🎚️🔊

[![Android CI](https://img.shields.io/badge/CI-GitHub_Actions-2088FF.svg?logo=github-actions&logoColor=white)](.github/workflows/android.yml)
[![Android Min SDK](https://img.shields.io/badge/minSdk-28-blue.svg)](https://developer.android.com/about/versions/pie)
[![Target SDK](https://img.shields.io/badge/targetSdk-36-green.svg)](https://developer.android.com)
[![Compose BOM](https://img.shields.io/badge/Compose_BOM-2026.03.01-blueviolet.svg)](https://developer.android.com/jetpack/compose)
[![Material 3 Expressive](https://img.shields.io/badge/Material_3-Expressive_%2B_Material_You-orange.svg)](https://m3.material.io)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)

**WaveEffects** is an advanced audio DSP (Digital Signal Processing) engine and equalizer for Android, inspired by the Linux powerhouse **EasyEffects** (formerly PulseEffects). Built natively with Jetpack Compose, Material 3 Expressive, and Android's low-level audio effects framework (`DynamicsProcessing`), WaveEffects brings desktop-class acoustic tuning, multiband compression, brickwall limiting, AutoEQ headphone calibration, and real-time audio visualization to mobile devices.

---

## 🌟 Highlights & Features

### 1. 🎛️ 10-Band Parametric / Graphic Equalizer
- **10 ISO Octave Bands**: 31 Hz, 62 Hz, 125 Hz, 250 Hz, 500 Hz, 1 kHz, 2 kHz, 4 kHz, 8 kHz, and 16 kHz with ±12 dB high-precision fader adjustments (0.5 dB step granularity).
- **Smooth Bézier Curve Visualization**: Real-time frequency response curve plotted using smooth Catmull-Rom cubic Bézier splines with adaptive gradient fill.
- **Preset Management**: Built-in reference presets (*Flat*, *Bass Boost*, *Harman Target*, *Vocal Clarity*, *Electronic Dance*, *Acoustic*) plus full support for saving, naming, and deleting custom user presets.

### 2. 🎧 AutoEQ Headphone Calibration Integration
- **Pre-Loaded Harman Target Database**: Instant one-tap compensation profiles for popular audiophile and consumer headphones:
  - *Sony*: WH-1000XM5, WH-1000XM4
  - *Apple*: AirPods Pro 2, AirPods Max
  - *Sennheiser*: HD 600, HD 650
  - *Beyerdynamic*: DT 770 Pro, DT 990 Pro
  - *Audio-Technica*: ATH-M50x
  - *Moondrop*: Chu II
  - *Samsung*: Galaxy Buds2 Pro
- **Universal Custom AutoEQ Importer**: Paste and calibrate raw text directly from the [AutoEq project](https://github.com/jaakkopasanen/AutoEq):
  - **GraphicEQ**: `GraphicEQ: 20 0.5; 25 1.0; 31.5 1.8; ...` (logarithmic frequency interpolation).
  - **EqualizerAPO Parametric PEQ**: `Filter 1: ON PK Fc 31 Gain 2.5 Q 1.41 ...` with exact biquad bell filter center-frequency mapping.

### 3. ⚡ Studio-Grade Dynamics Processing
- **Multiband Compressor**: Fine-tune dynamic range and punch without clipping:
  - Threshold (-60 dB to 0 dB)
  - Knee Width (0 dB to 20 dB)
  - Ratio (1:1 to 50:1)
  - Attack (1 ms to 200 ms) & Release (10 ms to 1000 ms)
  - Makeup Gain (-12 dB to +12 dB)
- **Brickwall Peak Limiter**: Distortion protection and master leveling:
  - Threshold (-20 dB to 0 dB)
  - Attack (0.1 ms to 50 ms) & Release (10 ms to 1000 ms)
  - Post Gain (-12 dB to +12 dB)

### 4. 🌌 Spatial & Acoustic Enhancement Suite
- **Bass Boost**: Deep sub-harmonic low-end resonance engine (0–1000 strength).
- **Virtualizer (Spatializer)**: Binaural soundstage widener for headphones and external stereos.
- **Loudness Enhancer**: Clean, low-distortion digital pre-amplifier for quiet media tracks (up to +12 dB target gain).
- **Environmental Reverb**: Natural acoustic simulation with presets (*Small Room*, *Medium Room*, *Large Room*, *Medium Hall*, *Large Hall*, *Plate*).

### 5. 📊 Real-Time Spectrum Visualizer & Audio Routing
- **Dual Visualizer Engine**: 16-band real-time FFT frequency spectrum analyzer combined with a fluid 64-point oscilloscope waveform overlay (30 FPS).
- **Dynamic Audio Session Routing**: Automatically intercepts session broadcasts (`OPEN_AUDIO_EFFECT_CONTROL_SESSION`) from media apps (Spotify, YouTube Music, Tidal, VLC) to apply effects per-app or system-wide (Session 0).
- **Built-in DSP Test Bench**: On-demand harmonic frequency sweeps and pink noise generator to audition EQ profiles and compression live without needing external media playing.

### 6. 🎨 Material 3 Expressive & Material You Dynamic Theming
- **Wallpaper Adaptive Color (Material You)**: On Android 12+ (API 31+), the entire interface dynamically pulls and blends its color palette from your current Android wallpaper.
- **One-Tap Theme Switcher**: Dedicated palette toggle in the top bar to switch instantly between **Wallpaper Adaptive** and **Expressive Neon Mint & Electric Violet**.
- **Tactile Expressive Design**: Pill-shaped master toggle, tactile faders, high-contrast dark surfaces, and fluid tab transitions.

---

## 🏗️ Architecture & Tech Stack

```
com.droid.waveeffects
├── audio/
│   └── AudioDemoPlayer.kt         # PCM audio synthesizer (Sine sweep & Pink noise)
├── domain/
│   ├── AutoEqDatabase.kt          # Pre-loaded Harman calibrated profiles
│   ├── AutoEqParser.kt            # EqualizerAPO Parametric PEQ & GraphicEQ parser
│   ├── AudioSessionRepository.kt  # StateFlow management of active audio sessions
│   ├── Models.kt                  # Serializable presets, bands, compressor & limiter configs
│   └── PresetRepository.kt        # JSON-backed custom preset storage
├── dsp/
│   ├── AndroidDspEngine.kt        # AudioEffect HAL controller (DynamicsProcessing, BB, Virt, Reverb)
│   └── DspEngine.kt               # Pure Kotlin DSP engine interface
├── service/
│   ├── AudioEffectsService.kt     # Foreground Service (mediaPlayback) with notification controls
│   └── AudioSessionReceiver.kt    # BroadcastReceiver for Android audio effect control sessions
├── theme/
│   ├── Color.kt                   # M3 Expressive tokens & fallback neon palettes
│   ├── Theme.kt                   # Material You dynamicDarkColorScheme / dynamicLightColorScheme
│   └── Type.kt                    # M3 Typography specifications
└── ui/
    ├── AudioEffectsViewModel.kt   # MVI/UDF ViewModel managing state & 30 FPS visualizer polling
    ├── components/
    │   ├── EqualizerCurve.kt      # Custom Canvas Bézier spline EQ curve renderer
    │   └── SpectrumVisualizer.kt  # FFT bars and oscilloscope waveform Canvas
    ├── main/
    │   └── MainScreen.kt          # Scaffold, TopAppBar, bottom navigation & tab router
    └── tabs/
        ├── EqualizerTab.kt        # 10-band sliders, curve, and preset chips
        ├── AutoEqTab.kt           # Headphone search, brand chips & custom text importer
        ├── DynamicsTab.kt         # Multiband compressor & peak limiter controls
        ├── EffectsTab.kt          # Bass Boost, Virtualizer, Loudness & Reverb cards
        └── SessionsTab.kt         # Live spectrum analyzer, test bench, & session list
```

---

## 🛠️ Prerequisites & Requirements

- **Android Host**: Android 9.0+ (API level 28 minimum, tested through Android 16 / API 36).
- **Development Toolchain**:
  - **JDK**: Java 21 (recommended) or Java 17.
  - **Android SDK**: Build-Tools `36.0.0` or higher, Platform `android-36`.
  - **Gradle**: 9.0+ with Android Gradle Plugin (AGP) `9.0.1`.
- **Runtime Permissions**:
  - `POST_NOTIFICATIONS` (Android 13+): Background service notification controls.
  - `RECORD_AUDIO`: Required by Android OS for `Visualizer` FFT capture (no mic audio is ever recorded or transmitted).
  - `FOREGROUND_SERVICE` & `FOREGROUND_SERVICE_MEDIA_PLAYBACK`: Continuous background equalizer operation.

---

## 🚀 Building & Running

### 1. Clone the Repository
```bash
git clone https://github.com/your-username/WaveEffects.git
cd WaveEffects
```

### 2. Configure Local Properties
Ensure `local.properties` points to your Android SDK:
```properties
sdk.dir=/path/to/your/android/sdk
```

### 3. Run JVM Unit Tests
WaveEffects includes comprehensive unit tests verifying parametric biquad calculations, GraphicEQ logarithmic interpolation, and preset serialization:
```bash
./gradlew testDebugUnitTest
```
*(On I/O-heavy Linux/Android laptop setups, add: `--project-cache-dir="$HOME/.cache/projects/WaveEffects/gradle"`)*

### 4. Build Debug APK
```bash
./gradlew assembleDebug
```
The compiled debug APK will be generated at:
```
app/build/outputs/apk/debug/WaveEffects-debug.apk
```

### 5. Install onto Device
Using ADB:
```bash
adb install -r app/build/outputs/apk/debug/WaveEffects-debug.apk
```

### 6. Build Release APK
```bash
./gradlew assembleRelease
```

---

## 💡 How to Use WaveEffects

1. **First Launch**: Open WaveEffects and grant the optional notification and visualizer permissions when prompted.
2. **Auditioning the DSP**:
   - Go to the **Sessions** tab.
   - Tap **Start Test Audio** to trigger the built-in pink noise or harmonic sweep.
   - Switch between **Equalizer**, **Dynamics**, and **Effects** tabs to hear your tweaks live.
3. **Calibrating Your Headphones (AutoEQ)**:
   - Navigate to the **AutoEQ** tab.
   - Filter by brand or search for your headphone model (e.g. *Sony WH-1000XM5*, *AirPods Pro 2*, *HD 600*).
   - Tap **Apply** to instantly load the Harman compensation curve.
   - Or tap **Import** to paste any Parametric EQ or GraphicEQ profile from AutoEq.
4. **Custom Presets**:
   - In the **Equalizer** tab, drag the 10 frequency faders to your preference.
   - Tap **Save** in the top-right corner to store your preset for later recall.
5. **Theme Switching**:
   - Tap the **Palette icon** in the top app bar to toggle between dynamic Android wallpaper colors and the Expressive Obsidian Neon style.
6. **Master Bypass**:
   - Toggle the **ACTIVE / BYPASS** switch at any time in the top-right corner to do quick A/B comparisons against your unaltered audio.

---

## 🔒 Play Protect & Privacy Guardrails

- **Zero Telemetry / Offline First**: WaveEffects operates 100% locally on device. No audio, identifiers, or usage metrics are collected or transmitted.
- **Audio Privacy**: While `RECORD_AUDIO` is required by the Android framework to access the `Visualizer` API, WaveEffects only reads FFT frequency magnitudes; it never opens audio input streams or microphones.
- **Safe Session Handling**: Audio effects are gracefully released upon disconnection or app termination to prevent audio HAL leaks or battery drain.

---

## 📜 License

```
Copyright 2026 WaveEffects Contributors

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
