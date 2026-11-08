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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.droid.waveeffects.domain.EqualizerBand

@Composable
fun EqualizerCurve(
    bands: List<EqualizerBand>,
    isMasterEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp)
    ) {
        val activeCurveColor = MaterialTheme.colorScheme.primary
        val inactiveCurveColor = MaterialTheme.colorScheme.outline
        val gridLineColor = MaterialTheme.colorScheme.outlineVariant
        val baselineColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)

        Canvas(modifier = Modifier.matchParentSize()) {
            val width = size.width
            val height = size.height

            // 1. Draw Grid Lines (-12dB, -6dB, 0dB, +6dB, +12dB)
            val dbSteps = listOf(12f, 6f, 0f, -6f, -12f)
            dbSteps.forEach { db ->
                val y = height / 2f - (db / 12f) * (height / 2f * 0.85f)
                val isZero = db == 0f
                drawLine(
                    color = if (isZero) baselineColor else gridLineColor,
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = if (isZero) 1.5f else 1.0f,
                    pathEffect = if (isZero) PathEffect.dashPathEffect(floatArrayOf(10f, 8f)) else null
                )
            }

            if (bands.isEmpty()) return@Canvas

            // 2. Compute Band Control Points (x, y)
            val paddingX = width * 0.05f
            val usableWidth = width - 2 * paddingX
            val stepX = usableWidth / (bands.size - 1)

            val points = bands.mapIndexed { index, band ->
                val x = paddingX + index * stepX
                val normalizedGain = (band.gainDb / 12f).coerceIn(-1f, 1f)
                val y = height / 2f - normalizedGain * (height / 2f * 0.85f)
                Offset(x, y)
            }

            // 3. Build Smooth Bézier Spline Path
            val curvePath = Path()
            curvePath.moveTo(points.first().x, points.first().y)

            for (i in 0 until points.size - 1) {
                val p0 = if (i > 0) points[i - 1] else points[i]
                val p1 = points[i]
                val p2 = points[i + 1]
                val p3 = if (i + 2 < points.size) points[i + 2] else p2

                // Catmull-Rom to Cubic Bézier control points
                val cp1x = p1.x + (p2.x - p0.x) / 6f
                val cp1y = p1.y + (p2.y - p0.y) / 6f
                val cp2x = p2.x - (p3.x - p1.x) / 6f
                val cp2y = p2.y - (p3.y - p1.y) / 6f

                curvePath.cubicTo(cp1x, cp1y, cp2x, cp2y, p2.x, p2.y)
            }

            // 4. Draw Gradient Area under Curve if Master is Enabled
            if (isMasterEnabled) {
                val fillPath = Path()
                fillPath.addPath(curvePath)
                fillPath.lineTo(points.last().x, height)
                fillPath.lineTo(points.first().x, height)
                fillPath.close()

                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            activeCurveColor.copy(alpha = 0.35f),
                            activeCurveColor.copy(alpha = 0.05f),
                            Color.Transparent
                        ),
                        startY = 0f,
                        endY = height
                    )
                )
            }

            // 5. Draw Curve Stroke
            drawPath(
                path = curvePath,
                color = if (isMasterEnabled) activeCurveColor else inactiveCurveColor,
                style = Stroke(width = 3.5f, cap = StrokeCap.Round)
            )

            // 6. Draw Control Point Dots & Halos
            points.forEachIndexed { index, point ->
                val isGainNonZero = bands[index].gainDb != 0f
                if (isMasterEnabled && isGainNonZero) {
                    drawCircle(
                        color = activeCurveColor.copy(alpha = 0.25f),
                        radius = 8.dp.toPx(),
                        center = point
                    )
                }
                drawCircle(
                    color = if (isMasterEnabled) {
                        if (isGainNonZero) Color.White else activeCurveColor
                    } else {
                        inactiveCurveColor
                    },
                    radius = 4.dp.toPx(),
                    center = point
                )
            }
        }
    }
}
