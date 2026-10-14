package com.gamer.calibration.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamer.calibration.ui.theme.MinimalGlassColors
import com.gamer.calibration.ui.theme.MinimalTypography

@Composable
fun LabTopBar(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    onRotate: () -> Unit,
    onReset: (() -> Unit)? = null,
    onFinish: () -> Unit,
    badges: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    actionButton: (@Composable () -> Unit)? = null
) {
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(MinimalGlassColors.CardSurface)
            .border(1.dp, MinimalGlassColors.GlassBorder)
            .statusBarsPadding()
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        if (isLandscape) {
            // Single Wide Row in Landscape
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MinimalGlassColors.TextPrimary)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(text = title, style = MinimalTypography.headlineSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 15.sp), color = MinimalGlassColors.TextPrimary)
                        Text(text = subtitle, style = MinimalTypography.labelSmall.copy(fontSize = 11.sp), color = MinimalGlassColors.TextSecondary)
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    badges()

                    IconButton(onClick = onRotate, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.ScreenRotation, contentDescription = "Rotate Screen", tint = MinimalGlassColors.TextSecondary)
                    }

                    if (onReset != null) {
                        IconButton(onClick = onReset, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reset Stats", tint = MinimalGlassColors.TextSecondary)
                        }
                    }

                    if (actionButton != null) {
                        actionButton()
                    } else {
                        CyberButton(
                            text = "Finish & Save",
                            onClick = onFinish,
                            accentColor = MinimalGlassColors.PrimaryBlue,
                            filled = true
                        )
                    }
                }
            }
        } else {
            // Adaptive 2-Row Stack in Portrait (Never overlaps!)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MinimalGlassColors.TextPrimary)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Column {
                            Text(text = title, style = MinimalTypography.headlineSmall.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp), color = MinimalGlassColors.TextPrimary, maxLines = 1)
                            Text(text = subtitle, style = MinimalTypography.labelSmall.copy(fontSize = 10.sp), color = MinimalGlassColors.TextSecondary, maxLines = 1)
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(onClick = onRotate, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.ScreenRotation, contentDescription = "Rotate Screen", tint = MinimalGlassColors.TextSecondary, modifier = Modifier.size(18.dp))
                        }

                        if (onReset != null) {
                            IconButton(onClick = onReset, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Refresh, contentDescription = "Reset Stats", tint = MinimalGlassColors.TextSecondary, modifier = Modifier.size(18.dp))
                            }
                        }

                        if (actionButton != null) {
                            actionButton()
                        } else {
                            CyberButton(
                                text = "Save",
                                onClick = onFinish,
                                accentColor = MinimalGlassColors.PrimaryBlue,
                                filled = true
                            )
                        }
                    }
                }

                // Row 2: Dedicated, Clean Telemetry Badges Strip
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    badges()
                }
            }
        }
    }
}

@Composable
fun LabBadge(
    label: String,
    value: String,
    unit: String = "",
    valueColor: Color = MinimalGlassColors.TextPrimary,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MinimalGlassColors.BackgroundSubtle)
            .border(1.dp, MinimalGlassColors.GlassBorderSubtle, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "$label: ",
                style = MinimalTypography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Normal),
                color = MinimalGlassColors.TextMuted
            )
            Text(
                text = value,
                style = MinimalTypography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                color = valueColor
            )
            if (unit.isNotEmpty()) {
                Text(
                    text = " $unit",
                    style = MinimalTypography.labelSmall.copy(fontSize = 9.sp),
                    color = MinimalGlassColors.TextSecondary
                )
            }
        }
    }
}
