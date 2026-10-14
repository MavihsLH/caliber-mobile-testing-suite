package com.gamer.calibration.modules.triggers

import android.view.KeyEvent
import com.gamer.calibration.domain.model.HardwareTriggersSummary
import com.gamer.calibration.domain.model.TriggerState
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
import kotlin.math.max

class TriggerTelemetryEngine(private val scope: CoroutineScope) {

    private val _triggersSummary = MutableStateFlow(HardwareTriggersSummary())
    val triggersSummary: StateFlow<HardwareTriggersSummary> = _triggersSummary.asStateFlow()

    private val pressDownTimes = ConcurrentHashMap<Int, Long>()
    private val clickTimestamps = ConcurrentHashMap<Int, ArrayDeque<Long>>()
    private val totalClicksMap = ConcurrentHashMap<Int, Int>()

    private var peakCps = 0.0
    private var lastActuationLatency = 0L

    private var monitorJob: Job? = null

    init {
        startCpsMonitor()
    }

    private fun startCpsMonitor() {
        monitorJob?.cancel()
        monitorJob = scope.launch(Dispatchers.Default) {
            while (isActive) {
                updateCpsMetrics()
                delay(50) // 20Hz update
            }
        }
    }

    fun handleKeyEvent(event: KeyEvent): Boolean {
        val keyCode = event.keyCode

        val isTargetKey = when (keyCode) {
            KeyEvent.KEYCODE_BUTTON_L1,
            KeyEvent.KEYCODE_BUTTON_R1,
            KeyEvent.KEYCODE_BUTTON_L2,
            KeyEvent.KEYCODE_BUTTON_R2,
            KeyEvent.KEYCODE_VOLUME_UP,
            KeyEvent.KEYCODE_VOLUME_DOWN -> true
            else -> false
        }

        if (!isTargetKey) return false

        val now = System.currentTimeMillis()

        if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
            pressDownTimes[keyCode] = now
            val queue = clickTimestamps.getOrPut(keyCode) { ArrayDeque() }
            synchronized(queue) {
                queue.addLast(now)
            }
            totalClicksMap[keyCode] = (totalClicksMap[keyCode] ?: 0) + 1
            updateSingleTriggerState(keyCode, isPressed = true, durationMs = 0L)
            return true
        } else if (event.action == KeyEvent.ACTION_UP) {
            val downTime = pressDownTimes.remove(keyCode) ?: now
            val latency = now - downTime
            lastActuationLatency = latency
            updateSingleTriggerState(keyCode, isPressed = false, durationMs = latency)
            return true
        }

        return false
    }

    private fun updateSingleTriggerState(keyCode: Int, isPressed: Boolean, durationMs: Long) {
        val current = _triggersSummary.value
        val clicks = totalClicksMap[keyCode] ?: 0

        _triggersSummary.value = when (keyCode) {
            KeyEvent.KEYCODE_BUTTON_L1 -> current.copy(
                l1Trigger = current.l1Trigger.copy(isPressed = isPressed, pressDurationMs = durationMs, totalClicks = clicks),
                lastTriggerLatencyMs = if (!isPressed) durationMs else current.lastTriggerLatencyMs
            )
            KeyEvent.KEYCODE_BUTTON_R1 -> current.copy(
                r1Trigger = current.r1Trigger.copy(isPressed = isPressed, pressDurationMs = durationMs, totalClicks = clicks),
                lastTriggerLatencyMs = if (!isPressed) durationMs else current.lastTriggerLatencyMs
            )
            KeyEvent.KEYCODE_BUTTON_L2 -> current.copy(
                l2Trigger = current.l2Trigger.copy(isPressed = isPressed, pressDurationMs = durationMs, totalClicks = clicks),
                lastTriggerLatencyMs = if (!isPressed) durationMs else current.lastTriggerLatencyMs
            )
            KeyEvent.KEYCODE_BUTTON_R2 -> current.copy(
                r2Trigger = current.r2Trigger.copy(isPressed = isPressed, pressDurationMs = durationMs, totalClicks = clicks),
                lastTriggerLatencyMs = if (!isPressed) durationMs else current.lastTriggerLatencyMs
            )
            KeyEvent.KEYCODE_VOLUME_UP -> current.copy(
                volumeUpTrigger = current.volumeUpTrigger.copy(isPressed = isPressed, pressDurationMs = durationMs, totalClicks = clicks),
                lastTriggerLatencyMs = if (!isPressed) durationMs else current.lastTriggerLatencyMs
            )
            KeyEvent.KEYCODE_VOLUME_DOWN -> current.copy(
                volumeDownTrigger = current.volumeDownTrigger.copy(isPressed = isPressed, pressDurationMs = durationMs, totalClicks = clicks),
                lastTriggerLatencyMs = if (!isPressed) durationMs else current.lastTriggerLatencyMs
            )
            else -> current
        }
    }

    private fun updateCpsMetrics() {
        val now = System.currentTimeMillis()
        var maxCurrentCps = 0.0

        fun calculateCps(keyCode: Int): Double {
            val queue = clickTimestamps[keyCode] ?: return 0.0
            synchronized(queue) {
                while (queue.isNotEmpty() && (now - queue.first()) > 1000L) {
                    queue.removeFirst()
                }
                return queue.size.toDouble()
            }
        }

        val l1Cps = calculateCps(KeyEvent.KEYCODE_BUTTON_L1)
        val r1Cps = calculateCps(KeyEvent.KEYCODE_BUTTON_R1)
        val l2Cps = calculateCps(KeyEvent.KEYCODE_BUTTON_L2)
        val r2Cps = calculateCps(KeyEvent.KEYCODE_BUTTON_R2)
        val vUpCps = calculateCps(KeyEvent.KEYCODE_VOLUME_UP)
        val vDownCps = calculateCps(KeyEvent.KEYCODE_VOLUME_DOWN)

        maxCurrentCps = maxOf(l1Cps, r1Cps, l2Cps, r2Cps, vUpCps, vDownCps)
        peakCps = max(peakCps, maxCurrentCps)

        val current = _triggersSummary.value
        _triggersSummary.value = current.copy(
            l1Trigger = current.l1Trigger.copy(clicksPerSecond = l1Cps),
            r1Trigger = current.r1Trigger.copy(clicksPerSecond = r1Cps),
            l2Trigger = current.l2Trigger.copy(clicksPerSecond = l2Cps),
            r2Trigger = current.r2Trigger.copy(clicksPerSecond = r2Cps),
            volumeUpTrigger = current.volumeUpTrigger.copy(clicksPerSecond = vUpCps),
            volumeDownTrigger = current.volumeDownTrigger.copy(clicksPerSecond = vDownCps),
            peakClicksPerSecond = peakCps
        )
    }

    fun setOverlayActive(active: Boolean) {
        _triggersSummary.value = _triggersSummary.value.copy(isOverlayActive = active)
    }

    fun resetStats() {
        pressDownTimes.clear()
        clickTimestamps.clear()
        totalClicksMap.clear()
        peakCps = 0.0
        lastActuationLatency = 0L
        _triggersSummary.value = HardwareTriggersSummary(isOverlayActive = _triggersSummary.value.isOverlayActive)
    }
}
