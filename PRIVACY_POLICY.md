# Privacy Policy for Caliber

**Last Updated:** October 15, 2026

**Caliber** ("we", "our", or "the app") is an open-source, on-device mobile hardware diagnostics and calibration suite developed by an independent developer. We believe privacy is a fundamental right, especially for competitive gaming and benchmarking utilities.

This Privacy Policy explains how Caliber handles your information and device data.

---

## 1. Core Principle: 100% On-Device Processing

Caliber is engineered with a **local-first, privacy-by-design architecture**:
* All hardware diagnostics (touch digitizer sampling, gyroscope drift calculation, gamepad circularity analysis, and audio latency pings) execute **entirely on your device**.
* We do **not** collect, store, transmit, or sell your hardware telemetry, gameplay habits, device specifications, or benchmark scores.
* There are **no account registrations**, logins, passwords, or cloud sync requirements.

---

## 2. Device Permissions & How They Are Used

Caliber requests only the minimum permissions strictly necessary to interrogate hardware sensors and display optional diagnostic overlays:

| Permission | Technical Purpose | Data Storage & Transmission |
| :--- | :--- | :--- |
| **`RECORD_AUDIO`** | Required solely for the **Audio Latency Lab**. Synthesizes a 15ms transient sound chirp and measures sample arrival via the microphone at 48 kHz to determine acoustic latency. | **Zero recording or transmission.** Audio buffers are processed in volatile RAM in real-time and immediately discarded. No voice or ambient audio is ever saved, analyzed, or sent to any server. |
| **`HIGH_SAMPLING_RATE_SENSORS`** | Required to read the IMU gyroscope and accelerometer at high refresh frequencies (>200Hz–400Hz+) for zero-point drift calibration. | Processed locally in real-time. Never logged off-device. |
| **`SYSTEM_ALERT_WINDOW`** | Used optionally if you enable the **Floating In-Game HUD** to display a draggable telemetry widget (FPS, touch Hz, controller polling) over active games. | Display only. No screen recording or capture occurs. |
| **`VIBRATE`** | Tests gamepad and device haptic motors. | Local hardware actuation only. |
| **`BLUETOOTH_CONNECT`** | Identifies connected audio routing (e.g. 3.5mm DAC vs Bluetooth SBC/AAC/aptX) to categorize audio transit delay. | Read-only hardware interrogation. Never shared. |

---

## 3. Third-Party Services & Advertising

To support ongoing development and hardware testing gear while keeping the app 100% free with no locked features, Caliber integrates **Google AdMob** to display a single, non-intrusive native ad card on the homescreen.

* **Google AdMob:** Google Mobile Ads SDK may collect and process pseudonymous identifiers, such as the Android Advertising ID, IP address, and general diagnostic signals to serve and personalize ads.
* For more information on how Google uses data from apps using its services, please visit:  
  [Google Privacy & Terms](https://policies.google.com/technologies/partner-sites) and [Google Privacy Policy](https://policies.google.com/privacy).

### Your Control Over Ads (Honor System)
Caliber provides complete transparency and control over advertising:
* **Hide Ads for Free:** You can hide ads at any time for free directly from the ad card or the in-app Settings menu.
* **Support the Developer:** You may optionally donate a one-time tip ($1.99) via Google Play to permanently remove ads forever across the app.

---

## 4. Children's Privacy
Caliber does not target children under the age of 13. We do not knowingly collect personal information from children.

---

## 5. Security & Open Source
Because Caliber processes all sensor data locally, your hardware diagnostics never leave your device. The application's source code is publicly accessible and transparent on GitHub:  
[https://github.com/MavihsLH/caliber-mobile-testing-suite](https://github.com/MavihsLH/caliber-mobile-testing-suite)

---

## 6. Changes to This Privacy Policy
If we update this Privacy Policy, the revised version will be posted in this repository with an updated "Last Updated" date.

---

## 7. Contact Us
If you have any questions or concerns regarding this Privacy Policy or Caliber's data practices, please open an issue on our GitHub repository:  
[https://github.com/MavihsLH/caliber-mobile-testing-suite/issues](https://github.com/MavihsLH/caliber-mobile-testing-suite/issues)
