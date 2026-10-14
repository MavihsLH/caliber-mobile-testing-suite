package com.gamer.calibration.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamer.calibration.ui.theme.MinimalGlassColors
import com.gamer.calibration.ui.theme.MinimalTypography

@Composable
fun GlassOptionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color = MinimalGlassColors.PrimaryBlue,
    iconBg: Color = MinimalGlassColors.PrimaryBlueLight,
    badgeText: String? = null,
    badgeColor: Color = MinimalGlassColors.TextMuted,
    badgeBg: Color = MinimalGlassColors.BackgroundSubtle,
    deviceTag: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
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
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Top Row: Icon on Left, Pill Badge on Right (Uniform Across All Cards)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Dedicated Top-Right Pill Badge (Renders only when calibrated / active)
                if (!badgeText.isNullOrEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(badgeBg)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = badgeText,
                            style = MinimalTypography.labelSmall.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = badgeColor,
                            maxLines = 1
                        )
                    }
                } else {
                    // Lightweight uncalibrated indicator: subtle neutral dash
                    Text(
                        text = "—",
                        style = MinimalTypography.labelSmall.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = MinimalGlassColors.TextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Middle: Card Title
            Text(
                text = title,
                style = MinimalTypography.headlineSmall.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp),
                color = MinimalGlassColors.TextPrimary,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Subtitle Description & Forward Arrow
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = subtitle,
                    style = MinimalTypography.bodyMedium,
                    color = MinimalGlassColors.TextSecondary,
                    modifier = Modifier.weight(1f),
                    maxLines = 2
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = null,
                    tint = MinimalGlassColors.TextMuted,
                    modifier = Modifier.size(12.dp)
                )
            }

            // Optional Connected External Device Tag
            if (!deviceTag.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MinimalGlassColors.BackgroundSubtle)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(iconTint)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = deviceTag,
                            style = MinimalTypography.labelSmall.copy(fontSize = 11.sp),
                            color = MinimalGlassColors.TextPrimary,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CompactOptionCard(
    title: String,
    icon: ImageVector,
    iconTint: Color = MinimalGlassColors.PrimaryBlue,
    iconBg: Color = MinimalGlassColors.PrimaryBlueLight,
    badgeText: String? = null,
    badgeColor: Color = MinimalGlassColors.TextMuted,
    badgeBg: Color = MinimalGlassColors.BackgroundSubtle,
    deviceTag: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MinimalGlassColors.CardSurface)
            .border(1.dp, MinimalGlassColors.GlassBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = title,
                        style = MinimalTypography.headlineSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
                        color = MinimalGlassColors.TextPrimary,
                        maxLines = 1
                    )
                    if (!deviceTag.isNullOrEmpty()) {
                        Text(
                            text = deviceTag,
                            style = MinimalTypography.labelSmall.copy(fontSize = 10.sp),
                            color = MinimalGlassColors.TextMuted,
                            maxLines = 1
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!badgeText.isNullOrEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(badgeBg)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = badgeText,
                            style = MinimalTypography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = badgeColor,
                            maxLines = 1
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = null,
                    tint = MinimalGlassColors.TextMuted,
                    modifier = Modifier.size(11.dp)
                )
            }
        }
    }
}
