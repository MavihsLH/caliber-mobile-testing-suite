package com.gamer.calibration.ui.dashboard

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamer.calibration.ui.components.CyberButton
import com.gamer.calibration.ui.theme.MinimalGlassColors
import com.gamer.calibration.ui.theme.MinimalTypography
import com.gamer.calibration.ui.theme.TelemetryFonts

data class TestDetailConfig(
    val module: CalibrationModule,
    val title: String,
    val category: String,
    val icon: ImageVector,
    val iconTint: Color,
    val iconBg: Color,
    val whatItIs: String,
    val whyItMatters: String,
    val instructions: List<String>,
    val benchmarks: List<Pair<String, String>>
)

@Composable
fun TestDetailScreen(
    config: TestDetailConfig,
    onBack: () -> Unit,
    onStartTest: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MinimalGlassColors.Background)
    ) {
        // Top Header
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
            Text(
                text = "Calibration Briefing",
                style = MinimalTypography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MinimalGlassColors.TextPrimary
            )
        }

        // Single Cohesive Document View
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(18.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MinimalGlassColors.CardSurface)
                    .border(1.dp, MinimalGlassColors.GlassBorder, RoundedCornerShape(16.dp))
                    .padding(22.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    // Header / Hero Section
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(config.iconBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = config.icon,
                                contentDescription = null,
                                tint = config.iconTint,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column {
                            Text(
                                text = config.category.uppercase(),
                                style = MinimalTypography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp
                                ),
                                color = config.iconTint
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = config.title,
                                style = MinimalTypography.displayMedium.copy(
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MinimalGlassColors.TextPrimary
                            )
                        }
                    }

                    // Hairline Divider
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MinimalGlassColors.GlassBorderSubtle)
                    )

                    // Diagnostic Rationale Section
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Column {
                            Text(
                                text = "DIAGNOSTIC PURPOSE",
                                style = MinimalTypography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.6.sp
                                ),
                                color = MinimalGlassColors.TextMuted
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = config.whatItIs,
                                style = MinimalTypography.bodyLarge.copy(
                                    fontSize = 14.sp,
                                    lineHeight = 22.sp
                                ),
                                color = MinimalGlassColors.TextPrimary
                            )
                        }

                        Column {
                            Text(
                                text = "ESPORTS IMPACT & SENSITIVITY",
                                style = MinimalTypography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.6.sp
                                ),
                                color = MinimalGlassColors.TextMuted
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = config.whyItMatters,
                                style = MinimalTypography.bodyMedium.copy(
                                    fontSize = 13.sp,
                                    lineHeight = 20.sp
                                ),
                                color = MinimalGlassColors.TextSecondary
                            )
                        }
                    }

                    // Hairline Divider
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MinimalGlassColors.GlassBorderSubtle)
                    )

                    // Testing Protocol (Numbered Step-by-Step)
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "CALIBRATION PROTOCOL",
                            style = MinimalTypography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.6.sp
                            ),
                            color = MinimalGlassColors.TextMuted
                        )

                        config.instructions.forEachIndexed { index, step ->
                            Row(verticalAlignment = Alignment.Top) {
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(MinimalGlassColors.PrimaryBlueLight),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${index + 1}",
                                        style = MinimalTypography.labelSmall.copy(
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = MinimalGlassColors.PrimaryBlue
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = step,
                                    style = MinimalTypography.bodyLarge.copy(
                                        fontSize = 13.sp,
                                        lineHeight = 20.sp
                                    ),
                                    color = MinimalGlassColors.TextPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Hairline Divider
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MinimalGlassColors.GlassBorderSubtle)
                    )

                    // Performance Benchmarks Table
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "PERFORMANCE BENCHMARKS",
                            style = MinimalTypography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.6.sp
                            ),
                            color = MinimalGlassColors.TextMuted
                        )

                        config.benchmarks.forEachIndexed { i, (tier, description) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = tier,
                                    style = MinimalTypography.bodyMedium.copy(fontSize = 13.sp),
                                    color = MinimalGlassColors.TextSecondary
                                )
                                Text(
                                    text = description,
                                    style = TelemetryFonts.Medium.copy(
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MinimalGlassColors.TextPrimary
                                )
                            }
                            if (i < config.benchmarks.size - 1) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(0.5.dp)
                                        .background(MinimalGlassColors.GlassBorderSubtle)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Clean, Authoritative Fixed Bottom Dock
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MinimalGlassColors.CardSurface)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MinimalGlassColors.GlassBorder)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 18.dp, vertical = 14.dp)
            ) {
                CyberButton(
                    text = "Start Calibration",
                    onClick = onStartTest,
                    accentColor = MinimalGlassColors.PrimaryBlue,
                    filled = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                )
            }
        }
    }
}
