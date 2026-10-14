package com.gamer.calibration.modules.motion

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.gamer.calibration.domain.model.MotionTelemetrySummary
import com.gamer.calibration.domain.model.SensorSample
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
import kotlin.math.pow
import kotlin.math.sqrt

class MotionTelemetryEngine(
    context: Context,
    private val scope: CoroutineScope
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val gyroSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val accelSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val _telemetrySummary = MutableStateFlow(MotionTelemetrySummary())
    val telemetrySummary: StateFlow<MotionTelemetrySummary> = _telemetrySummary.asStateFlow()

    // High speed lock-free waveform buffers (up to 300 samples for live multi-channel graphs)
    val gyroHistoryX = ConcurrentLinkedDeque<Float>()
    val gyroHistoryY = ConcurrentLinkedDeque<Float>()
    val gyroHistoryZ = ConcurrentLinkedDeque<Float>()

    val accelHistoryX = ConcurrentLinkedDeque<Float>()
    val accelHistoryY = ConcurrentLinkedDeque<Float>()
    val accelHistoryZ = ConcurrentLinkedDeque<Float>()

    private var gyroSampleCount = 0L
    private var accelSampleCount = 0L
    private var lastGyroCalcTime = System.currentTimeMillis()
    private var lastAccelCalcTime = System.currentTimeMillis()
    private var currentGyroHz = 0.0
    private var currentAccelHz = 0.0

    // Calibration Offsets (Zero-point drift bias tare)
    private var offsetX = 0f
    private var offsetY = 0f
    private var offsetZ = 0f
    private var isCalibrated = false

    // Rolling window for variance / noise floor calculation
    private val recentGyroMagnitudes = ArrayDeque<Float>(100)

    private var latestGyro = floatArrayOf(0f, 0f, 0f)
    private var latestAccel = floatArrayOf(0f, 0f, 0f)

    private var summaryJob: Job? = null
    private var isRunning = false

    fun start() {
        if (isRunning) return
        isRunning = true

        gyroSensor?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_FASTEST)
        }
        accelSensor?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_FASTEST)
        }

        startTelemetryPublisher()
    }

    fun stop() {
        if (!isRunning) return
        isRunning = false
        sensorManager?.unregisterListener(this)
        summaryJob?.cancel()
    }

    private fun startTelemetryPublisher() {
        summaryJob?.cancel()
        summaryJob = scope.launch(Dispatchers.Default) {
            while (isActive) {
                val now = System.currentTimeMillis()
                val gyroElapsed = now - lastGyroCalcTime
                if (gyroElapsed >= 250) {
                    currentGyroHz = (gyroSampleCount * 1000.0) / gyroElapsed
                    gyroSampleCount = 0L
                    lastGyroCalcTime = now
                }

                val accelElapsed = now - lastAccelCalcTime
                if (accelElapsed >= 250) {
                    currentAccelHz = (accelSampleCount * 1000.0) / accelElapsed
                    accelSampleCount = 0L
                    lastAccelCalcTime = now
                }

                val correctedGx = latestGyro[0] - offsetX
                val correctedGy = latestGyro[1] - offsetY
                val correctedGz = latestGyro[2] - offsetZ

                val driftRad = sqrt(correctedGx * correctedGx + correctedGy * correctedGy + correctedGz * correctedGz)
                val driftDeg = (driftRad * 180f / PI.toFloat())

                synchronized(recentGyroMagnitudes) {
                    recentGyroMagnitudes.addLast(driftRad)
                    if (recentGyroMagnitudes.size > 100) {
                        recentGyroMagnitudes.removeFirst()
                    }
                }

                val variance = synchronized(recentGyroMagnitudes) {
                    if (recentGyroMagnitudes.isEmpty()) 0f
                    else {
                        val mean = recentGyroMagnitudes.average().toFloat()
                        recentGyroMagnitudes.map { (it - mean).pow(2) }.average().toFloat()
                    }
                }

                _telemetrySummary.value = MotionTelemetrySummary(
                    gyroX = correctedGx,
                    gyroY = correctedGy,
                    gyroZ = correctedGz,
                    accelX = latestAccel[0],
                    accelY = latestAccel[1],
                    accelZ = latestAccel[2],
                    gyroHz = currentGyroHz,
                    accelHz = currentAccelHz,
                    driftMagnitudeRads = driftRad,
                    driftMagnitudeDegs = driftDeg,
                    noiseVariance = variance,
                    isCalibrated = isCalibrated,
                    offsetX = offsetX,
                    offsetY = offsetY,
                    offsetZ = offsetZ
                )

                delay(33) // Decoupled ~30Hz update to Compose
            }
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        val values = event.values ?: return

        when (event.sensor.type) {
            Sensor.TYPE_GYROSCOPE -> {
                gyroSampleCount++
                latestGyro = floatArrayOf(values[0], values[1], values[2])

                val cx = values[0] - offsetX
                val cy = values[1] - offsetY
                val cz = values[2] - offsetZ

                pushToRing(gyroHistoryX, cx)
                pushToRing(gyroHistoryY, cy)
                pushToRing(gyroHistoryZ, cz)
            }
            Sensor.TYPE_ACCELEROMETER -> {
                accelSampleCount++
                latestAccel = floatArrayOf(values[0], values[1], values[2])

                pushToRing(accelHistoryX, values[0])
                pushToRing(accelHistoryY, values[1])
                pushToRing(accelHistoryZ, values[2])
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun pushToRing(queue: ConcurrentLinkedDeque<Float>, value: Float) {
        queue.addLast(value)
        while (queue.size > 200) {
            queue.pollFirst()
        }
    }

    /**
     * Tares zero-point drift by capturing current resting values as baseline bias.
     */
    fun calibrateZeroPoint() {
        offsetX = latestGyro[0]
        offsetY = latestGyro[1]
        offsetZ = latestGyro[2]
        isCalibrated = true
    }

    fun resetCalibration() {
        offsetX = 0f
        offsetY = 0f
        offsetZ = 0f
        isCalibrated = false
    }
}
