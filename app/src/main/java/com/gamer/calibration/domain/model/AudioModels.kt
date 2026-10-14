package com.gamer.calibration.domain.model

data class AudioLatencySummary(
    val lastLatencyMs: Double = 0.0,
    val minLatencyMs: Double = 0.0,
    val maxLatencyMs: Double = 0.0,
    val averageLatencyMs: Double = 0.0,
    val jitterMs: Double = 0.0,
    val audioRoutingDevice: String = "Built-in Speaker",
    val isBluetooth: Boolean = false,
    val sampleRateHz: Int = 48000,
    val testInProgress: Boolean = false,
    val totalPingsConducted: Int = 0,
    val signalToNoiseRatioDb: Double = 0.0,
    val correlationConfidence: Double = 0.0,
    val latencyHistory: List<Float> = emptyList()
)
