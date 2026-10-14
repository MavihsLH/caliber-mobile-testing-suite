package com.gamer.calibration.ui.dashboard

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.gamer.calibration.R
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.gamer.calibration.ui.components.CompactOptionCard
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.gamer.calibration.ui.theme.TelemetryFonts
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamer.calibration.domain.model.AudioLatencySummary
import com.gamer.calibration.domain.model.ControllerPrecisionSummary
import com.gamer.calibration.domain.model.DeviceInfoHelper
import com.gamer.calibration.domain.model.DeviceSpecs
import com.gamer.calibration.domain.model.DiagnosticReport
import com.gamer.calibration.modules.audio.AudioLatencyEngine
import com.gamer.calibration.modules.controller.ControllerTelemetryEngine
import com.gamer.calibration.modules.triggers.FloatingTelemetryService
import com.gamer.calibration.ui.components.GlassOptionCard
import com.gamer.calibration.ui.theme.MinimalGlassColors
import com.gamer.calibration.ui.theme.MinimalTypography

enum class CalibrationModule {
    DASHBOARD,
    TEST_DETAIL,
    DIAGNOSTIC_REPORT,
    TOUCHSCREEN_LAB,
    MOTION_SENSOR_LAB,
    HARDWARE_TRIGGERS_LAB,
    CONTROLLER_LAB,
    AUDIO_LATENCY_LAB,
    SENSITIVITY_VAULT,
    SYSTEM_TUNER_HUB,
    REFLEX_SANDBOX,
    AV_SYNC_LAB
}

