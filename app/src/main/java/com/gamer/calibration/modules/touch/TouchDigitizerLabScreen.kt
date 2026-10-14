package com.gamer.calibration.modules.touch

import android.view.MotionEvent
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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamer.calibration.ui.components.CyberButton
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.tween
import com.gamer.calibration.ui.components.HoldToConfirmButton
import com.gamer.calibration.ui.components.KeepScreenOn
import com.gamer.calibration.ui.components.LabTopBar
import com.gamer.calibration.ui.theme.MinimalGlassColors
import com.gamer.calibration.ui.theme.MinimalTypography
import com.gamer.calibration.ui.theme.TelemetryFonts
import java.util.Locale

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun TouchDigitizerLabScreen(
    touchEngine: TouchTelemetryEngine,
    onBack: () -> Unit,
    onFinish: (samplingHz: Double, maxPointers: Int) -> Unit,
    onToggleOrientation: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    KeepScreenOn()

    val telemetry by touchEngine.telemetrySummary.collectAsState()

    var frameTick by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos { time ->
                frameTick = time
            }
        }
    }

    val isTouching = telemetry.activePointerCount > 0

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MinimalGlassColors.Background)
    ) {
        LabTopBar(
            title = "Touch Screen & Digitizer",
            subtitle = "Hardware polling rate & claw multi-touch test",
            onBack = onBack,
            onRotate = onToggleOrientation,
            onReset = { touchEngine.resetStats() },
            onFinish = {
                val recordedHz = if (telemetry.peakHz > 0) telemetry.peakHz else telemetry.instantaneousHz
                onFinish(recordedHz, telemetry.maxConcurrentPointers)
            },
            actionButton = {
                HoldToConfirmButton(
                    text = "Finish & Save",
                    onConfirmed = {
                        val recordedHz = if (telemetry.peakHz > 0) telemetry.peakHz else telemetry.instantaneousHz
                        onFinish(recordedHz, telemetry.maxConcurrentPointers)
                    },
                    requiredHoldMs = 850L,
                    accentColor = MinimalGlassColors.PrimaryBlue
                )
            },
            badges = {
                DigitizerHudStrip(
                    pointerCount = telemetry.activePointerCount,
                    instantHz = telemetry.instantaneousHz,
                    peakHz = telemetry.peakHz
                )
            }
        )

        // Dynamic Instruction Ribbon: Fades out and collapses immediately when user touches screen
        AnimatedVisibility(
            visible = !isTouching,
            enter = fadeIn(animationSpec = tween(150)) + expandVertically(animationSpec = tween(150)),
            exit = fadeOut(animationSpec = tween(120)) + shrinkVertically(animationSpec = tween(120))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MinimalGlassColors.CardSurface)
                    .border(1.dp, MinimalGlassColors.GlassBorder, RoundedCornerShape(10.dp))
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(MinimalGlassColors.PrimaryBlue)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Swipe rapidly across the screen for max digitizer Hz. Place 4+ fingers to benchmark claw grip.",
                        style = MinimalTypography.bodyMedium.copy(fontSize = 12.sp),
                        color = MinimalGlassColors.TextSecondary
                    )
                }
            }
        }

        // Clean Canvas Workspace
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MinimalGlassColors.CardSurface)
                .border(1.dp, MinimalGlassColors.GlassBorder, RoundedCornerShape(14.dp))
                .pointerInteropFilter { motionEvent ->
                    touchEngine.processMotionEvent(motionEvent)
                    true
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val _tick = frameTick
                val width = size.width
                val height = size.height

                // Subtle grid
                val gridSpacing = 120.dp.toPx()
                var gx = 0f
                while (gx < width) {
                    drawLine(
                        color = MinimalGlassColors.GlassBorderSubtle,
                        start = Offset(gx, 0f),
                        end = Offset(gx, height),
                        strokeWidth = 1f
                    )
                    gx += gridSpacing
                }

                var gy = 0f
                while (gy < height) {
                    drawLine(
                        color = MinimalGlassColors.GlassBorderSubtle,
                        start = Offset(0f, gy),
                        end = Offset(width, gy),
                        strokeWidth = 1f
                    )
                    gy += gridSpacing
                }

                val pointers = touchEngine.activePointers.values.toList()

                for (ptr in pointers) {
                    val color = MinimalGlassColors.PointerPalette[ptr.id % MinimalGlassColors.PointerPalette.size]

                    // Historical Packet Trail dots
                    if (ptr.historicalTrail.size > 1) {
                        for (dot in ptr.historicalTrail) {
                            drawCircle(
                                color = color.copy(alpha = 0.4f),
                                radius = 2.5.dp.toPx(),
                                center = dot
                            )
                        }
                    }

                    // Stroke Trail
                    if (ptr.trail.size > 1) {
                        val path = Path()
                        path.moveTo(ptr.trail.first().x, ptr.trail.first().y)
                        for (i in 1 until ptr.trail.size) {
                            path.lineTo(ptr.trail[i].x, ptr.trail[i].y)
                        }
                        drawPath(
                            path = path,
                            color = color.copy(alpha = 0.85f),
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }

                    val baseRadius = 24.dp.toPx()
                    val dynamicRadius = baseRadius + (ptr.pressure * 24.dp.toPx())

                    // Contact disc
                    drawCircle(
                        color = color.copy(alpha = 0.15f),
                        radius = dynamicRadius + 8.dp.toPx(),
                        center = Offset(ptr.x, ptr.y)
                    )
                    drawCircle(
                        color = color.copy(alpha = 0.6f),
                        radius = dynamicRadius,
                        center = Offset(ptr.x, ptr.y)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 4.dp.toPx(),
                        center = Offset(ptr.x, ptr.y)
                    )

                    // Theme-aware high-contrast text overlay
                    val labelColorInt = if (MinimalGlassColors.isDarkTheme) {
                        android.graphics.Color.parseColor("#E2E8F0")
                    } else {
                        android.graphics.Color.parseColor("#1E293B")
                    }
                    val textPaint = android.graphics.Paint().apply {
                        this.color = labelColorInt
                        textSize = 12.sp.toPx()
                        isAntiAlias = true
                        typeface = android.graphics.Typeface.create(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD)
                    }
                    val label = "Finger ${ptr.id + 1} (${ptr.x.toInt()}, ${ptr.y.toInt()})"
                    val labelX = (ptr.x + dynamicRadius + 10.dp.toPx()).coerceAtMost(width - 150.dp.toPx())
                    val labelY = (ptr.y - 8.dp.toPx()).coerceAtLeast(24.dp.toPx())
                    drawContext.canvas.nativeCanvas.drawText(
                        label,
                        labelX,
                        labelY,
                        textPaint
                    )
                }

                if (pointers.isEmpty()) {
                    val promptColorInt = if (MinimalGlassColors.isDarkTheme) {
                        android.graphics.Color.parseColor("#94A3B8")
                    } else {
                        android.graphics.Color.parseColor("#64748B")
                    }
                    val promptPaint = android.graphics.Paint().apply {
                        this.color = promptColorInt
                        textSize = 14.sp.toPx()
                        isAntiAlias = true
                        textAlign = android.graphics.Paint.Align.CENTER
                        typeface = android.graphics.Typeface.SANS_SERIF
                    }
                    drawContext.canvas.nativeCanvas.drawText(
                        "Touch or swipe anywhere with 1 to 10 fingers to test",
                        width / 2f,
                        height / 2f,
                        promptPaint
                    )
                }
            }
        }
    }
}

