# Caliber — Mobile Gamer Calibration & Diagnostic Suite

A high-performance, native Android hardware diagnostics and calibration suite engineered specifically for mobile esports competitors, phone hardware reviewers, and competitive gamers. Built with **Kotlin** and **Jetpack Compose**, designed around an **OLED-Optimized Cyber Aesthetic**, and powered by high-frequency hardware event telemetry pipelines decoupled from Compose recomposition churn.

100% on-device and privacy-first — no cloud dependencies, no accounts, and zero telemetry tracking.

---

## 🎮 Key Capabilities & Native Modules

### 1. Touchscreen & Digitizer Lab (Direct API)
* **True Hardware Polling Rate (Hz):** Bypasses OS frame rate limitations (e.g. 120Hz display refresh) by capturing sub-frame historical coordinates via `MotionEvent.getHistoricalX()` / `getHistoricalY()` and `getHistoricalEventTime()`. Accurately benchmarks true digitizer hardware capabilities (240Hz, 360Hz, 480Hz, 720Hz+).
* **10-Point Claw Grip Matrix & Pressure:** Multi-finger tracking designed for 4-finger to 6-finger claw-grip mobile esports layouts. Renders real-time contact disc halo scaled by `MotionEvent.getPressure()` and `MotionEvent.getSize()`.
* **Historical Packet Jitter Engine:** Computes standard deviation ($\sigma$) of packet arrival intervals in milliseconds and historical packets per VSYNC frame.
* **Direct Canvas Rendering:** Zero-allocation, high-speed vector trails using `withFrameNanos` and `DrawScope` without triggering Compose recomposition bottlenecks.

### 2. Motion Sensor Lab (6-Axis IMU & Zero-Point Drift)
* **High-Speed Sensor Hook:** Hooks into `SensorManager` with `SENSOR_DELAY_FASTEST` for `Sensor.TYPE_GYROSCOPE` and `Sensor.TYPE_ACCELEROMETER` (>200Hz–400Hz+ sampling).
* **Zero-Point Drift Calibration:** Measures resting gyro bias $(\mu_x, \mu_y, \mu_z)$, calculates drift magnitude $\|\vec{\omega}\|$ in degrees/sec and radians/sec, and analyzes sensor noise variance $\sigma^2$.
* **Oscilloscope Waveform:** 3-channel continuous rolling graph (Gyro X in Neon Cyan, Y in Neon Green, Z in Warning Orange).
* **Tare / Calibration Bias Elimination:** Single-tap hardware offset subtraction to eliminate unwanted gyro crosshair drift in titles like *PUBG Mobile*, *Call of Duty: Warzone Mobile*, and *Apex Legends Mobile*.

### 3. Hardware Triggers & Floating Telemetry HUD
* **Gaming Phone Shoulder Key Sniffer:** Sniffs physical and ultrasonic capacitive shoulder triggers (L1, R1, L2, R2, Volume rocker mapped keys found on Asus ROG Phone, Black Shark, RedMagic).
* **Actuation Latency & Clicks-Per-Second (CPS):** Measures press-down to release duration in milliseconds, hold time, and real-time rapid-fire CPS.
* **System Overlay HUD Widget:** Runs as an Android Foreground Service using `Settings.ACTION_MANAGE_OVERLAY_PERMISSION` (`SYSTEM_ALERT_WINDOW`). Draws an ultra-compact, draggable HUD over active 3D games displaying live hardware FPS (via `Choreographer`), touch polling rate (Hz), and controller polling rate (Hz).

### 4. Controller Precision Lab (Hall Effect Focus)
* **InputDevice Interrogation:** Interrogates connected gamepads via `InputDevice.getDeviceIds()`, identifying Vendor ID, Product ID, battery status, and axis capabilities.
* **Hall Effect vs Potentiometer Stick Analysis:**
  * Maps 360 angular sectors ($1^\circ$ bins) across stick rotation sweeps.
  * Calculates outer boundary circularity error:
    $$\text{Circularity Error \%} = \frac{1}{N} \sum_{i=1}^{N} |r_i - 1.0| \times 100\%$$
  * Distinguishes high-end Hall Effect / TMR electromagnetic sensors (smooth circular gating, error $< 5\%$) from traditional potentiometers (square clipping, error $> 15\%$).
* **Inner & Outer Deadzone Detection:** Detects residual stick center drift and premature boundary saturation.
* **Dual Rumble Motor Testing:** Tests asymmetric haptic feedback motors (heavy low-frequency rumble vs crisp high-frequency rumble) with independent amplitude sliders.