@Composable
fun BentoDashboardScreen(
    report: DiagnosticReport,
    controllerEngine: ControllerTelemetryEngine,
    audioEngine: AudioLatencyEngine,
    onSelectModuleForDetail: (CalibrationModule) -> Unit,
    onOpenReport: () -> Unit,
    onThemeChanged: ((Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val deviceSpecs = remember { DeviceInfoHelper.extract(context) }
    val controllerTelemetry by controllerEngine.telemetrySummary.collectAsState()
    val audioTelemetry by audioEngine.telemetrySummary.collectAsState()

    val dashboardPrefs = remember {
        val oldPrefs = context.getSharedPreferences("gamersense_dashboard_prefs", Context.MODE_PRIVATE)
        val newPrefs = context.getSharedPreferences("caliber_dashboard_prefs", Context.MODE_PRIVATE)
        if (newPrefs.all.isEmpty() && oldPrefs.all.isNotEmpty()) {
            val editor = newPrefs.edit()
            oldPrefs.all.forEach { (k, v) ->
                if (v is Boolean) editor.putBoolean(k, v)
            }
            editor.apply()
        }
        newPrefs
    }

    var isCompactView by rememberSaveable {
        mutableStateOf(dashboardPrefs.getBoolean("is_compact_view", false))
    }
    var showSettingsModal by remember { mutableStateOf(false) }
    var isCalibrationExpanded by rememberSaveable {
        mutableStateOf(dashboardPrefs.getBoolean("is_calibration_expanded", true))
    }
    var isToolsExpanded by rememberSaveable {
        mutableStateOf(dashboardPrefs.getBoolean("is_tools_expanded", false))
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MinimalGlassColors.Background)
    ) {
        DashboardTopHeader(
            onOpenSettings = { showSettingsModal = true }
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(if (isCompactView) 8.dp else 12.dp)
        ) {
            DeviceSpecsCard(specs = deviceSpecs)

            DiagnosticReportBanner(
                report = report,
                onOpenReport = onOpenReport
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SECTIONS",
                    style = MinimalTypography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                    color = MinimalGlassColors.TextMuted
                )

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MinimalGlassColors.BackgroundSubtle)
                        .border(1.dp, MinimalGlassColors.GlassBorderSubtle, RoundedCornerShape(8.dp))
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ViewModeIconButton(
                        icon = Icons.Default.ViewAgenda,
                        contentDescription = "Card view",
                        isSelected = !isCompactView,
                        onClick = {
                            isCompactView = false
                            dashboardPrefs.edit().putBoolean("is_compact_view", false).apply()
                        }
                    )
                    ViewModeIconButton(
                        icon = Icons.AutoMirrored.Filled.ViewList,
                        contentDescription = "Compact view",
                        isSelected = isCompactView,
                        onClick = {
                            isCompactView = true
                            dashboardPrefs.edit().putBoolean("is_compact_view", true).apply()
                        }
                    )
                }
            }

            // OPTION 1: CALIBRATION
            CategoryHeaderCard(
                title = "Calibration",
                subtitle = "Touch, motion, audio, triggers & gamepad",
                icon = Icons.Default.Tune,
                iconTint = MinimalGlassColors.PrimaryBlue,
                iconBg = MinimalGlassColors.PrimaryBlueLight,
                itemCount = 5,
                isExpanded = isCalibrationExpanded,
                onToggle = {
                    val next = !isCalibrationExpanded
                    isCalibrationExpanded = next
                    dashboardPrefs.edit().putBoolean("is_calibration_expanded", next).apply()
                }
            )

            AnimatedVisibility(
                visible = isCalibrationExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(if (isCompactView) 8.dp else 12.dp)
                ) {
                    TouchModuleCard(report = report, isCompact = isCompactView) {
                        onSelectModuleForDetail(CalibrationModule.TOUCHSCREEN_LAB)
                    }

                    MotionModuleCard(report = report, isCompact = isCompactView) {
                        onSelectModuleForDetail(CalibrationModule.MOTION_SENSOR_LAB)
                    }

                    AudioModuleCard(report = report, audioTelemetry = audioTelemetry, isCompact = isCompactView) {
                        onSelectModuleForDetail(CalibrationModule.AUDIO_LATENCY_LAB)
                    }

                    TriggersModuleCard(isCompact = isCompactView) {
                        onSelectModuleForDetail(CalibrationModule.HARDWARE_TRIGGERS_LAB)
                    }

                    ControllerModuleCard(controllerTelemetry = controllerTelemetry, isCompact = isCompactView) {
                        onSelectModuleForDetail(CalibrationModule.CONTROLLER_LAB)
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // OPTION 2: GAMING TOOLS
            CategoryHeaderCard(
                title = "Gaming Tools",
                subtitle = "Sensitivity converter, system guide, reaction & audio sync",
                icon = Icons.Default.SportsEsports,
                iconTint = MinimalGlassColors.SuccessGreen,
                iconBg = MinimalGlassColors.SuccessGreenLight,
                itemCount = 4,
                isExpanded = isToolsExpanded,
                onToggle = {
                    val next = !isToolsExpanded
                    isToolsExpanded = next
                    dashboardPrefs.edit().putBoolean("is_tools_expanded", next).apply()
                }
            )

            AnimatedVisibility(
                visible = isToolsExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(if (isCompactView) 8.dp else 12.dp)
                ) {
                    SensitivityConverterCard(isCompact = isCompactView) {
                        onSelectModuleForDetail(CalibrationModule.SENSITIVITY_VAULT)
                    }

                    SystemSettingsGuideCard(isCompact = isCompactView) {
                        onSelectModuleForDetail(CalibrationModule.SYSTEM_TUNER_HUB)
                    }

                    ReactionTimeTestCard(isCompact = isCompactView) {
                        onSelectModuleForDetail(CalibrationModule.REFLEX_SANDBOX)
                    }

                    AudioDelaySyncCard(isCompact = isCompactView) {
                        onSelectModuleForDetail(CalibrationModule.AV_SYNC_LAB)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        if (showSettingsModal) {
            SettingsModal(
                isDarkTheme = MinimalGlassColors.isDarkTheme,
                onThemeToggle = { isDark ->
                    MinimalGlassColors.isDarkTheme = isDark
                    onThemeChanged?.invoke(isDark)
                },
                onDismiss = { showSettingsModal = false }
            )
        }
    }
}

@Composable
private fun DashboardTopHeader(
    onOpenSettings: () -> Unit
) {
    val isHudActive by FloatingTelemetryService.isRunningFlow.collectAsState()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MinimalGlassColors.CardSurface)
            .border(1.dp, MinimalGlassColors.GlassBorder)
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Image(
                painter = painterResource(id = R.drawable.caliber_header_logo),
                contentDescription = "Caliber",
                modifier = Modifier
                    .height(38.dp)
                    .width(188.dp),
                contentScale = ContentScale.Fit
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Status by exception: Only display active overlay status, zero clutter when dormant
                if (isHudActive) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MinimalGlassColors.SuccessGreenLight)
                            .border(1.dp, MinimalGlassColors.SuccessGreen.copy(alpha = 0.25f), CircleShape)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(MinimalGlassColors.SuccessGreen)
                            )
                            Text(
                                text = "HUD ACTIVE",
                                style = MinimalTypography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                color = MinimalGlassColors.SuccessGreen
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = MinimalGlassColors.TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DiagnosticReportBanner(
    report: DiagnosticReport,
    onOpenReport: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = Color(0x0A0F172A),
                spotColor = Color(0x0F0F172A)
            )
            .clip(RoundedCornerShape(16.dp))
            .background(MinimalGlassColors.CardSurface)
            .border(1.dp, MinimalGlassColors.GlassBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onOpenReport)
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MinimalGlassColors.PrimaryBlueLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Assessment,
                            contentDescription = null,
                            tint = MinimalGlassColors.PrimaryBlue,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Diagnostic Report",
                            style = MinimalTypography.headlineSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
                            color = MinimalGlassColors.TextPrimary
                        )
                        Text(
                            text = "${report.completedCount} of 5 tests calibrated",
                            style = MinimalTypography.bodyMedium.copy(fontSize = 12.sp),
                            color = MinimalGlassColors.TextSecondary
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Grade: ${report.overallGrade}",
                        style = MinimalTypography.labelSmall.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MinimalGlassColors.PrimaryBlue
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = null,
                        tint = MinimalGlassColors.TextMuted,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { (report.completedCount / 5f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = MinimalGlassColors.PrimaryBlue,
                trackColor = MinimalGlassColors.BackgroundSubtle
            )
        }
    }
}

@Composable
private fun TouchModuleCard(report: DiagnosticReport, isCompact: Boolean = false, onClick: () -> Unit) {
    val badge = if (report.touchCompleted) "${report.touchSamplingHz.toInt()} Hz" else null
    if (isCompact) {
        CompactOptionCard(
            title = "Touch Screen & Digitizer",
            icon = Icons.Default.TouchApp,
            iconTint = MinimalGlassColors.PrimaryBlue,
            iconBg = MinimalGlassColors.PrimaryBlueLight,
            badgeText = badge,
            badgeColor = MinimalGlassColors.SuccessGreen,
            badgeBg = MinimalGlassColors.SuccessGreenLight,
            onClick = onClick
        )
    } else {
        GlassOptionCard(
            title = "Touch Screen & Digitizer",
            subtitle = "Measure true touch polling rate (Hz) and claw multi-touch response",
            icon = Icons.Default.TouchApp,
            iconTint = MinimalGlassColors.PrimaryBlue,
            iconBg = MinimalGlassColors.PrimaryBlueLight,
            badgeText = badge,
            badgeColor = MinimalGlassColors.SuccessGreen,
            badgeBg = MinimalGlassColors.SuccessGreenLight,
            onClick = onClick
        )
    }
}

@Composable
private fun MotionModuleCard(report: DiagnosticReport, isCompact: Boolean = false, onClick: () -> Unit) {
    val badge = if (report.motionCompleted) "Calibrated" else null
    if (isCompact) {
        CompactOptionCard(
            title = "Motion & Gyroscope",
            icon = Icons.Default.Sensors,
            iconTint = MinimalGlassColors.SuccessGreen,
            iconBg = MinimalGlassColors.SuccessGreenLight,
            badgeText = badge,
            badgeColor = MinimalGlassColors.SuccessGreen,
            badgeBg = MinimalGlassColors.SuccessGreenLight,
            onClick = onClick
        )
    } else {
        GlassOptionCard(
            title = "Motion & Gyroscope",
            subtitle = "Calibrate zero-point drift for steady gyro crosshair aiming",
            icon = Icons.Default.Sensors,
            iconTint = MinimalGlassColors.SuccessGreen,
            iconBg = MinimalGlassColors.SuccessGreenLight,
            badgeText = badge,
            badgeColor = MinimalGlassColors.SuccessGreen,
            badgeBg = MinimalGlassColors.SuccessGreenLight,
            onClick = onClick
        )
    }
}

@Composable
private fun AudioModuleCard(
    report: DiagnosticReport,
    audioTelemetry: AudioLatencySummary,
    isCompact: Boolean = false,
    onClick: () -> Unit
) {
    val audioDeviceTag = if (audioTelemetry.isBluetooth) {
        "Bluetooth: ${audioTelemetry.audioRoutingDevice}"
    } else {
        audioTelemetry.audioRoutingDevice
    }
    val badge = if (report.audioCompleted) "${report.audioLatencyMs.toInt()} ms" else null

    if (isCompact) {
        CompactOptionCard(
            title = "Audio Latency",
            icon = Icons.Default.GraphicEq,
            iconTint = MinimalGlassColors.Purple,
            iconBg = MinimalGlassColors.PurpleLight,
            badgeText = badge,
            badgeColor = MinimalGlassColors.SuccessGreen,
            badgeBg = MinimalGlassColors.SuccessGreenLight,
            deviceTag = audioDeviceTag,
            onClick = onClick
        )
    } else {
        GlassOptionCard(
            title = "Audio Latency",
            subtitle = "Measure Bluetooth & wired acoustic transit delay",
            icon = Icons.Default.GraphicEq,
            iconTint = MinimalGlassColors.Purple,
            iconBg = MinimalGlassColors.PurpleLight,
            badgeText = badge,
            badgeColor = MinimalGlassColors.SuccessGreen,
            badgeBg = MinimalGlassColors.SuccessGreenLight,
            deviceTag = "Output: $audioDeviceTag",
            onClick = onClick
        )
    }
}

@Composable
private fun TriggersModuleCard(isCompact: Boolean = false, onClick: () -> Unit) {
    val isHudActive by FloatingTelemetryService.isRunningFlow.collectAsState()
    val badge = if (isHudActive) "HUD Active" else null
    if (isCompact) {
        CompactOptionCard(
            title = "Gaming Triggers & HUD",
            icon = Icons.Default.Layers,
            iconTint = MinimalGlassColors.WarningAmber,
            iconBg = MinimalGlassColors.WarningAmberLight,
            badgeText = badge,
            badgeColor = MinimalGlassColors.PrimaryBlue,
            badgeBg = MinimalGlassColors.PrimaryBlueLight,
            onClick = onClick
        )
    } else {
        GlassOptionCard(
            title = "Gaming Triggers & HUD",
            subtitle = "Test shoulder button delay and launch floating in-game FPS HUD",
            icon = Icons.Default.Layers,
            iconTint = MinimalGlassColors.WarningAmber,
            iconBg = MinimalGlassColors.WarningAmberLight,
            badgeText = badge,
            badgeColor = MinimalGlassColors.PrimaryBlue,
            badgeBg = MinimalGlassColors.PrimaryBlueLight,
            onClick = onClick
        )
    }
}

@Composable
private fun ControllerModuleCard(
    controllerTelemetry: ControllerPrecisionSummary,
    isCompact: Boolean = false,
    onClick: () -> Unit
) {
    val isConnected = controllerTelemetry.device.isConnected
    val controllerDeviceTag = if (isConnected) {
        "Connected: ${controllerTelemetry.device.guessedBrand}"
    } else {
        "No gamepad connected"
    }
    val badge = if (isConnected) "Connected" else null

    if (isCompact) {
        CompactOptionCard(
            title = "Controller & Joysticks",
            icon = Icons.Default.SportsEsports,
            iconTint = MinimalGlassColors.PrimaryBlue,
            iconBg = MinimalGlassColors.PrimaryBlueLight,
            badgeText = badge,
            badgeColor = MinimalGlassColors.SuccessGreen,
            badgeBg = MinimalGlassColors.SuccessGreenLight,
            deviceTag = controllerDeviceTag,
            onClick = onClick
        )
    } else {
        GlassOptionCard(
            title = "Controller & Joysticks",
            subtitle = "Benchmark Hall Effect stick circularity and test dual rumble",
            icon = Icons.Default.SportsEsports,
            iconTint = MinimalGlassColors.PrimaryBlue,
            iconBg = MinimalGlassColors.PrimaryBlueLight,
            badgeText = badge,
            badgeColor = MinimalGlassColors.SuccessGreen,
            badgeBg = MinimalGlassColors.SuccessGreenLight,
            deviceTag = if (isConnected) controllerDeviceTag else "No gamepad connected (USB-C / Bluetooth)",
            onClick = onClick
        )
    }
}

@Composable
private fun SensitivityConverterCard(isCompact: Boolean = false, onClick: () -> Unit) {
    if (isCompact) {
        CompactOptionCard(
            title = "Sensitivity Converter",
            icon = Icons.Default.Tune,
            iconTint = MinimalGlassColors.PrimaryBlue,
            iconBg = MinimalGlassColors.PrimaryBlueLight,
            deviceTag = "CODM • BGMI • Warzone • Delta",
            onClick = onClick
        )
    } else {
        GlassOptionCard(
            title = "Sensitivity Converter",
            subtitle = "Convert aim sensitivity between games",
            icon = Icons.Default.Tune,
            iconTint = MinimalGlassColors.PrimaryBlue,
            iconBg = MinimalGlassColors.PrimaryBlueLight,
            deviceTag = "Supports CODM, BGMI, Warzone Mobile, Delta Force",
            onClick = onClick
        )
    }
}

@Composable
private fun SystemSettingsGuideCard(isCompact: Boolean = false, onClick: () -> Unit) {
    if (isCompact) {
        CompactOptionCard(
            title = "System Settings Guide",
            icon = Icons.Default.Speed,
            iconTint = MinimalGlassColors.SuccessGreen,
            iconBg = MinimalGlassColors.SuccessGreenLight,
            deviceTag = "Refresh rate & gesture settings",
            onClick = onClick
        )
    } else {
        GlassOptionCard(
            title = "System Settings Guide",
            subtitle = "Refresh rate, animation scales, and gesture lag",
            icon = Icons.Default.Speed,
            iconTint = MinimalGlassColors.SuccessGreen,
            iconBg = MinimalGlassColors.SuccessGreenLight,
            deviceTag = "Display & developer settings shortcuts",
            onClick = onClick
        )
    }
}

@Composable
private fun ReactionTimeTestCard(isCompact: Boolean = false, onClick: () -> Unit) {
    if (isCompact) {
        CompactOptionCard(
            title = "Reaction Time Test",
            icon = Icons.Default.TrackChanges,
            iconTint = Color(0xFF00F5D4),
            iconBg = Color(0xFF0D2825),
            deviceTag = "Touch response test",
            onClick = onClick
        )
    } else {
        GlassOptionCard(
            title = "Reaction Time Test",
            subtitle = "Measure touch input latency and flick reaction",
            icon = Icons.Default.TrackChanges,
            iconTint = Color(0xFF00F5D4),
            iconBg = Color(0xFF0D2825),
            deviceTag = "Touch response benchmark",
            onClick = onClick
        )
    }
}

@Composable
private fun AudioDelaySyncCard(isCompact: Boolean = false, onClick: () -> Unit) {
    if (isCompact) {
        CompactOptionCard(
            title = "Audio Delay Sync",
            icon = Icons.Default.SyncAlt,
            iconTint = MinimalGlassColors.Purple,
            iconBg = MinimalGlassColors.PurpleLight,
            deviceTag = "Rhythm Games & Emulators",
            onClick = onClick
        )
    } else {
        GlassOptionCard(
            title = "Audio Delay Sync",
            subtitle = "Measure audio delay offset for rhythm games and emulators",
            icon = Icons.Default.SyncAlt,
            iconTint = MinimalGlassColors.Purple,
            iconBg = MinimalGlassColors.PurpleLight,
            deviceTag = "Timing offset for rhythm games & emulators",
            onClick = onClick
        )
    }
}

@Composable
private fun CategoryHeaderCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    iconBg: Color,
    itemCount: Int,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rotation by animateFloatAsState(targetValue = if (isExpanded) 180f else 0f, label = "chevron_rot")
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MinimalGlassColors.CardSurface)
            .border(
                1.dp,
                if (isExpanded) iconTint.copy(alpha = 0.35f) else MinimalGlassColors.GlassBorder,
                RoundedCornerShape(16.dp)
            )
            .clickable { onToggle() }
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = title,
                            style = MinimalTypography.headlineSmall.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp),
                            color = MinimalGlassColors.TextPrimary
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MinimalGlassColors.BackgroundSubtle)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "$itemCount items",
                                style = MinimalTypography.labelSmall.copy(fontSize = 10.sp),
                                color = MinimalGlassColors.TextMuted
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MinimalTypography.bodyMedium.copy(fontSize = 12.sp),
                        color = MinimalGlassColors.TextSecondary
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MinimalGlassColors.BackgroundSubtle),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = MinimalGlassColors.TextSecondary,
                    modifier = Modifier
                        .size(20.dp)
                        .rotate(rotation)
                )
            }
        }
    }
}

