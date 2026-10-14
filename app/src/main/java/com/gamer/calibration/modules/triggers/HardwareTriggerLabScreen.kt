package com.gamer.calibration.modules.triggers

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamer.calibration.domain.model.TriggerState
import com.gamer.calibration.ui.components.KeepScreenOn
import com.gamer.calibration.ui.components.LabBadge
import com.gamer.calibration.ui.components.LabTopBar
import com.gamer.calibration.ui.theme.MinimalGlassColors
import com.gamer.calibration.ui.theme.MinimalTypography
import com.gamer.calibration.ui.theme.TelemetryFonts
import java.util.Locale

@Composable
fun HardwareTriggerLabScreen(
    triggerEngine: TriggerTelemetryEngine,
    onBack: () -> Unit,
    onFinish: (lastLatencyMs: Long, peakCps: Double) -> Unit,
    onToggleOrientation: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    KeepScreenOn()

    val context = LocalContext.current
    val telemetry by triggerEngine.triggersSummary.collectAsState()
    val isOverlayRunning by FloatingTelemetryService.isRunningFlow.collectAsState()
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MinimalGlassColors.Background)
    ) {
        LabTopBar(
            title = "Gaming Phone Triggers & HUD",
            subtitle = "Shoulder button actuation & floating telemetry HUD",
            onBack = onBack,
            onRotate = onToggleOrientation,
            onReset = { triggerEngine.resetStats() },
            onFinish = { onFinish(telemetry.lastTriggerLatencyMs, telemetry.peakClicksPerSecond) },
            badges = {
                LabBadge(
                    label = "Peak Speed",
                    value = String.format(Locale.US, "%.1f", telemetry.peakClicksPerSecond),
                    unit = "CPS",
                    valueColor = MinimalGlassColors.PrimaryBlue
                )
                LabBadge(
                    label = "Dispatch Lag",
                    value = "${telemetry.lastTriggerLatencyMs}",
                    unit = "ms"
                )
            }
        )

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
                    text = "Tap your gaming phone's physical shoulder triggers or volume keys rapidly. Drag the floating HUD to the bottom of your screen anytime to dismiss.",
                    style = MinimalTypography.bodyMedium,
                    color = MinimalGlassColors.TextSecondary
                )
            }
        }

        if (isLandscape) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Symmetrical 3x2 Grid including both VOL+ and VOL-
                Column(
                    modifier = Modifier
                        .weight(0.68f)
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CleanTriggerCard(
                            trigger = telemetry.l1Trigger,
                            shortLabel = "L1 SHOULDER",
                            accentColor = MinimalGlassColors.PrimaryBlue,
                            modifier = Modifier.weight(1f)
                        )
                        CleanTriggerCard(
                            trigger = telemetry.r1Trigger,
                            shortLabel = "R1 SHOULDER",
                            accentColor = MinimalGlassColors.SuccessGreen,
                            modifier = Modifier.weight(1f)
                        )
                        CleanTriggerCard(
                            trigger = telemetry.volumeUpTrigger,
                            shortLabel = "VOL+ TRIGGER",
                            accentColor = MinimalGlassColors.WarningAmber,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CleanTriggerCard(
                            trigger = telemetry.l2Trigger,
                            shortLabel = "L2 TRIGGER",
                            accentColor = MinimalGlassColors.PrimaryBlue,
                            modifier = Modifier.weight(1f)
                        )
                        CleanTriggerCard(
                            trigger = telemetry.r2Trigger,
                            shortLabel = "R2 TRIGGER",
                            accentColor = MinimalGlassColors.SuccessGreen,
                            modifier = Modifier.weight(1f)
                        )
                        CleanTriggerCard(
                            trigger = telemetry.volumeDownTrigger,
                            shortLabel = "VOL- TRIGGER",
                            accentColor = MinimalGlassColors.WarningAmber,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(0.32f)
                        .fillMaxSize()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MinimalGlassColors.CardSurface)
                        .border(1.dp, MinimalGlassColors.GlassBorder, RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    FloatingOverlayContent(context, triggerEngine, isOverlayRunning)
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(0.60f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CleanTriggerCard(
                            trigger = telemetry.l1Trigger,
                            shortLabel = "L1 SHOULDER",
                            accentColor = MinimalGlassColors.PrimaryBlue,
                            modifier = Modifier.weight(1f)
                        )
                        CleanTriggerCard(
                            trigger = telemetry.r1Trigger,
                            shortLabel = "R1 SHOULDER",
                            accentColor = MinimalGlassColors.SuccessGreen,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CleanTriggerCard(
                            trigger = telemetry.l2Trigger,
                            shortLabel = "L2 TRIGGER",
                            accentColor = MinimalGlassColors.PrimaryBlue,
                            modifier = Modifier.weight(1f)
                        )
                        CleanTriggerCard(
                            trigger = telemetry.r2Trigger,
                            shortLabel = "R2 TRIGGER",
                            accentColor = MinimalGlassColors.SuccessGreen,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CleanTriggerCard(
                            trigger = telemetry.volumeUpTrigger,
                            shortLabel = "VOL+ TRIGGER",
                            accentColor = MinimalGlassColors.WarningAmber,
                            modifier = Modifier.weight(1f)
                        )
                        CleanTriggerCard(
                            trigger = telemetry.volumeDownTrigger,
                            shortLabel = "VOL- TRIGGER",
                            accentColor = MinimalGlassColors.WarningAmber,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(0.40f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MinimalGlassColors.CardSurface)
                        .border(1.dp, MinimalGlassColors.GlassBorder, RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    FloatingOverlayContent(context, triggerEngine, isOverlayRunning)
                }
            }
        }
    }
}

@Composable
private fun FloatingOverlayContent(
    context: Context,
    triggerEngine: TriggerTelemetryEngine,
    isOverlayRunning: Boolean
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "Floating Telemetry HUD",
                style = MinimalTypography.headlineSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
                color = MinimalGlassColors.TextPrimary
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Monitors display VSYNC refresh rate (Hz/FPS) and active Caliber touch/gamepad polling in a draggable overlay.",
                style = MinimalTypography.bodyMedium,
                color = MinimalGlassColors.TextSecondary
            )

            Spacer(modifier = Modifier.height(10.dp))

            val hasOverlayPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Settings.canDrawOverlays(context)
            } else true

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Active Overlay",
                    style = MinimalTypography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MinimalGlassColors.TextPrimary
                )
                Switch(
                    checked = isOverlayRunning,
                    onCheckedChange = { checked ->
                        if (checked) {
                            if (hasOverlayPermission) {
                                val intent = Intent(context, FloatingTelemetryService::class.java)
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    context.startForegroundService(intent)
                                } else {
                                    context.startService(intent)
                                }
                                triggerEngine.setOverlayActive(true)
                            } else {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    val intent = Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        Uri.parse("package:${context.packageName}")
                                    )
                                    context.startActivity(intent)
                                }
                            }
                        } else {
                            context.stopService(Intent(context, FloatingTelemetryService::class.java))
                            triggerEngine.setOverlayActive(false)
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = MinimalGlassColors.PrimaryBlue
                    )
                )
            }
        }

        Text(
            text = "Tip: Drag the floating pill to the bottom 15% of the screen to close it at any time.",
            style = MinimalTypography.labelSmall.copy(fontSize = 10.sp),
            color = MinimalGlassColors.TextMuted
        )
    }
}

