package com.gamer.calibration.modules.controller

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
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
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration
import com.gamer.calibration.ui.components.KeepScreenOn
import com.gamer.calibration.ui.components.LabBadge
import com.gamer.calibration.ui.components.LabTopBar

@Composable
fun ControllerPrecisionLabScreen(
    controllerEngine: ControllerTelemetryEngine,
    onBack: () -> Unit,
    onFinish: (circularityErrorPercent: Double, isConnected: Boolean) -> Unit,
    onToggleOrientation: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    KeepScreenOn()

    val telemetry by controllerEngine.telemetrySummary.collectAsState()
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    var selectedStickIsLeft by remember { mutableStateOf(true) }

    var leftRumbleSlider by remember { mutableFloatStateOf(0.7f) }
    var rightRumbleSlider by remember { mutableFloatStateOf(0.7f) }

    var frameTick by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos { time ->
                frameTick = time
            }
        }
    }

    val activeStick = if (selectedStickIsLeft) telemetry.leftStick else telemetry.rightStick
    val activeBins = if (selectedStickIsLeft) controllerEngine.leftOuterBoundaryBins else controllerEngine.rightOuterBoundaryBins
    val activeTrail = if (selectedStickIsLeft) controllerEngine.leftStickTrail else controllerEngine.rightStickTrail

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MinimalGlassColors.Background)
    ) {
        LabTopBar(
            title = "Controller & Joysticks",
            subtitle = "Analog stick circularity error & haptics",
            onBack = onBack,
            onRotate = onToggleOrientation,
            onReset = { controllerEngine.resetCircularity() },
            onFinish = { onFinish(activeStick.outerCircularityErrorPercent, telemetry.device.isConnected) },
            badges = {
                LabBadge(
                    label = "Gamepad",
                    value = if (telemetry.device.isConnected) telemetry.device.name.take(12) else "Disconnected",
                    valueColor = if (telemetry.device.isConnected) MinimalGlassColors.SuccessGreen else MinimalGlassColors.TextMuted
                )
                LabBadge(
                    label = "Circularity Error",
                    value = String.format(Locale.US, "%.1f", activeStick.outerCircularityErrorPercent),
                    unit = "%",
                    valueColor = when {
                        activeStick.outerCircularityErrorPercent in 0.1..5.0 -> MinimalGlassColors.SuccessGreen
                        activeStick.outerCircularityErrorPercent in 5.1..12.0 -> MinimalGlassColors.PrimaryBlue
                        else -> MinimalGlassColors.WarningAmber
                    }
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
                    text = "Connect a Bluetooth/USB gamepad. Rotate the thumbstick in full 360° circles 3 times to measure outer boundary circularity.",
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
                // Polar Circularity Canvas Card
                Box(
                    modifier = Modifier
                        .weight(0.65f)
                        .fillMaxSize()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MinimalGlassColors.CardSurface)
                        .border(1.dp, MinimalGlassColors.GlassBorder, RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    StickCanvasContent(
                        selectedStickIsLeft = selectedStickIsLeft,
                        onSelectLeft = { selectedStickIsLeft = true },
                        onSelectRight = { selectedStickIsLeft = false },
                        activeStick = activeStick,
                        activeBins = activeBins,
                        activeTrail = activeTrail.toList(),
                        frameTick = frameTick
                    )
                }

                // Right: Rumble Feedback
                Box(
                    modifier = Modifier
                        .weight(0.35f)
                        .fillMaxSize()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MinimalGlassColors.CardSurface)
                        .border(1.dp, MinimalGlassColors.GlassBorder, RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    HapticsFeedbackContent(
                        leftSlider = leftRumbleSlider,
                        onLeftChange = { leftRumbleSlider = it },
                        rightSlider = rightRumbleSlider,
                        onRightChange = { rightRumbleSlider = it },
                        onTest = { controllerEngine.triggerRumble(leftRumbleSlider, rightRumbleSlider, 350L) },
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
                // Top: Polar Circularity Canvas (58% height)
                Box(
                    modifier = Modifier
                        .weight(0.58f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MinimalGlassColors.CardSurface)
                        .border(1.dp, MinimalGlassColors.GlassBorder, RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    StickCanvasContent(
                        selectedStickIsLeft = selectedStickIsLeft,
                        onSelectLeft = { selectedStickIsLeft = true },
                        onSelectRight = { selectedStickIsLeft = false },
                        activeStick = activeStick,
                        activeBins = activeBins,
                        activeTrail = activeTrail.toList(),
                        frameTick = frameTick
                    )
                }

                // Bottom: Wide Haptics Feedback Card (42% height)
                Box(
                    modifier = Modifier
                        .weight(0.42f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MinimalGlassColors.CardSurface)
                        .border(1.dp, MinimalGlassColors.GlassBorder, RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    HapticsFeedbackContent(
                        leftSlider = leftRumbleSlider,
                        onLeftChange = { leftRumbleSlider = it },
                        rightSlider = rightRumbleSlider,
                        onRightChange = { rightRumbleSlider = it },
                        onTest = { controllerEngine.triggerRumble(leftRumbleSlider, rightRumbleSlider, 350L) },
                        isLandscape = false
                    )
                }
            }
        }
    }
}

@Composable
private fun StickCanvasContent(
    selectedStickIsLeft: Boolean,
    onSelectLeft: () -> Unit,
    onSelectRight: () -> Unit,
    activeStick: com.gamer.calibration.domain.model.StickTelemetry,
    activeBins: FloatArray,
    activeTrail: List<Pair<Float, Float>>,
    frameTick: Long
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left / Right Stick Toggle
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selectedStickIsLeft) MinimalGlassColors.PrimaryBlueLight else MinimalGlassColors.BackgroundSubtle)
                        .clickable { onSelectLeft() }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Left Stick (LS)",
                        style = MinimalTypography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = if (selectedStickIsLeft) MinimalGlassColors.PrimaryBlue else MinimalGlassColors.TextSecondary
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (!selectedStickIsLeft) MinimalGlassColors.PrimaryBlueLight else MinimalGlassColors.BackgroundSubtle)
                        .clickable { onSelectRight() }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Right Stick (RS)",
                        style = MinimalTypography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = if (!selectedStickIsLeft) MinimalGlassColors.PrimaryBlue else MinimalGlassColors.TextSecondary
                    )
                }
            }

            val rating = when {
                activeStick.outerCircularityErrorPercent in 0.1..5.0 -> "Hall Effect / TMR (< 5% error)"
                activeStick.outerCircularityErrorPercent in 5.1..12.0 -> "Standard Gating"
                activeStick.outerCircularityErrorPercent > 12.0 -> "Square Clipping Detected"
                else -> "Rotate stick 360°"
            }
            Text(
                text = rating,
                style = MinimalTypography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = if (activeStick.outerCircularityErrorPercent in 0.1..5.0) MinimalGlassColors.SuccessGreen else MinimalGlassColors.TextSecondary
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val _tick = frameTick
                val width = size.width
                val height = size.height
                val center = Offset(width / 2f, height / 2f)
                val maxRadius = (minOf(width, height) / 2f) * 0.85f

                if (maxRadius <= 0f) return@Canvas

                // Concentric Guide Rings
                for (frac in listOf(0.25f, 0.5f, 0.75f, 1.0f)) {
                    val r = maxRadius * frac
                    drawCircle(
                        color = if (frac == 1.0f) MinimalGlassColors.PrimaryBlue.copy(alpha = 0.4f) else MinimalGlassColors.GlassBorderSubtle,
                        radius = r,
                        center = center,
                        style = Stroke(if (frac == 1.0f) 1.5.dp.toPx() else 1f)
                    )
                }

                // Axes
                drawLine(
                    color = MinimalGlassColors.GlassBorderSubtle,
                    start = Offset(center.x - maxRadius * 1.1f, center.y),
                    end = Offset(center.x + maxRadius * 1.1f, center.y),
                    strokeWidth = 1f
                )
                drawLine(
                    color = MinimalGlassColors.GlassBorderSubtle,
                    start = Offset(center.x, center.y - maxRadius * 1.1f),
                    end = Offset(center.x, center.y + maxRadius * 1.1f),
                    strokeWidth = 1f
                )

                // Outer Boundary Envelope
                val boundaryPath = Path()
                var firstPoint: Offset? = null
                for (deg in 0 until 360) {
                    val binR = activeBins[deg]
                    if (binR > 0.3f) {
                        val rad = deg * (Math.PI / 180.0)
                        val px = center.x + (cos(rad).toFloat() * binR * maxRadius)
                        val py = center.y + (sin(rad).toFloat() * binR * maxRadius)
                        if (firstPoint == null) {
                            firstPoint = Offset(px, py)
                            boundaryPath.moveTo(px, py)
                        } else {
                            boundaryPath.lineTo(px, py)
                        }
                    }
                }
                if (firstPoint != null) {
                    drawPath(
                        path = boundaryPath,
                        color = if (activeStick.outerCircularityErrorPercent < 6.0) MinimalGlassColors.SuccessGreen else MinimalGlassColors.WarningAmber,
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // Scatter Trail
                val trail = activeTrail.toList()
                for (pt in trail) {
                    val tx = center.x + (pt.first * maxRadius)
                    val ty = center.y + (pt.second * maxRadius)
                    drawCircle(
                        color = MinimalGlassColors.TextMuted.copy(alpha = 0.35f),
                        radius = 2.dp.toPx(),
                        center = Offset(tx, ty)
                    )
                }

                // Stick Head
                val curX = center.x + (activeStick.rawX * maxRadius)
                val curY = center.y + (activeStick.rawY * maxRadius)

                drawLine(
                    color = MinimalGlassColors.PrimaryBlue.copy(alpha = 0.4f),
                    start = center,
                    end = Offset(curX, curY),
                    strokeWidth = 2.dp.toPx()
                )

                drawCircle(
                    color = MinimalGlassColors.PrimaryBlue.copy(alpha = 0.2f),
                    radius = 16.dp.toPx(),
                    center = Offset(curX, curY)
                )
                drawCircle(
                    color = MinimalGlassColors.PrimaryBlue,
                    radius = 6.dp.toPx(),
                    center = Offset(curX, curY)
                )
            }
        }
    }
}

