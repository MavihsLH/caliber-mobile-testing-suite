package com.gamer.calibration.modules.touch

import android.os.Build
import android.view.MotionEvent
import androidx.compose.ui.geometry.Offset
import com.gamer.calibration.domain.model.ActivePointer
import com.gamer.calibration.domain.model.HistoricalTouchPoint
import com.gamer.calibration.domain.model.TouchTelemetrySummary
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
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

class TouchTelemetryEngine(private val scope: CoroutineScope) {

    private val _telemetrySummary = MutableStateFlow(TouchTelemetrySummary())
    val telemetrySummary: StateFlow<TouchTelemetrySummary> = _telemetrySummary.asStateFlow()

    // High frequency thread-safe structures for raw drawing (read by Canvas withFrameNanos)
    val activePointers = ConcurrentHashMap<Int, ActivePointer>()

    // Ring buffer of recent delta times (in nanoseconds or milliseconds) for Jitter & Hz
    private val intervalHistory = ArrayDeque<Double>(120)
    private val hzHistory = ArrayDeque<Float>(60)

    private var lastEventTimeNano: Long = 0L
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
                delay(33) // ~30Hz update rate to Compose State, completely decoupling from 500Hz MotionEvents
            }
        }
    }

    /**
     * Intercept raw MotionEvent. Extracts historical coordinates and calculates hardware digitizer telemetry.
     */
    fun processMotionEvent(event: MotionEvent) {
        val action = event.actionMasked
        val historySize = event.historySize
        val pointerCount = event.pointerCount
        maxConcurrentPointersRecorded = max(maxConcurrentPointersRecorded, pointerCount)

        totalEvents++
        totalHistoricalPackets += historySize

        when (action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
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
                // 1. Process Historical Coordinates
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

                // 2. Process Current Coordinates
                val curTimeNano = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    event.eventTime * 1_000_000L
                } else {
                    event.eventTime * 1_000_000L
                }
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
            }
        }
    }

    private fun recordSampleInterval(sampleTimeNano: Long) {
        if (lastEventTimeNano > 0L) {
            val deltaNano = sampleTimeNano - lastEventTimeNano
            // Valid sample interval between 0.1ms and 50ms
            if (deltaNano in 100_000L..50_000_000L) {
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

        if (intervals.isEmpty()) {
            _telemetrySummary.value = _telemetrySummary.value.copy(
                instantaneousHz = 0.0,
                activePointerCount = activePointers.size
            )
            return
        }

        val avgIntervalMs = intervals.average()
        val currentHz = if (avgIntervalMs > 0.0) 1000.0 / avgIntervalMs else 0.0
        val instIntervalMs = intervals.lastOrNull() ?: avgIntervalMs
        val instHz = if (instIntervalMs > 0.0) 1000.0 / instIntervalMs else 0.0

        if (currentHz > peakHzRecorded && currentHz < 1500.0) {
            peakHzRecorded = currentHz
        }

        // Standard deviation (Jitter)
        val variance = intervals.map { (it - avgIntervalMs).pow(2.0) }.average()
        val jitterMs = sqrt(variance)

        val packetsPerEvent = if (totalEvents > 0) {
            totalHistoricalPackets.toDouble() / totalEvents.toDouble()
        } else 0.0

        synchronized(hzHistory) {
            hzHistory.addLast(currentHz.toFloat())
            if (hzHistory.size > 60) {
                hzHistory.removeFirst()
            }
        }

        _telemetrySummary.value = TouchTelemetrySummary(
            instantaneousHz = instHz,
            peakHz = peakHzRecorded,
            averageHz = currentHz,
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
        _telemetrySummary.value = TouchTelemetrySummary()
    }
}