### 5. Bare-Metal Audio Latency (Acoustic Matched-Filter Ping)
* **Bypasses Software Audio Mixer:** Utilizes `AudioTrack` and `AudioRecord` configured with `PERFORMANCE_MODE_LOW_LATENCY` and `AudioAttributes.FLAG_LOW_LATENCY` (AAudio / Oboe fast track HAL pipeline).
* **Acoustic Loopback Correlation:**
  1. Synthesizes a 15ms Hann-windowed 2.4 kHz transient audio chirp.
  2. Emits chirp via speaker / Bluetooth earbud / 3.5mm DAC.
  3. Records physical arrival via microphone at 48,000 Hz.
  4. Runs cross-correlation (matched filter) $R_{xy}[k] = \sum_n x[n] y[n+k]$ to locate exact sample index of physical sound arrival.
  5. Computes sample-accurate round-trip audio transit delay ($ms$) and jitter standard deviation.
* **Audio Routing Detection:** Identifies connected routes: Analog 3.5mm / Type-C DAC ($<25$ms), Low Latency aptX / gaming mode ($40-70$ms), vs standard Bluetooth SBC/AAC ($120-220$ms).

### 6. Sensitivity Converter
* Mathematical cross-engine sensitivity translator across 12+ competitive mobile FPS/TPS titles:
  * *PUBG Mobile / BGMI*
  * *Call of Duty: Mobile (CODM)*
  * *Warzone Mobile*
  * *Apex Legends Mobile*
  * *Free Fire / Free Fire MAX*
  * *Arena Breakout*
  * *Rainbow Six Mobile*
  * *Blood Strike*
  * *T3 Arena*
  * *Critical Ops*
  * *Standoff 2*
  * *Special Forces Group 2 / Combat Master*
* Supports scoped optics multipliers (Red Dot / Holo, 2x, 3x, 4x, 6x, 8x / Sniper) and FoV scaling ratios.

### 7. Rhythm Game Audio Offset Calibrator
* Interactive audio-visual synchronizer designed for rhythm games (*Project Sekai*, *Arcaea*, *Cytus II*, *Phigros*, *Lanota*).
* Tap-to-the-beat metronome measuring exact tap timing deltas, calculating mean user sync offset ($\Delta t$) and jitter standard deviation, with direct in-app auditioning.

### 8. Target Acquisition & Micro-Flick Drill
* Dynamic target acquisition reaction training ground:
  * Multiple modes: Speed Flick, Precision Micro-Adjustment, Dynamic Reflex Tracking.
  * Real-time metrics: TTF (Time to First Hit in ms), Miss Rate %, Target Dispersion radius, and overall Esports Grade (S+, S, A, B, C).

### 9. Reaction Benchmark
* Clean visual (and auditory) stimulus reaction time benchmark with randomized false-start penalty guards, computing mean latency, 95th percentile, and standard deviation.

### 10. System Settings Guide
* Interactive hardware optimization checklist guiding competitive gamers through OEM-specific tweaks:
  * Maximum display refresh rate locking
  * Touch sampling boost / game turbo configuration
  * Disabling touch edge-mistouch suppression
  * Background throttle & battery optimization exemptions
  * Low latency Bluetooth codec selection (LDAC / aptX Adaptive / LHDC)

---

## 🎨 UI & Design Philosophy

* **OLED-Optimized Cyber Aesthetics:** Pure Black (`#000000`) background, dark gray containers (`#121212`), Cyber Cyan (`#38BDF8`), Neon Green (`#00FF9D`), Warning Orange (`#FF9500`), and Crimson Red (`#FF2A55`).
* **Bento Spatial Organization:** Interactive segmented accordion structure for fast category switching, remembering user state across navigation.
* **Utilitarian Typography:** Clean Sans-Serif for titles and strict Monospace font for live fluctuating telemetry figures.
* **Orientation:** Locked to `SCREEN_ORIENTATION_USER_LANDSCAPE` for esports phone form-factor ergonomics.

---

## ⚙️ Architecture & Build Instructions

### Prerequisites
* Android Studio (Ladybug / Jellyfish or newer)
* Android SDK 35 / 36
* JDK 21 (Bundled in Android Studio JBR)
* Gradle 8.14.3

### Build via Command Line
```powershell
# Set environment paths
$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"
$env:ANDROID_HOME="C:\Users\<user>\AppData\Local\Android\Sdk"

# Compile and verify Kotlin sources
.\gradlew assembleDebug
```

