package com.equinox.audioeq.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.equinox.audioeq.model.EqState
import com.equinox.audioeq.model.ResponseCurve
import com.equinox.audioeq.ui.theme.CurveFillAlpha
import com.equinox.audioeq.ui.theme.CurveGridLine
import com.equinox.audioeq.ui.theme.CurveZeroLine
import java.util.Locale
import kotlin.math.ln
import kotlin.math.pow

private const val DB_RANGE = 12f
private const val SAMPLE_POINTS = 120
private val DB_GRID = listOf(-12f, -6f, 0f, 6f, 12f)
private val FREQ_GRID = listOf(50f, 100f, 250f, 500f, 1000f, 2500f, 5000f, 10000f, 20000f)

private val LOG_SPAN = ln(ResponseCurve.MAX_HZ / ResponseCurve.MIN_HZ)
private fun freqToFraction(hz: Float) = ln(hz / ResponseCurve.MIN_HZ) / LOG_SPAN
private fun fractionToFreq(f: Float) = ResponseCurve.MIN_HZ * (ResponseCurve.MAX_HZ / ResponseCurve.MIN_HZ).pow(f)

private fun formatHz(hz: Float): String = if (hz >= 1000f) {
    String.format(Locale.US, if (hz >= 10_000f) "%.0fk" else "%.1fk", hz / 1000f)
} else {
    String.format(Locale.US, "%.0f", hz)
}

/** 20 Hz – 20 kHz log-frequency response plot with pointer/tap inspection. */
@Composable
fun EqVisualizerView(state: EqState, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(color = colors.onSurfaceVariant, fontSize = 10.sp)
    var inspectFraction by remember { mutableStateOf<Float?>(null) }

    val frequencies = remember { ResponseCurve.logFrequencies(SAMPLE_POINTS) }
    val gains = remember(state) { frequencies.map { ResponseCurve.totalGainDb(it, state) } }

    Box(
        modifier
            .fillMaxWidth()
            .height(240.dp)
            .clip(MaterialTheme.shapes.large)
            .background(colors.surfaceContainerHigh),
    ) {
        Canvas(
            Modifier
                .fillMaxSize()
                .padding(start = 32.dp, end = 12.dp, top = 12.dp, bottom = 20.dp)
                .pointerHoverIcon(PointerIcon.Crosshair)
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.first()
                            when {
                                event.type == PointerEventType.Exit && change.type == PointerType.Mouse -> inspectFraction = null
                                event.type == PointerEventType.Move || event.type == PointerEventType.Press ->
                                    inspectFraction = (change.position.x / size.width).coerceIn(0f, 1f)
                            }
                        }
                    }
                },
        ) {
            fun yFor(db: Float) = size.height * (1f - (db.coerceIn(-DB_RANGE, DB_RANGE) + DB_RANGE) / (2 * DB_RANGE))

            DB_GRID.forEach { db ->
                val y = yFor(db)
                drawLine(if (db == 0f) CurveZeroLine else CurveGridLine, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
                val label = textMeasurer.measure(if (db > 0) "+${db.toInt()}" else "${db.toInt()}", labelStyle)
                drawText(label, topLeft = Offset(-label.size.width - 6.dp.toPx(), y - label.size.height / 2f))
            }
            FREQ_GRID.forEach { hz ->
                val x = size.width * freqToFraction(hz)
                drawLine(CurveGridLine, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1.dp.toPx())
                val label = textMeasurer.measure(formatHz(hz), labelStyle)
                val lx = (x - label.size.width / 2f).coerceIn(0f, size.width - label.size.width)
                drawText(label, topLeft = Offset(lx, size.height + 2.dp.toPx()))
            }

            val curve = Path()
            frequencies.forEachIndexed { i, hz ->
                val x = size.width * freqToFraction(hz)
                val y = yFor(gains[i])
                if (i == 0) curve.moveTo(x, y) else curve.lineTo(x, y)
            }
            val fill = Path().apply {
                addPath(curve)
                lineTo(size.width, yFor(0f))
                lineTo(0f, yFor(0f))
                close()
            }
            val curveColor = if (state.isEnabled) colors.primary else colors.outline
            drawPath(fill, curveColor.copy(alpha = CurveFillAlpha))
            drawPath(curve, curveColor, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))

            inspectFraction?.let { f ->
                val x = size.width * f
                val y = yFor(ResponseCurve.totalGainDb(fractionToFreq(f), state))
                drawLine(colors.tertiary, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1.dp.toPx())
                drawCircle(colors.tertiary, radius = 5.dp.toPx(), center = Offset(x, y))
            }
        }

        inspectFraction?.let { f ->
            val hz = fractionToFreq(f)
            val db = ResponseCurve.totalGainDb(hz, state)
            Surface(
                modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
                shape = MaterialTheme.shapes.small,
                color = colors.tertiaryContainer,
            ) {
                Text(
                    text = String.format(Locale.US, "%s Hz : %+.1f dB", formatHz(hz), db),
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.onTertiaryContainer,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
        }
    }
}
