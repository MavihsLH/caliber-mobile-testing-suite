package com.gamer.calibration.domain.model

data class DiagnosticIssue(
    val title: String,
    val description: String,
    val severity: IssueSeverity,
    val recommendation: String
)

enum class IssueSeverity {
    OPTIMAL,
    NOTICE,
    WARNING
}

data class DiagnosticReport(
    val touchCompleted: Boolean = false,
    val touchSamplingHz: Double = 0.0,
    val touchMaxPointers: Int = 0,

    val motionCompleted: Boolean = false,
    val motionDriftDegs: Float = 0f,
    val isMotionCalibrated: Boolean = false,

    val audioCompleted: Boolean = false,
    val audioLatencyMs: Double = 0.0,
    val audioDevice: String = "",

    val triggerCompleted: Boolean = false,
    val triggerLatencyMs: Long = 0L,
    val triggerPeakCps: Double = 0.0,

    val controllerCompleted: Boolean = false,
    val controllerCircularityErrorPercent: Double = 0.0,
    val controllerConnected: Boolean = false
) {
    val completedCount: Int
        get() = (if (touchCompleted) 1 else 0) +
                (if (motionCompleted) 1 else 0) +
                (if (audioCompleted) 1 else 0) +
                (if (triggerCompleted) 1 else 0) +
                (if (controllerCompleted) 1 else 0)

    val overallGrade: String
        get() {
            if (completedCount == 0) return "--"
            var score = 100
            if (touchCompleted && touchSamplingHz < 180.0) score -= 15
            if (motionCompleted && motionDriftDegs > 0.8f) score -= 20
            if (audioCompleted && audioLatencyMs > 120.0) score -= 25
            if (controllerCompleted && controllerCircularityErrorPercent > 12.0) score -= 15
            return when {
                score >= 90 -> "A+"
                score >= 80 -> "A"
                score >= 70 -> "B"
                else -> "C"
            }
        }

    fun getIssues(): List<DiagnosticIssue> {
        val list = mutableListOf<DiagnosticIssue>()

        if (touchCompleted) {
            if (touchSamplingHz >= 240.0) {
                list.add(DiagnosticIssue(
                    title = "High-Speed Digitizer",
                    description = "Your screen samples touch at ${touchSamplingHz.toInt()}Hz.",
                    severity = IssueSeverity.OPTIMAL,
                    recommendation = "Optimal for fast swipes and competitive aim tracking."
                ))
            } else if (touchSamplingHz in 60.0..180.0) {
                list.add(DiagnosticIssue(
                    title = "Standard Touch Sampling",
                    description = "Digitizer is operating at ${touchSamplingHz.toInt()}Hz.",
                    severity = IssueSeverity.NOTICE,
                    recommendation = "Enable Gaming Mode or Ultra Touch in phone settings if available."
                ))
            }
        }

        if (motionCompleted) {
            if (motionDriftDegs > 0.5f) {
                list.add(DiagnosticIssue(
                    title = "Gyroscope Zero-Point Drift",
                    description = "Detected ${String.format("%.2f", motionDriftDegs)}°/s residual crosshair movement when resting.",
                    severity = IssueSeverity.WARNING,
                    recommendation = "Place phone on a flat surface and run the Gyroscope Zero-Point calibration."
                ))
            } else {
                list.add(DiagnosticIssue(
                    title = "Stable Motion Sensors",
                    description = "Resting drift is within esports tolerance (< 0.5°/s).",
                    severity = IssueSeverity.OPTIMAL,
                    recommendation = "Gyro aiming in shooter games will remain steady without slow wandering."
                ))
            }
        }

        if (audioCompleted) {
            if (audioLatencyMs > 120.0) {
                list.add(DiagnosticIssue(
                    title = "High Wireless Audio Delay",
                    description = "Acoustic delay measured at ${audioLatencyMs.toInt()}ms.",
                    severity = IssueSeverity.WARNING,
                    recommendation = "Standard Bluetooth codecs introduce gunshot audio lag. Enable Low Latency Game Mode on your earbuds or use a 3.5mm / USB-C wired connection."
                ))
            } else {
                list.add(DiagnosticIssue(
                    title = "Low Audio Delay",
                    description = "Round-trip audio latency measured at ${audioLatencyMs.toInt()}ms.",
                    severity = IssueSeverity.OPTIMAL,
                    recommendation = "Sound effects and footsteps will be synchronized with on-screen action."
                ))
            }
        }

        if (controllerCompleted && controllerConnected) {
            if (controllerCircularityErrorPercent > 10.0) {
                list.add(DiagnosticIssue(
                    title = "Stick Outer Boundary Distortion",
                    description = "Stick circularity error is ${String.format("%.1f", controllerCircularityErrorPercent)}%.",
                    severity = IssueSeverity.NOTICE,
                    recommendation = "Outer corners show square clipping. Typical of potentiometer joysticks."
                ))
            } else {
                list.add(DiagnosticIssue(
                    title = "High Precision Joysticks",
                    description = "Stick circularity error is under 5% (${String.format("%.1f", controllerCircularityErrorPercent)}%).",
                    severity = IssueSeverity.OPTIMAL,
                    recommendation = "Smooth circular gating detected. Characteristic of Hall Effect or TMR sensors."
                ))
            }
        }

        return list
    }
}
