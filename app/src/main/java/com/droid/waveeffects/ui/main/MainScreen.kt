package com.droid.waveeffects.ui.main

import androidx.compose.animation.Crossfade
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SurroundSound
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.droid.waveeffects.audio.AudioDemoPlayer
import com.droid.waveeffects.ui.AudioEffectsViewModel
import com.droid.waveeffects.ui.tabs.AutoEqTab
import com.droid.waveeffects.ui.tabs.DynamicsTab
import com.droid.waveeffects.ui.tabs.EffectsTab
import com.droid.waveeffects.ui.tabs.EqualizerTab
import com.droid.waveeffects.ui.tabs.SessionsTab
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: AudioEffectsViewModel,
    demoPlayer: AudioDemoPlayer,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }

    // Real-time Visualizer polling loop (30 FPS)
    LaunchedEffect(state.masterEnabled) {
        while (state.masterEnabled) {
            viewModel.pollVisualizer()
            delay(33)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "WaveEffects",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "AUTOEQ",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            val activeAppName = state.selectedSession?.appName ?: "Global Output"
                            Text(
                                text = "Routing: $activeAppName",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                },
                actions = {
                    // Wallpaper Color Adaptation Toggle Button
                    IconButton(
                        onClick = { viewModel.toggleDynamicColor() },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                if (state.useDynamicColor) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = "Toggle Wallpaper Color Adaptation",
                            tint = if (state.useDynamicColor) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Material 3 Expressive Master Power Pill
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(
                                width = 1.dp,
                                color = if (state.masterEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant,
                                shape = CircleShape
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (state.masterEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
                        )
                        Text(
                            text = if (state.masterEnabled) "ACTIVE" else "BYPASS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (state.masterEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                        Switch(
                            checked = state.masterEnabled,
                            onCheckedChange = { viewModel.toggleMaster() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                uncheckedTrackColor = MaterialTheme.colorScheme.surface
                            ),
                            modifier = Modifier.padding(start = 2.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                tonalElevation = 8.dp
            ) {
                val navItems = listOf(
                    Triple(0, "Equalizer", Icons.Default.Tune),
                    Triple(1, "AutoEQ", Icons.Default.Headphones),
                    Triple(2, "Dynamics", Icons.Default.Speed),
                    Triple(3, "Effects", Icons.Default.SurroundSound),
                    Triple(4, "Sessions", Icons.Default.Audiotrack)
                )

                navItems.forEach { (index, title, icon) ->
                    val isSelected = selectedTab == index
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { selectedTab = index },
                        icon = { Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp)) },
                        label = { Text(title, fontSize = 10.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            Crossfade(targetState = selectedTab, label = "TabCrossfade") { tab ->
                when (tab) {
                    0 -> EqualizerTab(
                        state = state,
                        onBandGainChanged = { bandIndex, gain -> viewModel.setBandGain(bandIndex, gain) },
                        onPresetSelected = { preset -> viewModel.selectPreset(preset) },
                        onResetToFlat = { viewModel.resetToFlat() },
                        onSaveCustomPreset = { name -> viewModel.saveCustomPreset(name) },
                        onDeleteCustomPreset = { id -> viewModel.deleteCustomPreset(id) }
                    )
                    1 -> AutoEqTab(
                        state = state,
                        onApplyProfile = { profile -> viewModel.applyAutoEqProfile(profile) },
                        onImportAutoEq = { name, content -> viewModel.importAutoEqPreset(name, content) }
                    )
                    2 -> DynamicsTab(
                        state = state,
                        onCompressorChanged = { comp -> viewModel.setCompressor(comp) },
                        onLimiterChanged = { lim -> viewModel.setLimiter(lim) }
                    )
                    3 -> EffectsTab(
                        state = state,
                        onBassBoostChanged = { enabled, strength -> viewModel.setBassBoost(enabled, strength) },
                        onVirtualizerChanged = { enabled, strength -> viewModel.setVirtualizer(enabled, strength) },
                        onLoudnessChanged = { enabled, gainMb -> viewModel.setLoudness(enabled, gainMb) },
                        onReverbChanged = { reverb -> viewModel.setReverb(reverb) }
                    )
                    4 -> SessionsTab(
                        state = state,
                        onSessionSelected = { sessionId -> viewModel.selectSession(sessionId) },
                        demoPlayer = demoPlayer
                    )
                }
            }
        }
    }
}
