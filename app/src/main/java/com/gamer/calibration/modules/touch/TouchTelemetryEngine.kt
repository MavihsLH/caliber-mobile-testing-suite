package com.gamer.calibration.modules.touch

import android.view.MotionEvent
import androidx.compose.ui.geometry.Offset
import com.gamer.calibration.domain.model.ActivePointer
import com.gamer.calibration.domain.model.TouchTelemetrySummary
import com.gamer.calibration.modules.triggers.FloatingTelemetryService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt

class TouchTelemetryEngine(private val scope: CoroutineScope) {

    private val _telemetrySummary = MutableStateFlow(TouchTelemetrySummary())
    val telemetrySummary: StateFlow<TouchTelemetrySummary> = _telemetrySummary.asStateFlow()

    val activePointers = ConcurrentHashMap<Int, ActivePointer>()

    private val intervalHistory = ArrayDeque<Double>(120)
    private val hzHistory = ArrayDeque<Float>(60)

    private var lastEventTimeNano: Long = 0L
    private var lastTouchEventWallMs: Long = 0L
    private var totalHistoricalPackets: Long = 0L
    private var totalEvents: Long = 0L
    private val totalSamplesCaptured = AtomicLong(0L)
    private var peakHzRecorded: Double = 0.0
    private var maxConcurrentPointersRecorded: Int = 0

    private var summaryPublishJob: Job? = null

    init {
        startSummaryPublisher()
    }

    private fun startSummaryPublisher() {
        summaryPublishJob?.cancel()
        summaryPublishJob = scope.launch(Dispatchers.Default) {
            while (isActive) {
                publishTelemetrySnapshot()
                delay(33)
            }
        }
    }

