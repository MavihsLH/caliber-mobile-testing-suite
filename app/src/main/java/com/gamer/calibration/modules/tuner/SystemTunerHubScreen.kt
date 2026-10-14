package com.gamer.calibration.modules.tuner

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamer.calibration.ui.components.CyberButton
import com.gamer.calibration.ui.theme.MinimalGlassColors
import com.gamer.calibration.ui.theme.MinimalTypography
import com.gamer.calibration.ui.theme.TelemetryFonts

data class TuningRecommendation(
    val id: String,
    val title: String,
    val category: String,
    val whyItMatters: String,
    val instructions: String,
    val icon: ImageVector,
    val intentAction: String
)

@Composable
fun SystemTunerHubScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember {
        val oldPrefs = context.getSharedPreferences("gamersense_tuning_prefs", Context.MODE_PRIVATE)
        val newPrefs = context.getSharedPreferences("caliber_tuning_prefs", Context.MODE_PRIVATE)
        if (newPrefs.all.isEmpty() && oldPrefs.all.isNotEmpty()) {
            val editor = newPrefs.edit()
            for ((k, v) in oldPrefs.all) {
                if (v is Boolean) editor.putBoolean(k, v)
            }
            editor.apply()
        }
        newPrefs
    }

    val recommendations = remember {
        listOf(
            TuningRecommendation(
                id = "peak_refresh_rate",
                title = "Force Peak Display Refresh Rate",
                category = "DISPLAY HARDWARE",
                whyItMatters = "Android dynamically throttles screens from 120Hz down to 60Hz during idle periods to save battery. In games, this causes stutter and uneven frame delivery.",
                instructions = "Go to Display settings > Refresh Rate. Set to High (120Hz/144Hz) rather than Auto/Dynamic.",
                icon = Icons.Default.Tv,
                intentAction = Settings.ACTION_DISPLAY_SETTINGS
            ),
            TuningRecommendation(
                id = "developer_animation_scale",
                title = "Reduce System Animation Scales (0.5x)",
                category = "OS COMPOSITOR",
                whyItMatters = "Default 1.0x window transitions cause visual hitching when switching apps or pulling down notification shades mid-fight.",
                instructions = "In Developer Options, change Window, Transition, and Animator duration scale from 1.0x to 0.5x or Off.",
                icon = Icons.Default.Speed,
                intentAction = Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS
            ),
            TuningRecommendation(
                id = "touch_hold_delay",
                title = "Shorten Touch & Hold Delay",
                category = "INPUT DIGITIZER",
                whyItMatters = "Accessibility hold delays can cause rapid double-tap gestures to be delayed by the OS touch dispatcher.",
                instructions = "Go to Accessibility > Timing controls / Interaction. Set 'Touch and hold delay' to Short (0.5s or faster).",
                icon = Icons.Default.TouchApp,
                intentAction = Settings.ACTION_ACCESSIBILITY_SETTINGS
            ),
            TuningRecommendation(
                id = "disable_3finger_gesture",
                title = "Disable 3-Finger Screenshot Gestures",
                category = "TOUCH PIPELINE",
                whyItMatters = "On Xiaomi, Realme, OnePlus, and Samsung devices, three-finger gesture listeners hold finger packets for 30-50ms to check for swipe gestures before passing touch to games.",
                instructions = "Search 'Gestures' in Settings and turn off 'Three-finger swipe for screenshot' to eliminate 3-finger aim dropouts.",
                icon = Icons.Default.Tune,
                intentAction = Settings.ACTION_SETTINGS
            ),
            TuningRecommendation(
                id = "bluetooth_codec_low_latency",
                title = "Optimize Bluetooth Audio Codec",
                category = "AUDIO LATENCY",
                whyItMatters = "Default SBC codec introduces up to 250ms delay. Switching to aptX Adaptive or LDAC reduces delay by over 60%.",
                instructions = "Open Bluetooth device settings or Developer Options > Bluetooth Audio Codec. Enable Gaming Mode on your earbuds.",
                icon = Icons.Default.Headphones,
                intentAction = Settings.ACTION_BLUETOOTH_SETTINGS
            )
        )
    }

    val checkedStates = remember {
        mutableStateMapOf<String, Boolean>().apply {
            recommendations.forEach { rec ->
                this[rec.id] = prefs.getBoolean("rec_${rec.id}", false)
            }
        }
    }

    val completedCount = checkedStates.values.count { it }
    val progressPercent = if (recommendations.isNotEmpty()) {
        (completedCount.toFloat() / recommendations.size.toFloat())
    } else 0f

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MinimalGlassColors.Background)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MinimalGlassColors.CardSurface)
                .border(1.dp, MinimalGlassColors.GlassBorder)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MinimalGlassColors.TextPrimary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "System Settings Guide",
                    style = MinimalTypography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MinimalGlassColors.TextPrimary
                )
                Text(
                    text = "Display, touch, and audio settings shortcuts",
                    style = MinimalTypography.labelSmall.copy(fontSize = 11.sp),
                    color = MinimalGlassColors.TextMuted
                )
            }
        }

        // Scrollable Content
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Optimization Readiness Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MinimalGlassColors.CardSurface)
                    .border(1.dp, MinimalGlassColors.GlassBorder, RoundedCornerShape(16.dp))
                    .padding(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "OPTIMIZATION PROGRESS",
                                style = MinimalTypography.labelSmall.copy(letterSpacing = 1.sp),
                                color = MinimalGlassColors.TextMuted
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (completedCount == recommendations.size) "All ${recommendations.size} optimizations configured" else "$completedCount of ${recommendations.size} optimizations configured",
                                style = MinimalTypography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = MinimalGlassColors.TextSecondary
                            )
                        }

                        Text(
                            text = "${(progressPercent * 100).toInt()}%",
                            style = TelemetryFonts.Hero.copy(fontSize = 32.sp),
                            color = if (progressPercent >= 0.8f) MinimalGlassColors.SuccessGreen else MinimalGlassColors.PrimaryBlue
                        )
                    }

                    LinearProgressIndicator(
                        progress = { progressPercent },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = if (progressPercent >= 0.8f) MinimalGlassColors.SuccessGreen else MinimalGlassColors.PrimaryBlue,
                        trackColor = MinimalGlassColors.BackgroundSubtle
                    )

                    Text(
                        text = "Hardware-level optimizations to eradicate touch sample buffering, frame throttling, and audio lag.",
                        style = MinimalTypography.bodyMedium.copy(fontSize = 12.sp),
                        color = MinimalGlassColors.TextSecondary
                    )
                }
            }

            // Checklist Items
            recommendations.forEach { rec ->
                val isChecked = checkedStates[rec.id] ?: false
                TuningItemCard(
                    recommendation = rec,
                    isChecked = isChecked,
                    onCheckedChange = { checked ->
                        checkedStates[rec.id] = checked
                        prefs.edit().putBoolean("rec_${rec.id}", checked).apply()
                    },
                    onOpenSettings = {
                        try {
                            val intent = Intent(rec.intentAction).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            try {
                                context.startActivity(Intent(Settings.ACTION_SETTINGS))
                            } catch (ignored: Exception) {
                                Toast.makeText(context, "Could not open system settings directly", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun TuningItemCard(
    recommendation: TuningRecommendation,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onOpenSettings: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MinimalGlassColors.CardSurface)
            .border(
                1.dp,
                if (isChecked) MinimalGlassColors.SuccessGreen.copy(alpha = 0.4f) else MinimalGlassColors.GlassBorder,
                RoundedCornerShape(14.dp)
            )
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isChecked) MinimalGlassColors.SuccessGreenLight else MinimalGlassColors.PrimaryBlueLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = recommendation.icon,
                        contentDescription = null,
                        tint = if (isChecked) MinimalGlassColors.SuccessGreen else MinimalGlassColors.PrimaryBlue,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = recommendation.category,
                        style = MinimalTypography.labelSmall.copy(fontSize = 10.sp),
                        color = MinimalGlassColors.TextMuted
                    )
                    Text(
                        text = recommendation.title,
                        style = MinimalTypography.headlineSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
                        color = MinimalGlassColors.TextPrimary
                    )
                }

                Checkbox(
                    checked = isChecked,
                    onCheckedChange = onCheckedChange,
                    colors = CheckboxDefaults.colors(
                        checkedColor = MinimalGlassColors.SuccessGreen,
                        uncheckedColor = MinimalGlassColors.GlassBorder
                    )
                )
            }

            Text(
                text = recommendation.whyItMatters,
                style = MinimalTypography.bodyMedium.copy(fontSize = 12.sp),
                color = MinimalGlassColors.TextSecondary
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MinimalGlassColors.BackgroundSubtle)
                    .padding(10.dp)
            ) {
                Text(
                    text = "How: ${recommendation.instructions}",
                    style = MinimalTypography.bodyMedium.copy(fontSize = 11.sp, color = MinimalGlassColors.TextMuted)
                )
            }

            CyberButton(
                text = "Open Settings",
                onClick = onOpenSettings,
                modifier = Modifier.fillMaxWidth(),
                accentColor = MinimalGlassColors.PrimaryBlue,
                filled = false
            )
        }
    }
}
