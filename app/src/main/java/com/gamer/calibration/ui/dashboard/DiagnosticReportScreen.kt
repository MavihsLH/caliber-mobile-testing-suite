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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.gamer.calibration.domain.model.DiagnosticReport
import com.gamer.calibration.domain.model.IssueSeverity
import com.gamer.calibration.ui.components.CyberButton
import com.gamer.calibration.ui.theme.MinimalGlassColors
import com.gamer.calibration.ui.theme.MinimalTypography
import com.gamer.calibration.ui.theme.TelemetryFonts

@Composable
fun DiagnosticReportScreen(
    report: DiagnosticReport,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val issues = report.getIssues()

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
                text = "Hardware Diagnostic Report",
                style = MinimalTypography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MinimalGlassColors.TextPrimary
            )
        }

        // Scrollable Portrait Content
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Overall Grade Hero Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MinimalGlassColors.CardSurface)
                    .border(1.dp, MinimalGlassColors.GlassBorder, RoundedCornerShape(16.dp))
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "OVERALL GAMING GRADE",
                            style = MinimalTypography.labelSmall.copy(fontSize = 10.sp),
                            color = MinimalGlassColors.TextMuted
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = when (report.overallGrade) {
                                "A+" -> "Esports Ready"
                                "A" -> "High Performance"
                                "B" -> "Standard Hardware"
                                "C" -> "Optimization Needed"
                                else -> "Calibration Pending"
                            },
                            style = MinimalTypography.displayMedium.copy(fontSize = 22.sp),
                            color = MinimalGlassColors.TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${report.completedCount} of 5 hardware lab tests completed.",
                            style = MinimalTypography.bodyMedium,
                            color = MinimalGlassColors.TextSecondary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(
                                when (report.overallGrade) {
                                    "A+", "A" -> MinimalGlassColors.SuccessGreenLight
                                    "B" -> MinimalGlassColors.PrimaryBlueLight
                                    else -> MinimalGlassColors.WarningAmberLight
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = report.overallGrade,
                            style = TelemetryFonts.Hero.copy(fontSize = 28.sp),
                            color = when (report.overallGrade) {
                                "A+", "A" -> MinimalGlassColors.SuccessGreen
                                "B" -> MinimalGlassColors.PrimaryBlue
                                else -> MinimalGlassColors.WarningAmber
                            }
                        )
                    }
                }
            }

            // Summary Grid of Completed Lab Metrics
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MinimalGlassColors.CardSurface)
                    .border(1.dp, MinimalGlassColors.GlassBorder, RoundedCornerShape(16.dp))
                    .padding(18.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "LAB METRICS SUMMARY",
                        style = MinimalTypography.labelSmall.copy(fontSize = 10.sp),
                        color = MinimalGlassColors.TextMuted
                    )

                    MetricSummaryRow(
                        title = "Touch Screen Polling",
                        value = if (report.touchCompleted) "${report.touchSamplingHz.toInt()} Hz (${report.touchMaxPointers} fingers)" else "Not tested",
                        isTested = report.touchCompleted
                    )

                    MetricSummaryRow(
                        title = "Gyroscope Zero-Drift",
                        value = if (report.motionCompleted) "${String.format("%.2f", report.motionDriftDegs)}°/s" else "Not tested",
                        isTested = report.motionCompleted
                    )

                    MetricSummaryRow(
                        title = "Audio Acoustic Delay",
                        value = if (report.audioCompleted) "${report.audioLatencyMs.toInt()} ms" else "Not tested",
                        isTested = report.audioCompleted
                    )

                    MetricSummaryRow(
                        title = "Hardware Triggers",
                        value = if (report.triggerCompleted) "${report.triggerLatencyMs} ms delay (${String.format("%.1f", report.triggerPeakCps)} CPS)" else "Not tested",
                        isTested = report.triggerCompleted
                    )

                    MetricSummaryRow(
                        title = "Stick Circularity Error",
                        value = if (report.controllerCompleted) "${String.format("%.1f", report.controllerCircularityErrorPercent)}% error" else "Not tested",
                        isTested = report.controllerCompleted
                    )
                }
            }

            // Findings & Optimization Tips
            if (issues.isNotEmpty()) {
                Text(
                    text = "HARDWARE FINDINGS & RECOMMENDATIONS",
                    style = MinimalTypography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                    color = MinimalGlassColors.TextPrimary
                )

                issues.forEach { issue ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MinimalGlassColors.CardSurface)
                            .border(1.dp, MinimalGlassColors.GlassBorder, RoundedCornerShape(14.dp))
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(
                                imageVector = when (issue.severity) {
                                    IssueSeverity.OPTIMAL -> Icons.Default.CheckCircle
                                    IssueSeverity.NOTICE -> Icons.Default.Info
                                    IssueSeverity.WARNING -> Icons.Default.Warning
                                },
                                contentDescription = null,
                                tint = when (issue.severity) {
                                    IssueSeverity.OPTIMAL -> MinimalGlassColors.SuccessGreen
                                    IssueSeverity.NOTICE -> MinimalGlassColors.PrimaryBlue
                                    IssueSeverity.WARNING -> MinimalGlassColors.WarningAmber
                                },
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = issue.title,
                                    style = MinimalTypography.headlineSmall.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                                    color = MinimalGlassColors.TextPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = issue.description,
                                    style = MinimalTypography.bodyMedium,
                                    color = MinimalGlassColors.TextSecondary
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MinimalGlassColors.BackgroundSubtle)
                                        .border(1.dp, MinimalGlassColors.GlassBorderSubtle, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 8.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.Top) {
                                        Icon(
                                            imageVector = Icons.Default.Tune,
                                            contentDescription = null,
                                            tint = MinimalGlassColors.PrimaryBlue,
                                            modifier = Modifier.size(15.dp).padding(top = 1.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = "RECOMMENDED ACTION",
                                                style = MinimalTypography.labelSmall.copy(
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    letterSpacing = 0.5.sp
                                                ),
                                                color = MinimalGlassColors.PrimaryBlue
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = issue.recommendation,
                                                style = MinimalTypography.bodyMedium.copy(
                                                    fontSize = 12.sp,
                                                    lineHeight = 17.sp,
                                                    color = MinimalGlassColors.TextSecondary
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }

        // Bottom Actions
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MinimalGlassColors.CardSurface)
                .border(1.dp, MinimalGlassColors.GlassBorder)
                .padding(16.dp)
        ) {
            val context = LocalContext.current
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                CyberButton(
                    text = "Share Hardware Telemetry Card",
                    onClick = {
                        val cardText = buildString {
                            appendLine("╔═══════════════════════════════════════╗")
                            appendLine("║  CALIBER HARDWARE LAB REPORT          ║")
                            appendLine("║  Overall Grade: ${report.overallGrade.padEnd(21)} ║")
                            appendLine("╠═══════════════════════════════════════╣")
                            appendLine("║ • Touch Digitizer : ${if (report.touchCompleted) "${report.touchSamplingHz.toInt()} Hz (${report.touchMaxPointers} fingers)".padEnd(17) else "Not tested".padEnd(17)} ║")
                            appendLine("║ • Gyroscope Drift : ${if (report.motionCompleted) "${String.format("%.2f", report.motionDriftDegs)}°/s".padEnd(17) else "Not tested".padEnd(17)} ║")
                            appendLine("║ • Audio Latency   : ${if (report.audioCompleted) "${report.audioLatencyMs.toInt()} ms".padEnd(17) else "Not tested".padEnd(17)} ║")
                            appendLine("║ • Trigger Latency : ${if (report.triggerCompleted) "${report.triggerLatencyMs} ms (${String.format("%.1f", report.triggerPeakCps)} CPS)".padEnd(17) else "Not tested".padEnd(17)} ║")
                            appendLine("║ • Stick Error     : ${if (report.controllerCompleted) "${String.format("%.1f", report.controllerCircularityErrorPercent)}%".padEnd(17) else "Not tested".padEnd(17)} ║")
                            appendLine("╠═══════════════════════════════════════╣")
                            appendLine("║ Caliber Hardware Lab                  ║")
                            appendLine("╚═══════════════════════════════════════╝")
                        }

                        val sendIntent = android.content.Intent().apply {
                            action = android.content.Intent.ACTION_SEND
                            putExtra(android.content.Intent.EXTRA_TEXT, cardText)
                            type = "text/plain"
                        }
                        val shareIntent = android.content.Intent.createChooser(sendIntent, "Share Hardware Telemetry")
                        context.startActivity(shareIntent)
                    },
                    accentColor = MinimalGlassColors.SuccessGreen,
                    filled = true,
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                )

                CyberButton(
                    text = "Back to Home",
                    onClick = onBack,
                    accentColor = MinimalGlassColors.PrimaryBlue,
                    filled = false,
                    modifier = Modifier.fillMaxWidth().height(44.dp)
                )
            }
        }
    }
}

@Composable
private fun MetricSummaryRow(title: String, value: String, isTested: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, style = MinimalTypography.bodyMedium, color = MinimalGlassColors.TextSecondary)
        Text(
            text = value,
            style = TelemetryFonts.Small.copy(
                fontSize = 12.sp,
                fontWeight = if (isTested) FontWeight.Bold else FontWeight.Normal
            ),
            color = if (isTested) MinimalGlassColors.TextPrimary else MinimalGlassColors.TextMuted
        )
    }
}
