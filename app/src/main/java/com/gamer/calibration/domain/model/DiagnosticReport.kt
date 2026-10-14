package com.gamer.calibration.domain.model

import java.util.Locale

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

            if (touchCompleted) {
                if (touchSamplingHz < 120.0) score -= 20
                else if (touchSamplingHz < 180.0) score -= 10
                if (touchMaxPointers in 1..3) score -= 10
            }
            if (motionCompleted) {
                if (motionDriftDegs > 1.2f) score -= 25
                else if (motionDriftDegs > 0.5f) score -= 15
            }
            if (audioCompleted) {
                if (audioLatencyMs > 150.0) score -= 25
                else if (audioLatencyMs > 90.0) score -= 12
            }
            if (triggerCompleted) {
                if (triggerLatencyMs > 120L) score -= 10
            }
            if (controllerCompleted && controllerConnected) {
                if (controllerCircularityErrorPercent > 12.0) score -= 15
                else if (controllerCircularityErrorPercent > 7.0) score -= 5
            }

            // Cap maximum grade if fewer than 3 core hardware tests are completed
            if (completedCount < 3) {
                return when {
                    score >= 85 -> "B+"
                    score >= 70 -> "B"
                    else -> "C"
                }
            }

            return when {
                score >= 92 -> "A+"
                score >= 82 -> "A"
                score >= 70 -> "B"
                else -> "C"
            }
        }

    fun getIssues(): List<DiagnosticIssue> {
        val list = mutableListOf<DiagnosticIssue>()

        if (touchCompleted) {
            when {
                touchSamplingHz >= 240.0 -> {
                    list.add(
                        DiagnosticIssue(
                            title = "High-Speed Digitizer",
                            description = "Your screen samples touch at ${touchSamplingHz.toInt()}Hz (${touchMaxPointers} max fingers).",
                            severity = IssueSeverity.OPTIMAL,
                            recommendation = "Optimal for fast swipes and multi-finger claw grip."
                        )
                    )
                }
                touchSamplingHz >= 120.0 -> {
                    list.add(
                        DiagnosticIssue(
                            title = "Standard Touch Sampling",
                            description = "Digitizer is operating at ${touchSamplingHz.toInt()}Hz.",
                            severity = IssueSeverity.NOTICE,
                            recommendation = "Enable Gaming Mode or Ultra Touch Sampling in system settings if supported."
                        )
                    )
                }
                else -> {
                    list.add(
                        DiagnosticIssue(
                            title = "Low Touch Sampling Rate",
                            description = "Digitizer polled at ${touchSamplingHz.toInt()}Hz.",
                            severity = IssueSeverity.WARNING,
                            recommendation = "Disable battery saver and three-finger screenshot gestures to reduce input buffering."
                        )
                    )
                }
            }
        }

        if (motionCompleted) {
            if (motionDriftDegs > 0.5f) {
                list.add(
                    DiagnosticIssue(
                        title = "Gyroscope Zero-Point Drift",
                        description = "Detected ${String.format(Locale.US, "%.2f", motionDriftDegs)}°/s residual crosshair movement when resting.",
                        severity = IssueSeverity.WARNING,
                        recommendation = "Place phone on a flat surface and run the Gyroscope Zero-Point calibration."
                    )
                )
            } else {
                list.add(
                    DiagnosticIssue(
                        title = "Stable Motion Sensors",
                        description = "Resting drift is within esports tolerance (${String.format(Locale.US, "%.2f", motionDriftDegs)}°/s).",
                        severity = IssueSeverity.OPTIMAL,
                        recommendation = "Gyro aiming in shooter games will remain steady without slow wandering."
                    )
                )
            }
        }

        if (audioCompleted) {
            if (audioLatencyMs > 120.0) {
                list.add(
                    DiagnosticIssue(
                        title = "High Wireless Audio Delay",
                        description = "Acoustic delay measured at ${audioLatencyMs.toInt()}ms on $audioDevice.",
                        severity = IssueSeverity.WARNING,
                        recommendation = "Standard Bluetooth codecs introduce gunshot audio lag. Enable Low Latency Game Mode on your earbuds or use a USB-C / 3.5mm wired DAC."
                    )
                )
            } else {
                list.add(
                    DiagnosticIssue(
                        title = "Low Audio Delay",
                        description = "Audio latency measured at ${audioLatencyMs.toInt()}ms on $audioDevice.",
                        severity = IssueSeverity.OPTIMAL,
                        recommendation = "Sound effects and footsteps are tightly synchronized with on-screen action."
                    )
                )
            }
        }

        if (controllerCompleted && controllerConnected) {
            when {
                controllerCircularityErrorPercent > 10.0 -> {
                    list.add(
                        DiagnosticIssue(
                            title = "Stick Outer Boundary Distortion",
                            description = "Stick circularity error is ${String.format(Locale.US, "%.1f", controllerCircularityErrorPercent)}%.",
                            severity = IssueSeverity.NOTICE,
                            recommendation = "Outer corners show axial clipping typical of standard potentiometer joysticks."
                        )
                    )
                }
                controllerCircularityErrorPercent > 5.0 -> {
                    list.add(
                        DiagnosticIssue(
                            title = "Standard Analog Stick Gating",
                            description = "Stick circularity error is ${String.format(Locale.US, "%.1f", controllerCircularityErrorPercent)}% (5%–10% range).",
                            severity = IssueSeverity.OPTIMAL,
                            recommendation = "Good rotational accuracy for general competitive play."
                        )
                    )
                }
                else -> {
                    list.add(
                        DiagnosticIssue(
                            title = "High Precision Joysticks",
                            description = "Stick circularity error is under 5% (${String.format(Locale.US, "%.1f", controllerCircularityErrorPercent)}%).",
                            severity = IssueSeverity.OPTIMAL,
                            recommendation = "Smooth circular gating detected. Characteristic of Hall Effect or TMR sensors."
                        )
                    )
                }
            }
        }

        return list
    }
}