@Composable
private fun HapticsFeedbackContent(
    leftSlider: Float,
    onLeftChange: (Float) -> Unit,
    rightSlider: Float,
    onRightChange: (Float) -> Unit,
    onTest: () -> Unit,
    isLandscape: Boolean
) {
    if (isLandscape) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Haptic Vibration Motors",
                    style = MinimalTypography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MinimalGlassColors.TextPrimary
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text("Left Motor (Heavy Low Freq)", style = MinimalTypography.bodyMedium, color = MinimalGlassColors.TextSecondary)
                Slider(
                    value = leftSlider,
                    onValueChange = onLeftChange,
                    colors = SliderDefaults.colors(
                        thumbColor = MinimalGlassColors.PrimaryBlue,
                        activeTrackColor = MinimalGlassColors.PrimaryBlue
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text("Right Motor (Crisp High Freq)", style = MinimalTypography.bodyMedium, color = MinimalGlassColors.TextSecondary)
                Slider(
                    value = rightSlider,
                    onValueChange = onRightChange,
                    colors = SliderDefaults.colors(
                        thumbColor = MinimalGlassColors.SuccessGreen,
                        activeTrackColor = MinimalGlassColors.SuccessGreen
                    )
                )
            }

            CyberButton(
                text = "Test Rumble Feedback",
                onClick = onTest,
                icon = Icons.Default.Vibration,
                accentColor = MinimalGlassColors.PrimaryBlue,
                filled = true,
                modifier = Modifier.fillMaxWidth()
            )
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
                    text = "Haptic Vibration Motors",
                    style = MinimalTypography.headlineSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
                    color = MinimalGlassColors.TextPrimary
                )

                Text("Left Motor: ${(leftSlider * 100).toInt()}%", style = MinimalTypography.labelSmall.copy(fontSize = 11.sp), color = MinimalGlassColors.TextSecondary)
                Slider(
                    value = leftSlider,
                    onValueChange = onLeftChange,
                    modifier = Modifier.height(26.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = MinimalGlassColors.PrimaryBlue,
                        activeTrackColor = MinimalGlassColors.PrimaryBlue
                    )
                )

                Text("Right Motor: ${(rightSlider * 100).toInt()}%", style = MinimalTypography.labelSmall.copy(fontSize = 11.sp), color = MinimalGlassColors.TextSecondary)
                Slider(
                    value = rightSlider,
                    onValueChange = onRightChange,
                    modifier = Modifier.height(26.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = MinimalGlassColors.SuccessGreen,
                        activeTrackColor = MinimalGlassColors.SuccessGreen
                    )
                )
            }

            CyberButton(
                text = "Test Rumble",
                onClick = onTest,
                icon = Icons.Default.Vibration,
                accentColor = MinimalGlassColors.PrimaryBlue,
                filled = true,
                modifier = Modifier.width(135.dp)
            )
        }
    }
}