@Composable
private fun CleanTriggerCard(
    trigger: TriggerState,
    shortLabel: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val borderColor by animateColorAsState(
        targetValue = if (trigger.isPressed) accentColor else MinimalGlassColors.GlassBorder,
        label = "trigger_border"
    )

    val bgColor by animateColorAsState(
        targetValue = if (trigger.isPressed) accentColor.copy(alpha = 0.08f) else MinimalGlassColors.CardSurface,
        label = "trigger_bg"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = shortLabel,
                    style = MinimalTypography.labelSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 10.sp),
                    color = MinimalGlassColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (trigger.isPressed) "ACTIVE" else "Idle",
                    style = MinimalTypography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                    color = if (trigger.isPressed) accentColor else MinimalGlassColors.TextMuted
                )
            }

            Text(
                text = "${trigger.totalClicks}",
                style = TelemetryFonts.Hero.copy(fontSize = 22.sp),
                color = if (trigger.isPressed) accentColor else MinimalGlassColors.TextPrimary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = String.format(Locale.US, "%.1f CPS", trigger.clicksPerSecond),
                    style = MinimalTypography.labelSmall.copy(fontSize = 10.sp),
                    color = MinimalGlassColors.TextSecondary
                )
                Text(
                    text = if (trigger.pressDurationMs > 0) "Hold ${trigger.pressDurationMs}ms" else "-- ms",
                    style = MinimalTypography.labelSmall.copy(fontSize = 9.sp),
                    color = MinimalGlassColors.TextMuted
                )
            }
        }
    }
}
