package com.gamer.calibration.modules.audio

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamer.calibration.ui.components.CyberButton
import com.gamer.calibration.ui.components.TelemetryValue
import com.gamer.calibration.ui.theme.MinimalGlassColors
import com.gamer.calibration.ui.theme.MinimalTypography
import kotlinx.coroutines.launch
import java.util.Locale

import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration
import com.gamer.calibration.ui.components.KeepScreenOn
import com.gamer.calibration.ui.components.LabBadge
import com.gamer.calibration.ui.components.LabTopBar

@Composable
fun BareMetalAudioLabScreen(
    audioEngine: AudioLatencyEngine,
    onBack: () -> Unit,
    onFinish: (latencyMs: Double, deviceName: String) -> Unit,
    onToggleOrientation: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    KeepScreenOn()

    val telemetry by audioEngine.telemetrySummary.collectAsState()
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val scope = rememberCoroutineScope()
    var isContinuousRunning by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        audioEngine.detectAudioRouting()
        onDispose {
            audioEngine.stopContinuousPing()
        }
    }

    var frameTick by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos { time ->
                frameTick = time
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MinimalGlassColors.Background)
    ) {
        LabTopBar(
            title = "Audio Latency",
            subtitle = "Bluetooth & speaker acoustic loopback",
            onBack = onBack,
            onRotate = onToggleOrientation,
            onReset = { audioEngine.resetStats() },
            onFinish = {
                val latency = if (telemetry.lastLatencyMs > 0) telemetry.lastLatencyMs else 32.0
                onFinish(latency, telemetry.audioRoutingDevice)
            },
            badges = {
                LabBadge(
                    label = "Acoustic Delay",
                    value = String.format(Locale.US, "%.1f", telemetry.lastLatencyMs),
                    unit = "ms",
                    valueColor = when {
                        telemetry.lastLatencyMs == 0.0 -> MinimalGlassColors.TextMuted
                        telemetry.lastLatencyMs < 45.0 -> MinimalGlassColors.SuccessGreen
                        telemetry.lastLatencyMs < 120.0 -> MinimalGlassColors.PrimaryBlue
                        else -> MinimalGlassColors.WarningAmber
                    }
                )
                LabBadge(
                    label = "Output",
                    value = telemetry.audioRoutingDevice.take(16)
                )
            }
        )

        // Instructions Ribbon
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MinimalGlassColors.CardSurface)
                .border(1.dp, MinimalGlassColors.GlassBorder, RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(MinimalGlassColors.PrimaryBlue)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Keep your phone unmute in a relatively quiet room. Tap 'Test Acoustic Delay' to fire a quick sound burst and calculate exact delay.",
                    style = MinimalTypography.bodyMedium,
                    color = MinimalGlassColors.TextSecondary
                )
            }
        }

        // Workspace
        if (isLandscape) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Left: Acoustic Waveform Oscilloscope
                Box(
                    modifier = Modifier
                        .weight(0.68f)
                        .fillMaxSize()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MinimalGlassColors.CardSurface)
                        .border(1.dp, MinimalGlassColors.GlassBorder, RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    AudioWaveformContent(audioEngine, frameTick)
                }

                // Right: Controls & Reference Benchmarks
                Box(
                    modifier = Modifier
                        .weight(0.32f)
                        .fillMaxSize()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MinimalGlassColors.CardSurface)
                        .border(1.dp, MinimalGlassColors.GlassBorder, RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    AudioControlsContent(
                        telemetry = telemetry,
                        isContinuousRunning = isContinuousRunning,
                        onTriggerPing = { scope.launch { audioEngine.executeAcousticPing() } },
                        onToggleContinuous = {
                            if (isContinuousRunning) {
                                audioEngine.stopContinuousPing()
                                isContinuousRunning = false
                            } else {
                                audioEngine.startContinuousPing()
                                isContinuousRunning = true
                            }
                        },
                        isLandscape = true
                    )
                }
            }
        } else {
            // Adaptive Vertical Layout for Portrait
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Top: Waveform Oscilloscope (58% height)
                Box(
                    modifier = Modifier
                        .weight(0.58f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MinimalGlassColors.CardSurface)
                        .border(1.dp, MinimalGlassColors.GlassBorder, RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    AudioWaveformContent(audioEngine, frameTick)
                }

                // Bottom: Wide Controls & Benchmarks Card (42% height)
                Box(
                    modifier = Modifier
                        .weight(0.42f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MinimalGlassColors.CardSurface)
                        .border(1.dp, MinimalGlassColors.GlassBorder, RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    AudioControlsContent(
                        telemetry = telemetry,
                        isContinuousRunning = isContinuousRunning,
                        onTriggerPing = { scope.launch { audioEngine.executeAcousticPing() } },
                        onToggleContinuous = {
                            if (isContinuousRunning) {
                                audioEngine.stopContinuousPing()
                                isContinuousRunning = false
                            } else {
                                audioEngine.startContinuousPing()
                                isContinuousRunning = true
                            }
                        },
                        isLandscape = false
                    )
                }
            }
        }
    }
}

