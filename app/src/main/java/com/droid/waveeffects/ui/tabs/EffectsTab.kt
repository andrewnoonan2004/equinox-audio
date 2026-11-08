package com.droid.waveeffects.ui.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.SurroundSound
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.droid.waveeffects.domain.ReverbPreset
import com.droid.waveeffects.ui.AudioEffectsUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EffectsTab(
    state: AudioEffectsUiState,
    onBassBoostChanged: (Boolean, Int) -> Unit,
    onVirtualizerChanged: (Boolean, Int) -> Unit,
    onLoudnessChanged: (Boolean, Int) -> Unit,
    onReverbChanged: (ReverbPreset) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Bass Boost
        val primaryAccent = MaterialTheme.colorScheme.primary
        val secondaryAccent = MaterialTheme.colorScheme.secondary
        val tertiaryAccent = MaterialTheme.colorScheme.tertiary

        EffectModuleCard(
            title = "Bass Boost",
            subtitle = "Deep sub-harmonic low-end resonance",
            icon = Icons.Default.Waves,
            accentColor = primaryAccent,
            enabled = state.bassBoostEnabled,
            onEnabledChange = { onBassBoostChanged(it, state.bassBoostStrength) }
        ) {
            EffectSlider(
                label = "Strength",
                valueText = "${(state.bassBoostStrength / 10f).toInt()}%",
                value = state.bassBoostStrength.toFloat(),
                range = 0f..1000f,
                enabled = state.bassBoostEnabled && state.masterEnabled,
                accentColor = primaryAccent,
                onValueChange = { onBassBoostChanged(state.bassBoostEnabled, it.toInt()) }
            )
        }

        // 2. Virtualizer / Spatial 3D Widener
        EffectModuleCard(
            title = "Virtualizer (Spatializer)",
            subtitle = "Binaural headphone soundstage expansion",
            icon = Icons.Default.SurroundSound,
            accentColor = secondaryAccent,
            enabled = state.virtualizerEnabled,
            onEnabledChange = { onVirtualizerChanged(it, state.virtualizerStrength) }
        ) {
            EffectSlider(
                label = "Width",
                valueText = "${(state.virtualizerStrength / 10f).toInt()}%",
                value = state.virtualizerStrength.toFloat(),
                range = 0f..1000f,
                enabled = state.virtualizerEnabled && state.masterEnabled,
                accentColor = secondaryAccent,
                onValueChange = { onVirtualizerChanged(state.virtualizerEnabled, it.toInt()) }
            )
        }

        // 3. Loudness Enhancer
        EffectModuleCard(
            title = "Loudness Enhancer",
            subtitle = "Clean gain boost for quiet sources",
            icon = Icons.AutoMirrored.Filled.VolumeUp,
            accentColor = tertiaryAccent,
            enabled = state.loudnessEnabled,
            onEnabledChange = { onLoudnessChanged(it, state.loudnessGainMb) }
        ) {
            EffectSlider(
                label = "Target Gain",
                valueText = "+%.1f dB".format(state.loudnessGainMb / 100f),
                value = state.loudnessGainMb.toFloat(),
                range = 0f..1200f,
                enabled = state.loudnessEnabled && state.masterEnabled,
                accentColor = tertiaryAccent,
                onValueChange = { onLoudnessChanged(state.loudnessEnabled, it.toInt()) }
            )
        }

        // 4. Environmental Reverb
        EffectModuleCard(
            title = "Environmental Reverb",
            subtitle = "Room acoustic simulation and ambience",
            icon = Icons.Default.GraphicEq,
            accentColor = primaryAccent,
            enabled = state.reverbPreset != ReverbPreset.NONE,
            onEnabledChange = { enabled ->
                onReverbChanged(if (enabled) ReverbPreset.MEDIUM_ROOM else ReverbPreset.NONE)
            }
        ) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ReverbPreset.entries.forEach { preset ->
                    val isSelected = state.reverbPreset == preset
                    ElevatedFilterChip(
                        selected = isSelected,
                        onClick = { onReverbChanged(preset) },
                        label = { Text(preset.displayName, fontSize = 12.sp) },
                        colors = FilterChipDefaults.elevatedFilterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun EffectModuleCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    content: @Composable () -> Unit
) {
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
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor
                    )
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Switch(
                    checked = enabled,
                    onCheckedChange = onEnabledChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = accentColor,
                        checkedTrackColor = accentColor.copy(alpha = 0.5f),
                        uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            }

            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun EffectSlider(
    label: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    enabled: Boolean,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    onValueChange: (Float) -> Unit
) {
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
            fontSize = 13.sp,
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
        modifier = Modifier.height(30.dp)
    )
}
