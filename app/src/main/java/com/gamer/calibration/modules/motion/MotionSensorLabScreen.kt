package com.gamer.calibration.modules.motion

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
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
import com.gamer.calibration.ui.theme.TelemetryFonts
import kotlinx.coroutines.delay
import java.util.Locale

import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration
import com.gamer.calibration.ui.components.KeepScreenOn
import com.gamer.calibration.ui.components.LabBadge
import com.gamer.calibration.ui.components.LabTopBar

@Composable
fun MotionSensorLabScreen(
    motionEngine: MotionTelemetryEngine,
    onBack: () -> Unit,
    onFinish: (driftDegs: Float, isCalibrated: Boolean) -> Unit,
    onToggleOrientation: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    KeepScreenOn()

    val telemetry by motionEngine.telemetrySummary.collectAsState()
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    // Guided calibration state
    var currentStep by remember { mutableIntStateOf(1) } // 1 = Resting drift, 2 = 90 deg tilt, 3 = Completed
    var stillTimerProgress by remember { mutableFloatStateOf(0f) }
    var axisDeflected by remember { mutableFloatStateOf(0f) }

    val onResetCalibration = {
        motionEngine.resetCalibration()
        currentStep = 1
        stillTimerProgress = 0f
        axisDeflected = 0f
    }

    DisposableEffect(Unit) {
        motionEngine.start()
        onDispose {
            motionEngine.stop()
        }
    }

    // Step 1: Guided Still Detection (charges progress while still, gently decays if bumped)
    LaunchedEffect(currentStep) {
        if (currentStep == 1) {
            while (currentStep == 1) {
                delay(50)
                if (telemetry.driftMagnitudeDegs < 0.6f) {
                    stillTimerProgress = (stillTimerProgress + 0.035f).coerceAtMost(1f)
                    if (stillTimerProgress >= 1f) {
                        motionEngine.calibrateZeroPoint()
                        currentStep = 2
                        break
                    }
                } else {
                    stillTimerProgress = (stillTimerProgress - 0.05f).coerceAtLeast(0f)
                }
            }
        }
    }

    // Step 2: Axis Rotation Verification
    LaunchedEffect(currentStep) {
        if (currentStep == 2) {
            while (currentStep == 2) {
                delay(50)
                if (telemetry.driftMagnitudeDegs > 2.0f) {
                    axisDeflected = 1f
                    delay(800)
                    currentStep = 3
                    break
                }
            }
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
            title = "Motion & Gyroscope",
            subtitle = "Aim stability & drift calibration",
            onBack = onBack,
            onRotate = onToggleOrientation,
            onReset = onResetCalibration,
            onFinish = { onFinish(telemetry.driftMagnitudeDegs, telemetry.isCalibrated) },
            badges = {
                LabBadge(
                    label = "Drift Rate",
                    value = String.format(Locale.US, "%.2f", telemetry.driftMagnitudeDegs).trim(),
                    unit = "°/s",
                    valueColor = if (telemetry.driftMagnitudeDegs < 0.5f) MinimalGlassColors.SuccessGreen else MinimalGlassColors.WarningAmber
                )
                LabBadge(
                    label = "Rate",
                    value = String.format(Locale.US, "%.0f", telemetry.gyroHz),
                    unit = "Hz"
                )
                LabBadge(
                    label = "Status",
                    value = if (telemetry.isCalibrated) "Calibrated" else "Raw",
                    valueColor = if (telemetry.isCalibrated) MinimalGlassColors.SuccessGreen else MinimalGlassColors.TextMuted
                )
            }
        )

        // Guided Instruction Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MinimalGlassColors.CardSurface)
                .border(1.dp, MinimalGlassColors.GlassBorder, RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(
                                if (currentStep == 3) MinimalGlassColors.SuccessGreenLight
                                else MinimalGlassColors.PrimaryBlueLight
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (currentStep == 3) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MinimalGlassColors.SuccessGreen,
                                modifier = Modifier.size(16.dp)
                            )
                        } else {
                            Text(
                                text = "$currentStep",
                                style = MinimalTypography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MinimalGlassColors.PrimaryBlue
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = when (currentStep) {
                                1 -> "Step 1: Place phone flat on a table and keep still"
                                2 -> "Step 2: Gently rotate phone 90° to verify response"
                                else -> "Calibration verified! Motion stability optimal."
                            },
                            style = MinimalTypography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = MinimalGlassColors.TextPrimary
                        )
                        Text(
                            text = when (currentStep) {
                                1 -> "Measuring zero-point drift bias to eliminate unwanted crosshair wandering."
                                2 -> "Testing dynamic gyro angular velocity response."
                                else -> "Zero-point offsets saved. Your gyro crosshair will not wander."
                            },
                            style = MinimalTypography.bodyMedium,
                            color = MinimalGlassColors.TextSecondary
                        )
                    }
                }

                if (currentStep == 1) {
                    Box(modifier = Modifier.width(120.dp).padding(start = 12.dp)) {
                        LinearProgressIndicator(
                            progress = { stillTimerProgress.coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                            color = MinimalGlassColors.PrimaryBlue,
                            trackColor = MinimalGlassColors.BackgroundSubtle
                        )
                    }
                }
            }
        }

        // Oscilloscope Graph & Bubble Level Workspace
        if (isLandscape) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Waveform Card
                Box(
                    modifier = Modifier
                        .weight(0.72f)
                        .fillMaxSize()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MinimalGlassColors.CardSurface)
                        .border(1.dp, MinimalGlassColors.GlassBorder, RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Motion Waveform",
                                style = MinimalTypography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MinimalGlassColors.TextPrimary
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                Text("● X (Pitch)", style = MinimalTypography.labelSmall, color = MinimalGlassColors.PrimaryBlue)
                                Text("● Y (Roll)", style = MinimalTypography.labelSmall, color = MinimalGlassColors.SuccessGreen)
                                Text("● Z (Yaw)", style = MinimalTypography.labelSmall, color = MinimalGlassColors.WarningAmber)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Canvas(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            val _tick = frameTick
                            val width = size.width
                            val height = size.height
                            val centerY = height / 2f
                            if (width <= 0f || height <= 0f) return@Canvas

                            drawLine(color = MinimalGlassColors.GlassBorder, start = Offset(0f, centerY), end = Offset(width, centerY), strokeWidth = 1.5f)
                            drawLine(color = MinimalGlassColors.GlassBorderSubtle, start = Offset(0f, centerY - height * 0.28f), end = Offset(width, centerY - height * 0.28f), strokeWidth = 1f)
                            drawLine(color = MinimalGlassColors.GlassBorderSubtle, start = Offset(0f, centerY + height * 0.28f), end = Offset(width, centerY + height * 0.28f), strokeWidth = 1f)

                            val samplesX = motionEngine.gyroHistoryX.toList()
                            val samplesY = motionEngine.gyroHistoryY.toList()
                            val samplesZ = motionEngine.gyroHistoryZ.toList()

                            fun drawWave(samples: List<Float>, color: Color, scale: Float) {
                                if (samples.size < 2) return
                                val step = width / (samples.size - 1).coerceAtLeast(1)
                                val path = Path()
                                for (i in samples.indices) {
                                    val x = i * step
                                    val y = (centerY - (samples[i] * scale)).coerceIn(4f, height - 4f)
                                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                                }
                                drawPath(path = path, color = color, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
                            }

                            val scale = 65f
                            drawWave(samplesX, MinimalGlassColors.PrimaryBlue, scale)
                            drawWave(samplesY, MinimalGlassColors.SuccessGreen, scale)
                            drawWave(samplesZ, MinimalGlassColors.WarningAmber, scale)
                        }
                    }
                }

                // Balance Card
                Box(
                    modifier = Modifier
                        .weight(0.28f)
                        .fillMaxSize()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MinimalGlassColors.CardSurface)
                        .border(1.dp, MinimalGlassColors.GlassBorder, RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Phone Balance",
                            style = MinimalTypography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MinimalGlassColors.TextPrimary
                        )

                        Box(modifier = Modifier.size(110.dp), contentAlignment = Alignment.Center) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val _tick = frameTick
                                val radius = size.minDimension / 2f
                                val center = Offset(size.width / 2f, size.height / 2f)

                                drawCircle(color = MinimalGlassColors.GlassBorder, radius = radius, center = center, style = Stroke(1.5.dp.toPx()))
                                drawCircle(color = MinimalGlassColors.SuccessGreen.copy(alpha = 0.1f), radius = radius * 0.3f, center = center)

                                val bubbleX = (center.x + (telemetry.accelX * 5f)).coerceIn(center.x - radius * 0.75f, center.x + radius * 0.75f)
                                val bubbleY = (center.y + (telemetry.accelY * 5f)).coerceIn(center.y - radius * 0.75f, center.y + radius * 0.75f)

                                drawCircle(color = MinimalGlassColors.PrimaryBlue, radius = 7.dp.toPx(), center = Offset(bubbleX, bubbleY))
                            }
                        }

                        CyberButton(
                            text = "Reset Tare",
                            onClick = onResetCalibration,
                            accentColor = MinimalGlassColors.TextSecondary,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        } else {
            // Balanced Vertical 2-Card Layout in Portrait
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
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Motion Waveform",
                                style = MinimalTypography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MinimalGlassColors.TextPrimary
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("● X (Pitch)", style = MinimalTypography.labelSmall, color = MinimalGlassColors.PrimaryBlue)
                                Text("● Y (Roll)", style = MinimalTypography.labelSmall, color = MinimalGlassColors.SuccessGreen)
                                Text("● Z (Yaw)", style = MinimalTypography.labelSmall, color = MinimalGlassColors.WarningAmber)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Canvas(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            val _tick = frameTick
                            val width = size.width
                            val height = size.height
                            val centerY = height / 2f
                            if (width <= 0f || height <= 0f) return@Canvas

                            drawLine(color = MinimalGlassColors.GlassBorder, start = Offset(0f, centerY), end = Offset(width, centerY), strokeWidth = 1.5f)
                            drawLine(color = MinimalGlassColors.GlassBorderSubtle, start = Offset(0f, centerY - height * 0.28f), end = Offset(width, centerY - height * 0.28f), strokeWidth = 1f)
                            drawLine(color = MinimalGlassColors.GlassBorderSubtle, start = Offset(0f, centerY + height * 0.28f), end = Offset(width, centerY + height * 0.28f), strokeWidth = 1f)

                            val samplesX = motionEngine.gyroHistoryX.toList()
                            val samplesY = motionEngine.gyroHistoryY.toList()
                            val samplesZ = motionEngine.gyroHistoryZ.toList()

                            fun drawWave(samples: List<Float>, color: Color, scale: Float) {
                                if (samples.size < 2) return
                                val step = width / (samples.size - 1).coerceAtLeast(1)
                                val path = Path()
                                for (i in samples.indices) {
                                    val x = i * step
                                    val y = (centerY - (samples[i] * scale)).coerceIn(4f, height - 4f)
                                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                                }
                                drawPath(path = path, color = color, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
                            }

                            val scale = 65f
                            drawWave(samplesX, MinimalGlassColors.PrimaryBlue, scale)
                            drawWave(samplesY, MinimalGlassColors.SuccessGreen, scale)
                            drawWave(samplesZ, MinimalGlassColors.WarningAmber, scale)
                        }
                    }
                }

                // Bottom: Wide Phone Balance Level Card (42% height)
                Box(
                    modifier = Modifier
                        .weight(0.42f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MinimalGlassColors.CardSurface)
                        .border(1.dp, MinimalGlassColors.GlassBorder, RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Bubble spirit level circle
                        Box(
                            modifier = Modifier.size(110.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val _tick = frameTick
                                val radius = size.minDimension / 2f
                                val center = Offset(size.width / 2f, size.height / 2f)

                                drawCircle(color = MinimalGlassColors.GlassBorder, radius = radius, center = center, style = Stroke(1.5.dp.toPx()))
                                drawCircle(color = MinimalGlassColors.SuccessGreen.copy(alpha = 0.1f), radius = radius * 0.3f, center = center)

                                val bubbleX = (center.x + (telemetry.accelX * 5f)).coerceIn(center.x - radius * 0.75f, center.x + radius * 0.75f)
                                val bubbleY = (center.y + (telemetry.accelY * 5f)).coerceIn(center.y - radius * 0.75f, center.y + radius * 0.75f)

                                drawCircle(color = MinimalGlassColors.PrimaryBlue, radius = 7.dp.toPx(), center = Offset(bubbleX, bubbleY))
                            }
                        }

                        // Info and Tare action
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Phone Spirit Level",
                                style = MinimalTypography.headlineSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
                                color = MinimalGlassColors.TextPrimary
                            )
                            Text(
                                text = "Tilt: X ${String.format(Locale.US, "%+.1f", telemetry.accelX)} • Y ${String.format(Locale.US, "%+.1f", telemetry.accelY)} m/s²",
                                style = MinimalTypography.bodyMedium,
                                color = MinimalGlassColors.TextSecondary
                            )
                            CyberButton(
                                text = "Reset Zero-Point Tare",
                                onClick = onResetCalibration,
                                accentColor = MinimalGlassColors.TextSecondary,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}
