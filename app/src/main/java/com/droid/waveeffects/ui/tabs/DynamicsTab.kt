package com.droid.waveeffects.ui.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.droid.waveeffects.domain.CompressorConfig
import com.droid.waveeffects.domain.LimiterConfig
import com.droid.waveeffects.ui.AudioEffectsUiState

@Composable
fun DynamicsTab(
    state: AudioEffectsUiState,
    onCompressorChanged: (CompressorConfig) -> Unit,
    onLimiterChanged: (LimiterConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Dynamic Compressor Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Compress,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text(
                                text = "Compressor",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Dynamic range compression & punch",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Switch(
                        checked = state.compressorConfig.enabled,
                        onCheckedChange = { enabled ->
                            onCompressorChanged(state.compressorConfig.copy(enabled = enabled))
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }

                Spacer(Modifier.height(16.dp))

                val comp = state.compressorConfig
                val compEnabled = comp.enabled && state.masterEnabled

                DynamicsSliderRow(
                    label = "Threshold",
                    valueText = "%.1f dB".format(comp.thresholdDb),
                    value = comp.thresholdDb,
                    range = -60f..0f,
                    enabled = compEnabled,
                    onValueChange = { onCompressorChanged(comp.copy(thresholdDb = it)) }
                )

                DynamicsSliderRow(
                    label = "Ratio",
                    valueText = "%.1f : 1".format(comp.ratio),
                    value = comp.ratio,
                    range = 1f..20f,
                    enabled = compEnabled,
                    onValueChange = { onCompressorChanged(comp.copy(ratio = it)) }
                )

                DynamicsSliderRow(
                    label = "Attack Time",
                    valueText = "%.0f ms".format(comp.attackMs),
                    value = comp.attackMs,
                    range = 1f..200f,
                    enabled = compEnabled,
                    onValueChange = { onCompressorChanged(comp.copy(attackMs = it)) }
                )

                DynamicsSliderRow(
                    label = "Release Time",
                    valueText = "%.0f ms".format(comp.releaseMs),
                    value = comp.releaseMs,
                    range = 10f..1000f,
                    enabled = compEnabled,
                    onValueChange = { onCompressorChanged(comp.copy(releaseMs = it)) }
                )

                DynamicsSliderRow(
                    label = "Makeup Gain",
                    valueText = "+%.1f dB".format(comp.makeupGainDb),
                    value = comp.makeupGainDb,
                    range = 0f..24f,
                    enabled = compEnabled,
                    onValueChange = { onCompressorChanged(comp.copy(makeupGainDb = it)) }
                )
            }
        }

        // 2. Peak Limiter Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Speed,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Column {
                            Text(
                                text = "Peak Limiter",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Brickwall clipping and distortion shield",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Switch(
                        checked = state.limiterConfig.enabled,
                        onCheckedChange = { enabled ->
                            onLimiterChanged(state.limiterConfig.copy(enabled = enabled))
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.secondary,
                            checkedTrackColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),
                            uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }

                Spacer(Modifier.height(16.dp))

                val lim = state.limiterConfig
                val limEnabled = lim.enabled && state.masterEnabled

                DynamicsSliderRow(
                    label = "Threshold",
                    valueText = "%.1f dB".format(lim.thresholdDb),
                    value = lim.thresholdDb,
                    range = -20f..0f,
                    enabled = limEnabled,
                    onValueChange = { onLimiterChanged(lim.copy(thresholdDb = it)) }
                )

                DynamicsSliderRow(
                    label = "Attack Time",
                    valueText = "%.1f ms".format(lim.attackMs),
                    value = lim.attackMs,
                    range = 0.5f..10f,
                    enabled = limEnabled,
                    onValueChange = { onLimiterChanged(lim.copy(attackMs = it)) }
                )

                DynamicsSliderRow(
                    label = "Release Time",
                    valueText = "%.0f ms".format(lim.releaseMs),
                    value = lim.releaseMs,
                    range = 5f..200f,
                    enabled = limEnabled,
                    onValueChange = { onLimiterChanged(lim.copy(releaseMs = it)) }
                )

                DynamicsSliderRow(
                    label = "Post-Gain",
                    valueText = if (lim.postGainDb >= 0) "+%.1f dB".format(lim.postGainDb) else "%.1f dB".format(lim.postGainDb),
                    value = lim.postGainDb,
                    range = -6f..6f,
                    enabled = limEnabled,
                    onValueChange = { onLimiterChanged(lim.copy(postGainDb = it)) }
                )
            }
        }
    }
}

@Composable
private fun DynamicsSliderRow(
    label: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    enabled: Boolean,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = valueText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = if (enabled) accentColor else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = if (enabled) accentColor else MaterialTheme.colorScheme.outline,
                activeTrackColor = if (enabled) accentColor else MaterialTheme.colorScheme.outlineVariant,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier.height(28.dp)
        )
    }
}
