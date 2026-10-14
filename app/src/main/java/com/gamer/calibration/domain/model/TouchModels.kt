package com.gamer.calibration.domain.model

import androidx.compose.ui.geometry.Offset

data class HistoricalTouchPoint(
    val x: Float,
    val y: Float,
    val eventTimeNano: Long,
    val pressure: Float,
    val size: Float,
    val pointerId: Int,
    val isHistorical: Boolean
)

data class ActivePointer(
    val id: Int,
    val x: Float,
    val y: Float,
    val pressure: Float,
    val size: Float,
    val touchMajor: Float = 0f,
    val touchMinor: Float = 0f,
    val trail: List<Offset> = emptyList(),
    val historicalTrail: List<Offset> = emptyList()
)

data class TouchTelemetrySummary(
    val instantaneousHz: Double = 0.0,
    val peakHz: Double = 0.0,
    val averageHz: Double = 0.0,
    val jitterMs: Double = 0.0,
    val activePointerCount: Int = 0,
    val maxConcurrentPointers: Int = 0,
    val historicalPacketsPerEvent: Double = 0.0,
    val totalSamplesCaptured: Long = 0L,
    val hzHistory: List<Float> = emptyList()
)