@Composable
private fun ViewModeIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) MinimalGlassColors.CardSurface else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isSelected) MinimalGlassColors.PrimaryBlue else MinimalGlassColors.TextMuted,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun DeviceSpecsCard(specs: DeviceSpecs) {
    var isExpanded by remember { mutableStateOf(false) }
    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "specs_chevron"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MinimalGlassColors.CardSurface)
            .border(1.dp, MinimalGlassColors.GlassBorder, RoundedCornerShape(14.dp))
            .clickable { isExpanded = !isExpanded }
            .animateContentSize()
            .padding(14.dp)
    ) {
        Column {
            // Header Row (Device Name, OS, Chevron)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.PhoneAndroid,
                    contentDescription = null,
                    tint = MinimalGlassColors.PrimaryBlue,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = specs.deviceName,
                    style = MinimalTypography.headlineSmall.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                    color = MinimalGlassColors.TextPrimary
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "Android ${specs.androidVersion}",
                    style = MinimalTypography.labelSmall.copy(fontSize = 11.sp),
                    color = MinimalGlassColors.TextMuted
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse specs" else "Expand specs",
                    tint = MinimalGlassColors.TextMuted,
                    modifier = Modifier
                        .size(18.dp)
                        .rotate(rotationAngle)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Summary Grid (Always visible)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SpecItem("Display", "${specs.refreshRateHz.toInt()} Hz (${specs.screenResolution})")
                SpecItem("RAM", specs.totalRamGb)
                SpecItem("Chipset", specs.chipset)
            }

            // Expanded Hardware Telemetry (Detailed Android Specs)
            if (isExpanded) {
                Spacer(modifier = Modifier.height(14.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MinimalGlassColors.GlassBorderSubtle)
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Section 1: SoC & Architecture
                ExpandedSpecsSectionTitle("COMPUTE & ARCHITECTURE")
                Spacer(modifier = Modifier.height(6.dp))
                ExpandedSpecRow("SoC Platform", specs.chipset)
                ExpandedSpecRow("CPU Architecture", "${specs.cpuCores} Cores (${specs.supportedAbis})")
                ExpandedSpecRow("Motherboard", specs.board)

                Spacer(modifier = Modifier.height(10.dp))

                // Section 2: Display & Graphics
                ExpandedSpecsSectionTitle("DISPLAY & GRAPHICS PIPELINE")
                Spacer(modifier = Modifier.height(6.dp))
                ExpandedSpecRow("Physical Resolution", specs.screenResolution)
                ExpandedSpecRow("Pixel Density", "${specs.screenDensityDpi} DPI")
                ExpandedSpecRow("Refresh Modes", specs.supportedRefreshRates)
                ExpandedSpecRow("Graphics API", specs.glEsVersion)

                Spacer(modifier = Modifier.height(10.dp))

                // Section 3: Memory & Runtime
                ExpandedSpecsSectionTitle("MEMORY & SYSTEM RUNTIME")
                Spacer(modifier = Modifier.height(6.dp))
                ExpandedSpecRow("Physical RAM", specs.totalRamGb)
                ExpandedSpecRow("Available Memory", specs.availableRamGb)
                ExpandedSpecRow("Android API Level", "API ${specs.apiLevel}")
            }
        }
    }
}

@Composable
private fun SpecItem(label: String, value: String) {
    Column {
        Text(text = label, style = MinimalTypography.labelSmall.copy(fontSize = 10.sp), color = MinimalGlassColors.TextMuted)
        Text(
            text = value,
            style = TelemetryFonts.Small.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp
            ),
            color = MinimalGlassColors.TextSecondary
        )
    }
}

@Composable
private fun ExpandedSpecsSectionTitle(title: String) {
    Text(
        text = title,
        style = MinimalTypography.labelSmall.copy(
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.6.sp
        ),
        color = MinimalGlassColors.PrimaryBlue
    )
}

@Composable
private fun ExpandedSpecRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MinimalTypography.bodyMedium.copy(fontSize = 12.sp),
            color = MinimalGlassColors.TextSecondary
        )
        Text(
            text = value,
            style = TelemetryFonts.Small.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            ),
            color = MinimalGlassColors.TextPrimary
        )
    }
}