@Composable
private fun DigitizerHudStrip(
    pointerCount: Int,
    instantHz: Double,
    peakHz: Double,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MinimalGlassColors.BackgroundSubtle)
            .border(1.dp, MinimalGlassColors.GlassBorderSubtle, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HudMetricItem(
            label = "POINTERS",
            value = "$pointerCount / 10",
            highlight = pointerCount > 0,
            highlightColor = MinimalGlassColors.SuccessGreen
        )

        Box(
            modifier = Modifier
                .width(1.dp)
                .height(18.dp)
                .background(MinimalGlassColors.GlassBorder)
        )

        HudMetricItem(
            label = "RATE",
            value = String.format(Locale.US, "%.0f", instantHz),
            unit = "Hz",
            highlight = instantHz >= 240.0,
            highlightColor = MinimalGlassColors.PrimaryBlue
        )

        Box(
            modifier = Modifier
                .width(1.dp)
                .height(18.dp)
                .background(MinimalGlassColors.GlassBorder)
        )

        HudMetricItem(
            label = "PEAK",
            value = String.format(Locale.US, "%.0f", peakHz),
            unit = "Hz",
            highlight = peakHz > 0,
            highlightColor = MinimalGlassColors.TextPrimary
        )
    }
}

@Composable
private fun HudMetricItem(
    label: String,
    value: String,
    unit: String = "",
    highlight: Boolean = false,
    highlightColor: Color = MinimalGlassColors.TextPrimary,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            style = MinimalTypography.labelSmall.copy(
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            ),
            color = MinimalGlassColors.TextMuted
        )
        Text(
            text = value,
            style = TelemetryFonts.Medium.copy(
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            ),
            color = if (highlight) highlightColor else MinimalGlassColors.TextPrimary
        )
        if (unit.isNotEmpty()) {
            Text(
                text = unit,
                style = MinimalTypography.labelSmall.copy(fontSize = 9.sp),
                color = MinimalGlassColors.TextMuted
            )
        }
    }
}
