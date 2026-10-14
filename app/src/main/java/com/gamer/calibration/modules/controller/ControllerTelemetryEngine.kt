package com.gamer.calibration.modules.controller

import android.content.Context
import android.hardware.input.InputManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.InputDevice
import android.view.MotionEvent
import com.gamer.calibration.domain.model.ControllerPrecisionSummary
import com.gamer.calibration.domain.model.GamepadDeviceInfo
import com.gamer.calibration.domain.model.StickTelemetry
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
import java.util.concurrent.ConcurrentLinkedDeque
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.sqrt

class ControllerTelemetryEngine(
    private val context: Context,
    private val scope: CoroutineScope
) : InputManager.InputDeviceListener {

    private val inputManager = context.getSystemService(Context.INPUT_SERVICE) as? InputManager
    private val settingsPrefs = context.getSharedPreferences("apex_calibrate_prefs", Context.MODE_PRIVATE)

    private val _telemetrySummary = MutableStateFlow(ControllerPrecisionSummary())
    val telemetrySummary: StateFlow<ControllerPrecisionSummary> = _telemetrySummary.asStateFlow()

    val leftOuterBoundaryBins = FloatArray(360) { 0f }
    val rightOuterBoundaryBins = FloatArray(360) { 0f }

    val leftStickTrail = ConcurrentLinkedDeque<Pair<Float, Float>>()
    val rightStickTrail = ConcurrentLinkedDeque<Pair<Float, Float>>()

    private var leftSampleCount = 0L
    private var rightSampleCount = 0L
    private var lastRateCalcTime = System.currentTimeMillis()
    private var leftStickHz = 0.0
    private var rightStickHz = 0.0

    private var pollJob: Job? = null

    init {
        scanConnectedGamepads()
        inputManager?.registerInputDeviceListener(this, null)
        startTelemetryLoop()
    }

    override fun onInputDeviceAdded(deviceId: Int) = scanConnectedGamepads()
    override fun onInputDeviceRemoved(deviceId: Int) = scanConnectedGamepads()
    override fun onInputDeviceChanged(deviceId: Int) = scanConnectedGamepads()

    private fun startTelemetryLoop() {
        pollJob?.cancel()
        pollJob = scope.launch(Dispatchers.Default) {
            while (isActive) {
                val now = System.currentTimeMillis()
                val elapsed = now - lastRateCalcTime
                if (elapsed >= 300) {
                    leftStickHz = (leftSampleCount * 1000.0) / elapsed
                    rightStickHz = (rightSampleCount * 1000.0) / elapsed
                    leftSampleCount = 0L
                    rightSampleCount = 0L
                    lastRateCalcTime = now

                    FloatingTelemetryService.sharedControllerHz = max(leftStickHz, rightStickHz)
                }

                publishSummary()
                delay(33)
            }
        }
    }

    fun scanConnectedGamepads() {
        val deviceIds = InputDevice.getDeviceIds()
        var matchedDevice: InputDevice? = null

        for (id in deviceIds) {
            val dev = InputDevice.getDevice(id) ?: continue
            val sources = dev.sources
            val isGamepad = (sources and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD ||
                    (sources and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK
            if (isGamepad && !dev.isVirtual) {
                matchedDevice = dev
                break
            }
        }

        if (matchedDevice != null) {
            val axes = mutableListOf<String>()
            for (range in matchedDevice.motionRanges) {
                axes.add(MotionEvent.axisToString(range.axis))
            }

            val hasVibe = matchedDevice.vibrator.hasVibrator()
            val rawName = matchedDevice.name ?: "Generic Gamepad"
            val guessed = com.gamer.calibration.domain.model.GamepadBrandGuesser.guess(
                name = rawName,
                vendorId = matchedDevice.vendorId,
                productId = matchedDevice.productId
            )

            val info = GamepadDeviceInfo(
                id = matchedDevice.id,
                name = rawName,
                vendorId = matchedDevice.vendorId,
                productId = matchedDevice.productId,
                isConnected = true,
                hasVibrator = hasVibe,
                guessedBrand = guessed,
                axesSupported = axes
            )
            _telemetrySummary.value = _telemetrySummary.value.copy(device = info)
        } else {
            FloatingTelemetryService.sharedControllerHz = 0.0
            _telemetrySummary.value = _telemetrySummary.value.copy(
                device = GamepadDeviceInfo()
            )
        }
    }

    fun handleGenericMotionEvent(event: MotionEvent): Boolean {
        if ((event.source and InputDevice.SOURCE_JOYSTICK) == 0 &&
            (event.source and InputDevice.SOURCE_GAMEPAD) == 0
        ) {
            return false
        }

        val lx = event.getAxisValue(MotionEvent.AXIS_X)
        val ly = event.getAxisValue(MotionEvent.AXIS_Y)
        processLeftStick(lx, ly)

        val rx = event.getAxisValue(MotionEvent.AXIS_Z)
        val ry = event.getAxisValue(MotionEvent.AXIS_RZ)
        processRightStick(rx, ry)

        return true
    }

    private fun processLeftStick(x: Float, y: Float) {
        leftSampleCount++
        leftStickTrail.addLast(Pair(x, y))
        while (leftStickTrail.size > 150) {
            leftStickTrail.pollFirst()
        }

        val r = sqrt(x * x + y * y)
        val angleRad = atan2(y, x)
        var angleDeg = (angleRad * 180f / PI.toFloat())
        if (angleDeg < 0) angleDeg += 360f

        val binIdx = angleDeg.toInt().coerceIn(0, 359)
        if (r > 0.5f) {
            leftOuterBoundaryBins[binIdx] = max(leftOuterBoundaryBins[binIdx], r)
        }
    }

    private fun processRightStick(x: Float, y: Float) {
        rightSampleCount++
        rightStickTrail.addLast(Pair(x, y))
        while (rightStickTrail.size > 150) {
            rightStickTrail.pollFirst()
        }

        val r = sqrt(x * x + y * y)
        val angleRad = atan2(y, x)
        var angleDeg = (angleRad * 180f / PI.toFloat())
        if (angleDeg < 0) angleDeg += 360f

        val binIdx = angleDeg.toInt().coerceIn(0, 359)
        if (r > 0.5f) {
            rightOuterBoundaryBins[binIdx] = max(rightOuterBoundaryBins[binIdx], r)
        }
    }

    private fun computeCircularityError(bins: FloatArray): Double {
        var filledCount = 0
        var totalDeviation = 0.0
        for (r in bins) {
            if (r > 0.4f) {
                filledCount++
                totalDeviation += abs(r - 1.0f)
            }
        }
        return if (filledCount > 10) {
            (totalDeviation / filledCount.toDouble()) * 100.0
        } else 0.0
    }

    private fun publishSummary() {
        val lastL = leftStickTrail.peekLast() ?: Pair(0f, 0f)
        val lastR = rightStickTrail.peekLast() ?: Pair(0f, 0f)

        val leftRadius = sqrt(lastL.first * lastL.first + lastL.second * lastL.second)
        val rightRadius = sqrt(lastR.first * lastR.first + lastR.second * lastR.second)

        val leftAngle = (atan2(lastL.second, lastL.first) * 180f / PI.toFloat() + 360f) % 360f
        val rightAngle = (atan2(lastR.second, lastR.first) * 180f / PI.toFloat() + 360f) % 360f

        val leftCircError = computeCircularityError(leftOuterBoundaryBins)
        val rightCircError = computeCircularityError(rightOuterBoundaryBins)

        val innerDeadL = if (leftRadius < 0.2f) leftRadius * 100f else 0f
        val innerDeadR = if (rightRadius < 0.2f) rightRadius * 100f else 0f

        _telemetrySummary.value = _telemetrySummary.value.copy(
            leftStick = StickTelemetry(
                rawX = lastL.first,
                rawY = lastL.second,
                radius = leftRadius,
                angleDeg = leftAngle,
                innerDeadzonePercent = innerDeadL,
                outerCircularityErrorPercent = leftCircError,
                pollingRateHz = leftStickHz
            ),
            rightStick = StickTelemetry(
                rawX = lastR.first,
                rawY = lastR.second,
                radius = rightRadius,
                angleDeg = rightAngle,
                innerDeadzonePercent = innerDeadR,
                outerCircularityErrorPercent = rightCircError,
                pollingRateHz = rightStickHz
            )
        )
    }

    fun triggerRumble(leftAmplitude: Float, rightAmplitude: Float, durationMs: Long = 300L) {
        if (!settingsPrefs.getBoolean("haptics_enabled", true)) return

        _telemetrySummary.value = _telemetrySummary.value.copy(
            rumbleLeftMotor = leftAmplitude,
            rumbleRightMotor = rightAmplitude
        )

        val lowAmp = (leftAmplitude.coerceIn(0f, 1f) * 255).toInt().coerceIn(1, 255)
        val highAmp = (rightAmplitude.coerceIn(0f, 1f) * 255).toInt().coerceIn(1, 255)
        val halfDur = (durationMs / 2L).coerceAtLeast(50L)

        // Two-stage low/high impulse waveform so left & right sliders produce distinct tactile profiles
        val timings = longArrayOf(halfDur, halfDur)
        val amplitudes = intArrayOf(lowAmp, highAmp)

        val devId = _telemetrySummary.value.device.id
        if (devId != -1) {
            val dev = InputDevice.getDevice(devId)
            val vibe = dev?.vibrator
            if (vibe != null && vibe.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibe.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibe.vibrate(durationMs)
                }
                return
            }
        }

        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(durationMs)
            }
        }
    }

    fun resetCircularity() {
        for (i in 0 until 360) {
            leftOuterBoundaryBins[i] = 0f
            rightOuterBoundaryBins[i] = 0f
        }
        leftStickTrail.clear()
        rightStickTrail.clear()
        publishSummary()
    }
}
