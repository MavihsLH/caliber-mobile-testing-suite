package com.gamer.calibration.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.gamer.calibration.ui.theme.MinimalGlassColors
import kotlin.math.max

@Composable
fun SparklineCanvas(
    data: List<Float>,
    modifier: Modifier = Modifier,
    lineColor: Color = MinimalGlassColors.PrimaryBlue,
    minY: Float? = null,
    maxY: Float? = null,
    showGrid: Boolean = true
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        if (width <= 0f || height <= 0f) return@Canvas

        if (showGrid) {
            val gridColor = MinimalGlassColors.GlassBorder.copy(alpha = 0.6f)
            drawLine(
                color = gridColor,
                start = Offset(0f, height * 0.5f),
                end = Offset(width, height * 0.5f),
                strokeWidth = 1f
            )
        }

        if (data.size < 2) return@Canvas

        val computedMin = minY ?: (data.minOrNull() ?: 0f)
        val computedMax = maxY ?: (data.maxOrNull() ?: 100f)
        val range = max(0.001f, computedMax - computedMin)

        val stepX = width / (data.size - 1).coerceAtLeast(1)

        val path = Path()
        val fillPath = Path()

        for (i in data.indices) {
            val normalizedY = 1f - ((data[i] - computedMin) / range).coerceIn(0f, 1f)
            val x = i * stepX
            val y = normalizedY * (height - 6f) + 3f

            if (i == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, height)
                fillPath.lineTo(x, y)
            } else {
                path.lineTo(x, y)
                fillPath.lineTo(x, y)
            }

            if (i == data.size - 1) {
                fillPath.lineTo(x, height)
                fillPath.close()
            }
        }

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    lineColor.copy(alpha = 0.15f),
                    Color.Transparent
                ),
                startY = 0f,
                endY = height
            )
        )

        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(
                width = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
        )
    }
}