    fun processMotionEvent(event: MotionEvent) {
        val action = event.actionMasked
        val historySize = event.historySize
        val pointerCount = event.pointerCount
        maxConcurrentPointersRecorded = max(maxConcurrentPointersRecorded, pointerCount)
        lastTouchEventWallMs = System.currentTimeMillis()

        totalEvents++
        totalHistoricalPackets += historySize

        when (action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                if (action == MotionEvent.ACTION_DOWN) {
                    lastEventTimeNano = 0L
                }
                val actionIndex = event.actionIndex
                val pointerId = event.getPointerId(actionIndex)
                val x = event.getX(actionIndex)
                val y = event.getY(actionIndex)
                val pressure = event.getPressure(actionIndex)
                val size = event.getSize(actionIndex)

                activePointers[pointerId] = ActivePointer(
                    id = pointerId,
                    x = x,
                    y = y,
                    pressure = pressure,
                    size = size,
                    trail = listOf(Offset(x, y)),
                    historicalTrail = emptyList()
                )
            }

            MotionEvent.ACTION_MOVE -> {
                for (h in 0 until historySize) {
                    val histTimeNano = event.getHistoricalEventTime(h) * 1_000_000L
                    recordSampleInterval(histTimeNano)
                    totalSamplesCaptured.incrementAndGet()

                    for (p in 0 until pointerCount) {
                        val pid = event.getPointerId(p)
                        val hx = event.getHistoricalX(p, h)
                        val hy = event.getHistoricalY(p, h)
                        val hPressure = event.getHistoricalPressure(p, h)
                        val hSize = event.getHistoricalSize(p, h)

                        val current = activePointers[pid]
                        if (current != null) {
                            val newHistTrail = (current.historicalTrail + Offset(hx, hy)).takeLast(250)
                            activePointers[pid] = current.copy(
                                x = hx,
                                y = hy,
                                pressure = hPressure,
                                size = hSize,
                                historicalTrail = newHistTrail
                            )
                        }
                    }
                }

                val curTimeNano = event.eventTime * 1_000_000L
                recordSampleInterval(curTimeNano)
                totalSamplesCaptured.incrementAndGet()

                for (p in 0 until pointerCount) {
                    val pid = event.getPointerId(p)
                    val x = event.getX(p)
                    val y = event.getY(p)
                    val pressure = event.getPressure(p)
                    val size = event.getSize(p)
                    val touchMajor = event.getTouchMajor(p)
                    val touchMinor = event.getTouchMinor(p)

                    val current = activePointers[pid]
                    if (current != null) {
                        val newTrail = (current.trail + Offset(x, y)).takeLast(100)
                        activePointers[pid] = current.copy(
                            x = x,
                            y = y,
                            pressure = pressure,
                            size = size,
                            touchMajor = touchMajor,
                            touchMinor = touchMinor,
                            trail = newTrail
                        )
                    }
                }
            }

            MotionEvent.ACTION_POINTER_UP -> {
                val actionIndex = event.actionIndex
                val pointerId = event.getPointerId(actionIndex)
                activePointers.remove(pointerId)
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                activePointers.clear()
                lastEventTimeNano = 0L
            }
        }
    }

    private fun recordSampleInterval(sampleTimeNano: Long) {
        if (lastEventTimeNano > 0L) {
            val deltaNano = sampleTimeNano - lastEventTimeNano
            // Filter out simultaneous multi-pointer batch timestamps (< 0.25ms) and pauses (> 50ms)
            if (deltaNano in 250_000L..50_000_000L) {
                val deltaMs = deltaNano / 1_000_000.0
                synchronized(intervalHistory) {
                    intervalHistory.addLast(deltaMs)
                    if (intervalHistory.size > 120) {
                        intervalHistory.removeFirst()
                    }
                }
            }
        }
        lastEventTimeNano = sampleTimeNano
    }

    private fun publishTelemetrySnapshot() {
        val intervals: List<Double>
        synchronized(intervalHistory) {
            intervals = intervalHistory.toList()
        }

        val isActivelyMoving = activePointers.isNotEmpty() &&
                (System.currentTimeMillis() - lastTouchEventWallMs) < 180L

        if (intervals.isEmpty()) {
            FloatingTelemetryService.sharedTouchHz = 0.0
            _telemetrySummary.value = _telemetrySummary.value.copy(
                instantaneousHz = 0.0,
                activePointerCount = activePointers.size
            )
            return
        }

        val avgIntervalMs = intervals.average()
        val rollingAvgHz = if (avgIntervalMs > 0.0) (1000.0 / avgIntervalMs).coerceAtMost(1200.0) else 0.0

        // Use a 12-sample short window for live RATE so single-frame OS batching doesn't cause false spikes
        val recentSlice = intervals.takeLast(12)
        val recentAvgMs = recentSlice.average()
        val liveRateHz = if (isActivelyMoving && recentAvgMs > 0.0) {
            (1000.0 / recentAvgMs).coerceAtMost(1200.0)
        } else {
            0.0
        }

        // Guarantee PEAK is always >= any displayed live RATE
        val candidatePeak = max(rollingAvgHz, liveRateHz)
        if (candidatePeak > peakHzRecorded && candidatePeak <= 1200.0) {
            peakHzRecorded = candidatePeak
        }

        FloatingTelemetryService.sharedTouchHz = liveRateHz

        val variance = intervals.map { (it - avgIntervalMs).pow(2.0) }.average()
        val jitterMs = sqrt(variance)

        val packetsPerEvent = if (totalEvents > 0) {
            totalHistoricalPackets.toDouble() / totalEvents.toDouble()
        } else 0.0

        if (isActivelyMoving) {
            synchronized(hzHistory) {
                hzHistory.addLast(liveRateHz.toFloat())
                if (hzHistory.size > 60) {
                    hzHistory.removeFirst()
                }
            }
        }

        _telemetrySummary.value = TouchTelemetrySummary(
            instantaneousHz = liveRateHz,
            peakHz = peakHzRecorded,
            averageHz = rollingAvgHz,
            jitterMs = jitterMs,
            activePointerCount = activePointers.size,
            maxConcurrentPointers = maxConcurrentPointersRecorded,
            historicalPacketsPerEvent = packetsPerEvent,
            totalSamplesCaptured = totalSamplesCaptured.get(),
            hzHistory = synchronized(hzHistory) { hzHistory.toList() }
        )
    }

    fun resetStats() {
        synchronized(intervalHistory) { intervalHistory.clear() }
        synchronized(hzHistory) { hzHistory.clear() }
        peakHzRecorded = 0.0
        totalEvents = 0L
        totalHistoricalPackets = 0L
        totalSamplesCaptured.set(0L)
        maxConcurrentPointersRecorded = 0
        lastEventTimeNano = 0L
        activePointers.clear()
        FloatingTelemetryService.sharedTouchHz = 0.0
        _telemetrySummary.value = TouchTelemetrySummary()
    }
}
