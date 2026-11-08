package com.droid.waveeffects.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun SpectrumVisualizer(
    magnitudes: FloatArray,
    waveform: FloatArray,
    isMasterEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(130.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp)
    ) {
        val barGradient = Brush.verticalGradient(
            colors = listOf(
                MaterialTheme.colorScheme.primary,
                MaterialTheme.colorScheme.secondary,
                MaterialTheme.colorScheme.tertiary
            )
        )
        val waveColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
        val inactiveColor = MaterialTheme.colorScheme.outlineVariant

        Canvas(modifier = Modifier.matchParentSize()) {
            val width = size.width
            val height = size.height

            // 1. Draw FFT Spectrum Bars
            val barCount = magnitudes.size.coerceAtLeast(1)
            val spacing = 6.dp.toPx()
            val totalSpacing = spacing * (barCount - 1)
            val barWidth = (width - totalSpacing) / barCount

            for (i in 0 until barCount) {
                val mag = if (isMasterEnabled) magnitudes[i].coerceIn(0.04f, 1.0f) else 0.04f
                val barHeight = (mag * height * 0.85f).coerceAtLeast(4.dp.toPx())
                val x = i * (barWidth + spacing)
                val y = height - barHeight

                drawRoundRect(
                    brush = if (isMasterEnabled) barGradient else Brush.linearGradient(listOf(inactiveColor, inactiveColor)),
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )
            }

            // 2. Draw Oscilloscope Waveform overlay
            if (isMasterEnabled && waveform.isNotEmpty()) {
                val wavePath = Path()
                val stepX = width / (waveform.size - 1)
                wavePath.moveTo(0f, height / 2f + waveform[0] * (height / 3f))

                for (i in 1 until waveform.size) {
                    val x = i * stepX
                    val y = height / 2f + waveform[i] * (height / 3f)
                    wavePath.lineTo(x, y)
                }

                drawPath(
                    path = wavePath,
                    color = waveColor,
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }
    }
}
