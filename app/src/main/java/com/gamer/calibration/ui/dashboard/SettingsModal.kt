package com.gamer.calibration.ui.dashboard

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.res.painterResource
import com.gamer.calibration.R
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamer.calibration.ui.theme.MinimalGlassColors
import com.gamer.calibration.ui.theme.MinimalTypography
import com.gamer.calibration.ui.theme.TelemetryFonts

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsModal(
    isDarkTheme: Boolean,
    onThemeToggle: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("apex_calibrate_prefs", Context.MODE_PRIVATE) }
    val dashboardPrefs = remember { context.getSharedPreferences("caliber_dashboard_prefs", Context.MODE_PRIVATE) }
    var hapticsEnabled by remember { mutableStateOf(prefs.getBoolean("haptics_enabled", true)) }
    var autoTareEnabled by remember { mutableStateOf(prefs.getBoolean("auto_tare_enabled", true)) }
    var keepScreenAwake by remember { mutableStateOf(prefs.getBoolean("keep_screen_awake", true)) }
    val hasDonated by remember { mutableStateOf(dashboardPrefs.getBoolean("has_donated", false)) }
    var displayAdEnabled by remember { mutableStateOf(dashboardPrefs.getBoolean("display_ad_enabled", true)) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MinimalGlassColors.CardSurface,
        scrimColor = Color.Black.copy(alpha = 0.55f),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 6.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(MinimalGlassColors.TextMuted.copy(alpha = 0.35f))
            )
        },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Settings",
                    style = MinimalTypography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    ),
                    color = MinimalGlassColors.TextPrimary
                )

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MinimalGlassColors.BackgroundSubtle)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close settings",
                        tint = MinimalGlassColors.TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // SECTION 1: THEME TOGGLE (Strictly Sun & Moon icons only - zero text)
            Text(
                text = "APPEARANCE",
                style = MinimalTypography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                ),
                color = MinimalGlassColors.TextMuted
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MinimalGlassColors.BackgroundSubtle)
                    .border(1.dp, MinimalGlassColors.GlassBorderSubtle, RoundedCornerShape(12.dp))
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sun Icon Toggle (Light Mode)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (!isDarkTheme) MinimalGlassColors.CardSurface else Color.Transparent)
                        .border(
                            width = if (!isDarkTheme) 1.dp else 0.dp,
                            color = if (!isDarkTheme) MinimalGlassColors.GlassBorder else Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { onThemeToggle(false) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LightMode,
                        contentDescription = "Light mode",
                        tint = if (!isDarkTheme) MinimalGlassColors.PrimaryBlue else MinimalGlassColors.TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Moon Icon Toggle (Dark Mode)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isDarkTheme) MinimalGlassColors.CardSurface else Color.Transparent)
                        .border(
                            width = if (isDarkTheme) 1.dp else 0.dp,
                            color = if (isDarkTheme) MinimalGlassColors.GlassBorder else Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { onThemeToggle(true) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DarkMode,
                        contentDescription = "Dark mode",
                        tint = if (isDarkTheme) MinimalGlassColors.PrimaryBlue else MinimalGlassColors.TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // SECTION 2: CALIBRATION PREFERENCES
            Text(
                text = "CALIBRATION PREFERENCES",
                style = MinimalTypography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                ),
                color = MinimalGlassColors.TextMuted
            )

            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MinimalGlassColors.BackgroundSubtle)
                    .border(1.dp, MinimalGlassColors.GlassBorderSubtle, RoundedCornerShape(14.dp))
            ) {
                SettingsSwitchRow(
                    title = "Haptic Feedback",
                    subtitle = "Vibration impulse on sensor lock & touch hits",
                    checked = hapticsEnabled,
                    onCheckedChange = { checked ->
                        hapticsEnabled = checked
                        prefs.edit().putBoolean("haptics_enabled", checked).apply()
                    }
                )

                HairlineDivider()

                SettingsSwitchRow(
                    title = "Auto-Tare Sensors",
                    subtitle = "Zero resting IMU drift on test start",
                    checked = autoTareEnabled,
                    onCheckedChange = { checked ->
                        autoTareEnabled = checked
                        prefs.edit().putBoolean("auto_tare_enabled", checked).apply()
                    }
                )



                HairlineDivider()

                SettingsSwitchRow(
                    title = "Keep Screen Awake",
                    subtitle = "Prevents display dimming or sleep during calibration",
                    checked = keepScreenAwake,
                    onCheckedChange = { checked ->
                        keepScreenAwake = checked
                        prefs.edit().putBoolean("keep_screen_awake", checked).apply()
                    }
                )

                HairlineDivider()

                SettingsSwitchRow(
                    title = "Display AD",
                    subtitle = if (hasDonated) "Thank you for your support! Ads are permanently removed." else "Supports me by keeping it on (or donate to remove)",
                    checked = displayAdEnabled && !hasDonated,
                    enabled = !hasDonated,
                    onCheckedChange = { checked ->
                        displayAdEnabled = checked
                        dashboardPrefs.edit()
                            .putBoolean("display_ad_enabled", checked)
                            .remove("ads_hidden_until")
                            .apply()
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Caliber Brand Card & Version
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MinimalGlassColors.BackgroundSubtle)
                    .border(1.dp, MinimalGlassColors.GlassBorderSubtle, RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.caliber_emblem),
                            contentDescription = "Caliber Emblem",
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(6.dp))
                        )
                        Column {
                            Text(
                                text = "Caliber",
                                style = MinimalTypography.headlineSmall.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
                                color = MinimalGlassColors.TextPrimary
                            )
                            Text(
                                text = "Calibration & Hardware Lab",
                                style = MinimalTypography.labelSmall.copy(fontSize = 10.sp),
                                color = MinimalGlassColors.TextMuted
                            )
                        }
                    }

                    Text(
                        text = "v1.0.0",
                        style = TelemetryFonts.Small.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold),
                        color = MinimalGlassColors.PrimaryBlue
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MinimalTypography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                ),
                color = if (enabled) MinimalGlassColors.TextPrimary else MinimalGlassColors.TextMuted
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MinimalTypography.labelSmall.copy(fontSize = 11.sp),
                color = MinimalGlassColors.TextMuted
            )
        }

        Switch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = MinimalGlassColors.PrimaryBlue,
                uncheckedThumbColor = MinimalGlassColors.TextMuted,
                uncheckedTrackColor = MinimalGlassColors.CardSurface,
                uncheckedBorderColor = MinimalGlassColors.GlassBorder
            )
        )
    }
}

@Composable
private fun HairlineDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MinimalGlassColors.GlassBorderSubtle)
    )
}
