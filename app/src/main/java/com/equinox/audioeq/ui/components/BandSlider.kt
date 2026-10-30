package com.equinox.audioeq.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import java.util.Locale

const val BAND_MIN_DB = -12f
const val BAND_MAX_DB = 12f

fun formatBandFrequency(hz: Int): String = if (hz >= 1000) "${hz / 1000}k" else "$hz"

private fun formatDb(db: Float) = String.format(Locale.US, "%+.1f", db)

/** Vertical fader (-12..+12 dB) for the studio layout, or a horizontal row when [compact]. */
@Composable
fun BandSlider(
    frequencyHz: Int,
    gainDb: Float,
    onGainChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    enabled: Boolean = true,
) {
    if (compact) {
        Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(formatBandFrequency(frequencyHz), style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(40.dp))
            Slider(
                value = gainDb,
                onValueChange = onGainChange,
                valueRange = BAND_MIN_DB..BAND_MAX_DB,
                enabled = enabled,
                modifier = Modifier.weight(1f).pointerHoverIcon(PointerIcon.Hand),
            )
            Text(formatDb(gainDb), style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(48.dp))
        }
        return
    }

    Column(modifier.width(52.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(formatDb(gainDb), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        Box(Modifier.height(180.dp), contentAlignment = Alignment.Center) {
            Slider(
                value = gainDb,
                onValueChange = onGainChange,
                valueRange = BAND_MIN_DB..BAND_MAX_DB,
                enabled = enabled,
                modifier = Modifier
                    .graphicsLayer {
                        rotationZ = 270f
                        transformOrigin = TransformOrigin(0f, 0f)
                    }
                    .layout { measurable, constraints ->
                        // Measure with swapped axes, then place so the rotated slider fills the column.
                        val placeable = measurable.measure(
                            Constraints(
                                minWidth = constraints.minHeight,
                                maxWidth = constraints.maxHeight,
                                minHeight = constraints.minWidth,
                                maxHeight = constraints.maxWidth,
                            ),
                        )
                        layout(placeable.height, placeable.width) { placeable.place(-placeable.width, 0) }
                    }
                    .width(180.dp)
                    .pointerHoverIcon(PointerIcon.Hand),
            )
        }
        Text(formatBandFrequency(frequencyHz), style = MaterialTheme.typography.labelMedium)
    }
}
