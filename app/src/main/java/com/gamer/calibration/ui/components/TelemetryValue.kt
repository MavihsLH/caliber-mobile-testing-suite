package com.gamer.calibration.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamer.calibration.ui.theme.MinimalGlassColors
import com.gamer.calibration.ui.theme.MinimalTypography
import com.gamer.calibration.ui.theme.TelemetryFonts

@Composable
fun TelemetryValue(
    label: String,
    value: String,
    unit: String = "",
    valueColor: Color = MinimalGlassColors.TextPrimary,
    modifier: Modifier = Modifier,
    isLarge: Boolean = false,
    minWidth: Int = 0
) {
    Column(
        modifier = modifier.then(if (minWidth > 0) Modifier.widthIn(min = minWidth.dp) else Modifier)
    ) {
        Text(
            text = label.uppercase(),
            style = MinimalTypography.labelSmall.copy(fontSize = 10.sp),
            color = MinimalGlassColors.TextMuted,
            maxLines = 1
        )
        Spacer(modifier = Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                style = if (isLarge) TelemetryFonts.Large else TelemetryFonts.Medium,
                fontWeight = FontWeight.Bold,
                color = valueColor,
                maxLines = 1
            )
            if (unit.isNotEmpty()) {
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = unit,
                    style = TelemetryFonts.Small.copy(fontSize = 10.sp),
                    color = MinimalGlassColors.TextSecondary,
                    modifier = Modifier.alignByBaseline(),
                    maxLines = 1
                )
            }
        }
    }
}
