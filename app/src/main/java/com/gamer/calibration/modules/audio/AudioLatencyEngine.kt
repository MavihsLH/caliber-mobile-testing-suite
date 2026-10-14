package com.gamer.calibration.modules.audio

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.os.Build
import androidx.core.content.ContextCompat
import com.gamer.calibration.domain.model.AudioLatencySummary
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentLinkedDeque
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class AudioLatencyEngine(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    private val _telemetrySummary = MutableStateFlow(AudioLatencySummary())
    val telemetrySummary: StateFlow<AudioLatencySummary> = _telemetrySummary.asStateFlow()

    private val sampleRate = 48000
    private val chirpDurationMs = 25
    private val chirpSampleCount = (sampleRate * chirpDurationMs) / 1000 // 1200 samples
    private val leadSilenceSamples = 480 // 10ms lead padding
    private val chirpSignal = ShortArray(chirpSampleCount)

    val referenceChirpWaveform = FloatArray(120)
    val recordedWaveform = ConcurrentLinkedDeque<Float>()
    val correlationWaveform = ConcurrentLinkedDeque<Float>()

    private val latencyHistory = ArrayDeque<Double>(40)
    private var continuousPingJob: Job? = null

    init {
        generateChirpReference()
        detectAudioRouting()
    }

    private fun generateChirpReference() {
        val freqStart = 1400.0
        val freqEnd = 2800.0
        for (i in 0 until chirpSampleCount) {
            val t = i.toDouble() / sampleRate
            val durationSec = chirpDurationMs / 1000.0
            val instantaneousPhase = 2.0 * PI * (freqStart * t + ((freqEnd - freqStart) / (2.0 * durationSec)) * t * t)
            val window = 0.5 * (1.0 - cos(2.0 * PI * i / (chirpSampleCount - 1)))
            val sample = (window * sin(instantaneousPhase) * Short.MAX_VALUE * 0.95).toInt().toShort()
            chirpSignal[i] = sample
            if (i < 120) {
                referenceChirpWaveform[i] = (sample.toFloat() / Short.MAX_VALUE)
            }
        }
    }

    fun detectAudioRouting() {
        try {
            val am = audioManager ?: return
            var deviceName = "Internal Phone Speaker"
            var isBt = false

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val devices = am.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
                for (dev in devices) {
                    when (dev.type) {
                        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                        AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
                        AudioDeviceInfo.TYPE_HEARING_AID,
                        AudioDeviceInfo.TYPE_BLE_HEADSET,
                        AudioDeviceInfo.TYPE_BLE_SPEAKER -> {
                            val prod = dev.productName?.toString()?.trim() ?: "Bluetooth Audio"
                            deviceName = if (prod.isNotEmpty()) prod else "Bluetooth Audio Device"
                            isBt = true
                            break
                        }
                        AudioDeviceInfo.TYPE_WIRED_HEADSET,
                        AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> {
                            deviceName = "Wired 3.5mm Headphones"
                            break
                        }
                        AudioDeviceInfo.TYPE_USB_HEADSET,
                        AudioDeviceInfo.TYPE_USB_DEVICE -> {
                            val prod = dev.productName?.toString()?.trim() ?: "USB-C Audio DAC"
                            deviceName = if (prod.isNotEmpty()) prod else "USB-C Audio DAC"
                            break
                        }
                    }
                }
            }

            _telemetrySummary.value = _telemetrySummary.value.copy(
                audioRoutingDevice = deviceName,
                isBluetooth = isBt
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun executeAcousticPing(): Double = withContext(Dispatchers.Default) {
        detectAudioRouting()
        _telemetrySummary.value = _telemetrySummary.value.copy(testInProgress = true)

        val hasMicPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        val minTrackBuf = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val trackBufSize = max(minTrackBuf, 4096)

        val minRecordBuf = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val recordBufSize = max(minRecordBuf * 2, 16384)

        var audioRecord: AudioRecord? = null
        var audioTrack: AudioTrack? = null

        try {
            val trackAttributesBuilder = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)

            val trackFormat = AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(sampleRate)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build()

            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(trackAttributesBuilder.build())
                .setAudioFormat(trackFormat)
                .setBufferSizeInBytes(trackBufSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            if (hasMicPermission) {
                try {
                    val recordFormat = AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                        .build()

                    audioRecord = AudioRecord.Builder()
                        .setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION)
                        .setAudioFormat(recordFormat)
                        .setBufferSizeInBytes(recordBufSize)
                        .build()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            audioTrack.play()

            val recordLengthSamples = (sampleRate * 0.55).toInt()
            val recordedData = ShortArray(recordLengthSamples)
            val recordingReady = CompletableDeferred<Unit>()

            val recordJob = scope.launch(Dispatchers.IO) {
                if (audioRecord?.state == AudioRecord.STATE_INITIALIZED) {
                    try {
                        audioRecord.startRecording()
                        // Read warm-up chunk to ensure ADC pipeline is actively capturing
                        val warmup = ShortArray(256)
                        audioRecord.read(warmup, 0, warmup.size)
                        recordingReady.complete(Unit)

                        var offset = 0
                        while (offset < recordLengthSamples && isActive) {
                            val read = audioRecord.read(recordedData, offset, recordLengthSamples - offset)
                            if (read > 0) offset += read else break
                        }
                    } catch (e: Exception) {
                        recordingReady.complete(Unit)
                    }
                } else {
                    recordingReady.complete(Unit)
                }
            }

            // Wait until AudioRecord is actively capturing before firing chirp
            recordingReady.await()

            val leadSilence = ShortArray(leadSilenceSamples)
            audioTrack.write(leadSilence, 0, leadSilence.size)
            audioTrack.write(chirpSignal, 0, chirpSignal.size)

            val tailSilence = ShortArray(trackBufSize / 2)
            audioTrack.write(tailSilence, 0, tailSilence.size)

            recordJob.join()

            var detectedLatencyMs = 0.0
            if (hasMicPermission && audioRecord?.state == AudioRecord.STATE_INITIALIZED) {
                detectedLatencyMs = analyzeAcousticArrival(recordedData)
            }

            if (detectedLatencyMs <= 0.0) {
                // Query OS AudioManager output latency property if echo wasn't audible (e.g., sealed IEMs)
                val framesPerBuffer = audioManager?.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER)?.toIntOrNull() ?: 256
                val halBufferMs = (framesPerBuffer.toDouble() / sampleRate.toDouble()) * 1000.0
                val trackBufferMs = (trackBufSize.toDouble() / (sampleRate * 2.0)) * 1000.0
                detectedLatencyMs = if (_telemetrySummary.value.isBluetooth) {
                    135.0 + trackBufferMs + halBufferMs
                } else {
                    18.0 + trackBufferMs + halBufferMs
                }
            }

            recordPingResult(detectedLatencyMs)
            return@withContext detectedLatencyMs

        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext 0.0
        } finally {
            try {
                audioTrack?.stop()
                audioTrack?.release()
                audioRecord?.stop()
                audioRecord?.release()
            } catch (ignored: Exception) {}

            _telemetrySummary.value = _telemetrySummary.value.copy(testInProgress = false)
        }
    }

    private fun analyzeAcousticArrival(captured: ShortArray): Double {
        if (captured.isEmpty()) return 0.0

        recordedWaveform.clear()
        val decimate = max(1, captured.size / 200)
        for (i in captured.indices step decimate) {
            recordedWaveform.addLast(captured[i].toFloat() / Short.MAX_VALUE)
        }

        val searchWindow = min(captured.size - chirpSampleCount, (sampleRate * 0.48).toInt())
        var maxCorr = 0.0
        var totalEnergy = 0.0
        var bestLag = -1

        val correlationPoints = mutableListOf<Float>()
        val step = 2

        for (lag in 0 until searchWindow step step) {
            var sum = 0.0
            for (i in 0 until chirpSampleCount step 4) {
                sum += (chirpSignal[i].toDouble() * captured[lag + i].toDouble())
            }
            val absSum = abs(sum)
            totalEnergy += absSum
            if (absSum > maxCorr) {
                maxCorr = absSum
                bestLag = lag
            }
            if (correlationPoints.size < 200 && lag % (searchWindow / 200).coerceAtLeast(1) == 0) {
                correlationPoints.add((absSum / 1e8).toFloat())
            }
        }

        correlationWaveform.clear()
        correlationPoints.forEach { correlationWaveform.addLast(it) }

        val meanCorr = if (searchWindow > 0) totalEnergy / (searchWindow / step) else 1.0
        val peakToAverageRatio = if (meanCorr > 0) maxCorr / meanCorr else 0.0

        if (bestLag > leadSilenceSamples && maxCorr > 3.5e7 && peakToAverageRatio > 3.2) {
            val netLagSamples = (bestLag - leadSilenceSamples).coerceAtLeast(1)
            val ms = (netLagSamples.toDouble() / sampleRate.toDouble()) * 1000.0
            if (ms in 8.0..480.0) {
                return ms
            }
        }
        return 0.0
    }

    private fun recordPingResult(latencyMs: Double) {
        if (latencyMs <= 0.0) return
        synchronized(latencyHistory) {
            latencyHistory.addLast(latencyMs)
            if (latencyHistory.size > 30) latencyHistory.removeFirst()
        }

        val allResults = synchronized(latencyHistory) { latencyHistory.toList() }
        val minLat = allResults.minOrNull() ?: latencyMs
        val maxLat = allResults.maxOrNull() ?: latencyMs
        val avgLat = allResults.average()

        val variance = allResults.map { (it - avgLat).pow(2.0) }.average()
        val jitter = sqrt(variance)

        _telemetrySummary.value = _telemetrySummary.value.copy(
            lastLatencyMs = latencyMs,
            minLatencyMs = minLat,
            maxLatencyMs = maxLat,
            averageLatencyMs = avgLat,
            jitterMs = jitter,
            totalPingsConducted = _telemetrySummary.value.totalPingsConducted + 1,
            latencyHistory = allResults.map { it.toFloat() }
        )
    }

    fun startContinuousPing(intervalMs: Long = 700L) {
        if (continuousPingJob?.isActive == true) return
        continuousPingJob = scope.launch(Dispatchers.Default) {
            while (isActive) {
                executeAcousticPing()
                delay(intervalMs)
            }
        }
    }

    fun stopContinuousPing() {
        continuousPingJob?.cancel()
        continuousPingJob = null
    }

    fun resetStats() {
        synchronized(latencyHistory) { latencyHistory.clear() }
        correlationWaveform.clear()
        recordedWaveform.clear()
        _telemetrySummary.value = AudioLatencySummary(
            audioRoutingDevice = _telemetrySummary.value.audioRoutingDevice,
            isBluetooth = _telemetrySummary.value.isBluetooth
        )
    }
}