@Composable
private fun AudioWaveformContent(
    audioEngine: AudioLatencyEngine,
    frameTick: Long
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Microphone Echo Response & Correlation",
                style = MinimalTypography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MinimalGlassColors.TextPrimary
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("● Chirp", style = MinimalTypography.labelSmall.copy(fontSize = 10.sp), color = MinimalGlassColors.PrimaryBlue)
                Text("● Mic", style = MinimalTypography.labelSmall.copy(fontSize = 10.sp), color = MinimalGlassColors.TextMuted)
                Text("● Echo", style = MinimalTypography.labelSmall.copy(fontSize = 10.sp), color = MinimalGlassColors.SuccessGreen)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Canvas(modifier = Modifier.weight(1f).fillMaxWidth()) {
            val _tick = frameTick
            val width = size.width
            val height = size.height
            val centerY = height / 2f

            if (width <= 0f || height <= 0f) return@Canvas

            drawLine(
                color = MinimalGlassColors.GlassBorder,
                start = Offset(0f, centerY),
                end = Offset(width, centerY),
                strokeWidth = 1f
            )

            // 1. Reference Chirp Template
            val refPoints = audioEngine.referenceChirpWaveform
            if (refPoints.isNotEmpty()) {
                val refWidth = width * 0.15f
                val step = refWidth / refPoints.size
                val refPath = Path()
                for (i in refPoints.indices) {
                    val x = i * step
                    val y = centerY - (refPoints[i] * height * 0.35f)
                    if (i == 0) refPath.moveTo(x, y) else refPath.lineTo(x, y)
                }
                drawPath(
                    path = refPath,
                    color = MinimalGlassColors.PrimaryBlue,
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // 2. Recorded Microphone Audio
            val recorded = audioEngine.recordedWaveform.toList()
            if (recorded.size > 1) {
                val step = width / (recorded.size - 1)
                val recPath = Path()
                for (i in recorded.indices) {
                    val x = i * step
                    val y = centerY - (recorded[i] * height * 0.4f)
                    if (i == 0) recPath.moveTo(x, y) else recPath.lineTo(x, y)
                }
                drawPath(
                    path = recPath,
                    color = MinimalGlassColors.TextMuted.copy(alpha = 0.4f),
                    style = Stroke(width = 1.dp.toPx())
                )
            }

            // 3. Correlation Peak
            val corr = audioEngine.correlationWaveform.toList()
            if (corr.size > 1) {
                val maxVal = corr.maxOrNull() ?: 1f
                val normScale = if (maxVal > 0f) (height * 0.45f) / maxVal else 1f
                val step = width / (corr.size - 1)
                val corrPath = Path()
                for (i in corr.indices) {
                    val x = i * step
                    val y = height - 10f - (corr[i] * normScale).coerceIn(0f, height - 20f)
                    if (i == 0) corrPath.moveTo(x, y) else corrPath.lineTo(x, y)
                }
                drawPath(
                    path = corrPath,
                    color = MinimalGlassColors.SuccessGreen,
                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }
    }
}

@Composable
private fun AudioControlsContent(
    telemetry: com.gamer.calibration.domain.model.AudioLatencySummary,
    isContinuousRunning: Boolean,
    onTriggerPing: () -> Unit,
    onToggleContinuous: () -> Unit,
    isLandscape: Boolean
) {
    if (isLandscape) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Reference Targets",
                    style = MinimalTypography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MinimalGlassColors.TextPrimary
                )

                Spacer(modifier = Modifier.height(10.dp))

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Wired 3.5mm / Type-C", style = MinimalTypography.bodyMedium, color = MinimalGlassColors.TextSecondary)
                        Text("< 30 ms", style = MinimalTypography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = MinimalGlassColors.SuccessGreen)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Earbuds Game Mode", style = MinimalTypography.bodyMedium, color = MinimalGlassColors.TextSecondary)
                        Text("40 - 70 ms", style = MinimalTypography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = MinimalGlassColors.PrimaryBlue)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Standard Bluetooth", style = MinimalTypography.bodyMedium, color = MinimalGlassColors.TextSecondary)
                        Text("> 150 ms", style = MinimalTypography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = MinimalGlassColors.WarningAmber)
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CyberButton(
                    text = if (telemetry.testInProgress) "Testing..." else "Test Acoustic Delay",
                    onClick = onTriggerPing,
                    icon = Icons.Default.PlayArrow,
                    accentColor = MinimalGlassColors.PrimaryBlue,
                    filled = true,
                    enabled = !telemetry.testInProgress,
                    modifier = Modifier.fillMaxWidth()
                )

                CyberButton(
                    text = if (isContinuousRunning) "Stop Continuous" else "Continuous Test",
                    onClick = onToggleContinuous,
                    icon = if (isContinuousRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                    accentColor = if (isContinuousRunning) MinimalGlassColors.DangerRed else MinimalGlassColors.TextSecondary,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    } else {
        // Wide Row layout for Portrait
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Reference Targets",
                    style = MinimalTypography.headlineSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
                    color = MinimalGlassColors.TextPrimary
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Wired USB/3.5mm", style = MinimalTypography.labelSmall.copy(fontSize = 11.sp), color = MinimalGlassColors.TextSecondary)
                    Text("< 30 ms", style = MinimalTypography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold), color = MinimalGlassColors.SuccessGreen)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Earbuds Game Mode", style = MinimalTypography.labelSmall.copy(fontSize = 11.sp), color = MinimalGlassColors.TextSecondary)
                    Text("40-70 ms", style = MinimalTypography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold), color = MinimalGlassColors.PrimaryBlue)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Standard Bluetooth", style = MinimalTypography.labelSmall.copy(fontSize = 11.sp), color = MinimalGlassColors.TextSecondary)
                    Text("> 150 ms", style = MinimalTypography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold), color = MinimalGlassColors.WarningAmber)
                }
            }

            Column(
                modifier = Modifier.width(160.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CyberButton(
                    text = if (telemetry.testInProgress) "Testing..." else "Test Delay",
                    onClick = onTriggerPing,
                    icon = Icons.Default.PlayArrow,
                    accentColor = MinimalGlassColors.PrimaryBlue,
                    filled = true,
                    enabled = !telemetry.testInProgress,
                    modifier = Modifier.fillMaxWidth()
                )

                CyberButton(
                    text = if (isContinuousRunning) "Stop Continuous" else "Continuous",
                    onClick = onToggleContinuous,
                    icon = if (isContinuousRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                    accentColor = if (isContinuousRunning) MinimalGlassColors.DangerRed else MinimalGlassColors.TextSecondary,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
