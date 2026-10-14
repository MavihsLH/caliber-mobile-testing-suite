package com.gamer.calibration.domain.model

data class SensorSample(
    val x: Float,
    val y: Float,
    val z: Float,
    val timestampNano: Long
)

data class MotionTelemetrySummary(
    val gyroX: Float = 0f,
    val gyroY: Float = 0f,
    val gyroZ: Float = 0f,
    val accelX: Float = 0f,
    val accelY: Float = 0f,
    val accelZ: Float = 0f,
    val gyroHz: Double = 0.0,
    val accelHz: Double = 0.0,
    val driftMagnitudeRads: Float = 0f,
    val driftMagnitudeDegs: Float = 0f,
    val noiseVariance: Float = 0f,
    val isCalibrated: Boolean = false,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val offsetZ: Float = 0f
)
