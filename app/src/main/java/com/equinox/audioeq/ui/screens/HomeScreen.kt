package com.equinox.audioeq.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SurroundSound
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.Card
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import com.equinox.audioeq.audio.EngineStatus
import com.equinox.audioeq.model.AutoEqProfile
import com.equinox.audioeq.model.BandSetting
import com.equinox.audioeq.model.EqState
import com.equinox.audioeq.ui.components.BandSlider
import com.equinox.audioeq.ui.components.EqVisualizerView
import com.equinox.audioeq.ui.components.TweakCard
import java.util.Locale
import kotlinx.coroutines.launch

private val HandCursor = Modifier.pointerHoverIcon(PointerIcon.Hand)

/** How the studio is arranged for the current window size. */
enum class StudioLayout {
    /** Wide and tall: headphone list beside the full studio. */
    DualPane,

    /** Medium width, or wide but short: navigation rail switching between sections. */
    Rail,

    /** Narrow: top app bar with a drawer menu and an overflow menu. */
    Drawer,
}

private enum class StudioSection(val label: String, val icon: ImageVector) {
    Headphones("Headphones", Icons.Default.Headphones),
    Equalizer("Equalizer", Icons.Default.Equalizer),
    Effects("Effects", Icons.Default.Tune),
}

private fun EqState.withFlatGraphicEq() =
    copy(graphicBands = graphicBands.map { it.copy(gainDb = 0f) }, customPreampDb = 0f)

@Composable
fun HomeScreen(
    state: EqState,
    engineStatus: EngineStatus,
    profiles: List<AutoEqProfile>,
    query: String,
    onQueryChange: (String) -> Unit,
    layout: StudioLayout,
    isInPictureInPicture: Boolean,
    onStateChange: ((EqState) -> EqState) -> Unit,
    onSelectProfile: (AutoEqProfile?) -> Unit,
    onImportProfile: () -> Unit,
    onEnterPictureInPicture: () -> Unit,
    onQuit: () -> Unit,
) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        when {
            isInPictureInPicture -> PictureInPictureContent(state, onStateChange)
            layout == StudioLayout.DualPane -> Row(Modifier.fillMaxSize().safeDrawingPadding()) {
                AutoEqSidebar(
                    profiles = profiles,
                    selected = state.selectedProfile,
                    query = query,
                    onQueryChange = onQueryChange,
                    onSelectProfile = onSelectProfile,
                    onImportProfile = onImportProfile,
                    autoFocusSearch = true,
                    modifier = Modifier.width(360.dp).fillMaxHeight().padding(16.dp),
                )
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 16.dp)
                        .padding(end = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    StudioHeader(state, engineStatus, onStateChange, onEnterPictureInPicture, onQuit)
                    EqVisualizerView(state)
                    GraphicEqCard(state, compact = false, onStateChange)
                    TweakGrid(state, compact = false, onStateChange)
                }
            }
            else -> MenuStudio(
                layout, state, engineStatus, profiles, query, onQueryChange,
                onStateChange, onSelectProfile, onImportProfile, onEnterPictureInPicture, onQuit,
            )
        }
    }
}

/** Section-based studio used when the window is too small for the dual-pane layout. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MenuStudio(
    layout: StudioLayout,
    state: EqState,
    engineStatus: EngineStatus,
    profiles: List<AutoEqProfile>,
    query: String,
    onQueryChange: (String) -> Unit,
    onStateChange: ((EqState) -> EqState) -> Unit,
    onSelectProfile: (AutoEqProfile?) -> Unit,
    onImportProfile: () -> Unit,
    onEnterPictureInPicture: () -> Unit,
    onQuit: () -> Unit,
) {
    var section by rememberSaveable { mutableStateOf(StudioSection.Equalizer) }
    val compact = layout == StudioLayout.Drawer
    val overflowMenu: @Composable () -> Unit = {
        StudioOverflowMenu(state, onStateChange, onSelectProfile, onImportProfile, onEnterPictureInPicture, onQuit)
    }
    val content: @Composable (Modifier) -> Unit = { modifier ->
        when (section) {
            StudioSection.Headphones -> AutoEqSidebar(
                profiles = profiles,
                selected = state.selectedProfile,
                query = query,
                onQueryChange = onQueryChange,
                onSelectProfile = onSelectProfile,
                onImportProfile = onImportProfile,
                autoFocusSearch = !compact,
                modifier = modifier.fillMaxSize(),
            )
            StudioSection.Equalizer, StudioSection.Effects -> Column(
                modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                EqVisualizerView(state)
                if (section == StudioSection.Equalizer) {
                    GraphicEqCard(state, compact, onStateChange)
                } else {
                    TweakGrid(state, compact, onStateChange)
                }
            }
        }
    }

    if (layout == StudioLayout.Rail) {
        Row(Modifier.fillMaxSize().safeDrawingPadding()) {
            NavigationRail(header = { overflowMenu() }) {
                Spacer(Modifier.weight(1f))
                StudioSection.entries.forEach { item ->
                    NavigationRailItem(
                        selected = section == item,
                        onClick = { section = item },
                        icon = { Icon(item.icon, contentDescription = null) },
                        label = { Text(item.label) },
                        modifier = HandCursor,
                    )
                }
                Spacer(Modifier.weight(1f))
            }
            Column(
                Modifier.weight(1f).fillMaxHeight().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                StudioHeader(state, engineStatus, onStateChange, onEnterPictureInPicture, onQuit)
                content(Modifier.weight(1f))
            }
        }
        return
    }

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text("Equinox Audio", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(24.dp))
                StudioSection.entries.forEach { item ->
                    NavigationDrawerItem(
                        label = { Text(item.label) },
                        icon = { Icon(item.icon, contentDescription = null) },
                        selected = section == item,
                        onClick = {
                            section = item
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(horizontal = 12.dp).then(HandCursor),
                    )
                }
                HorizontalDivider(Modifier.padding(vertical = 12.dp, horizontal = 24.dp))
                NavigationDrawerItem(
                    label = { Text("Import .txt profile") },
                    icon = { Icon(Icons.Default.UploadFile, contentDescription = null) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onImportProfile()
                    },
                    modifier = Modifier.padding(horizontal = 12.dp).then(HandCursor),
                )
                NavigationDrawerItem(
                    label = { Text("Picture-in-picture") },
                    icon = { Icon(Icons.Default.PictureInPictureAlt, contentDescription = null) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onEnterPictureInPicture()
                    },
                    modifier = Modifier.padding(horizontal = 12.dp).then(HandCursor),
                )
                NavigationDrawerItem(
                    label = { Text("Quit and turn off EQ") },
                    icon = { Icon(Icons.Default.PowerSettingsNew, contentDescription = null) },
                    selected = false,
                    onClick = onQuit,
                    modifier = Modifier.padding(horizontal = 12.dp).then(HandCursor),
                )
            }
        },
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(section.label, maxLines = 1)
                            Text(
                                "${state.selectedProfile?.displayName ?: "Manual EQ"} · ${engineStatus.outputDevice}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }, modifier = HandCursor) {
                            Icon(Icons.Default.Menu, contentDescription = "Open menu")
                        }
                    },
                    actions = {
                        Switch(
                            checked = state.isEnabled,
                            onCheckedChange = { on -> onStateChange { it.copy(isEnabled = on) } },
                            modifier = HandCursor,
                        )
                        overflowMenu()
                    },
                )
            },
        ) { padding ->
            content(Modifier.padding(padding).padding(16.dp))
        }
    }
}

@Composable
private fun StudioOverflowMenu(
    state: EqState,
    onStateChange: ((EqState) -> EqState) -> Unit,
    onSelectProfile: (AutoEqProfile?) -> Unit,
    onImportProfile: () -> Unit,
    onEnterPictureInPicture: () -> Unit,
    onQuit: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }, modifier = HandCursor) {
            Icon(Icons.Default.MoreVert, contentDescription = "More options")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Import .txt profile") },
                leadingIcon = { Icon(Icons.Default.UploadFile, contentDescription = null) },
                onClick = { expanded = false; onImportProfile() },
                modifier = HandCursor,
            )
            DropdownMenuItem(
                text = { Text("Picture-in-picture") },
                leadingIcon = { Icon(Icons.Default.PictureInPictureAlt, contentDescription = null) },
                onClick = { expanded = false; onEnterPictureInPicture() },
                modifier = HandCursor,
            )
            DropdownMenuItem(
                text = { Text("Reset graphic EQ") },
                leadingIcon = { Icon(Icons.Default.RestartAlt, contentDescription = null) },
                onClick = { expanded = false; onStateChange { it.withFlatGraphicEq() } },
                modifier = HandCursor,
            )
            if (state.selectedProfile != null) {
                DropdownMenuItem(
                    text = { Text("Clear profile") },
                    leadingIcon = { Icon(Icons.Default.Clear, contentDescription = null) },
                    onClick = { expanded = false; onSelectProfile(null) },
                    modifier = HandCursor,
                )
            }
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("Quit and turn off EQ") },
                leadingIcon = { Icon(Icons.Default.PowerSettingsNew, contentDescription = null) },
                onClick = { expanded = false; onQuit() },
                modifier = HandCursor,
            )
        }
    }
}

@Composable
private fun AutoEqSidebar(
    profiles: List<AutoEqProfile>,
    selected: AutoEqProfile?,
    query: String,
    onQueryChange: (String) -> Unit,
    onSelectProfile: (AutoEqProfile?) -> Unit,
    onImportProfile: () -> Unit,
    autoFocusSearch: Boolean,
    modifier: Modifier = Modifier,
) {
    val searchFocusRequester = remember { FocusRequester() }
    if (autoFocusSearch) {
        // Desktop users can start typing a headphone name immediately.
        LaunchedEffect(Unit) { searchFocusRequester.requestFocus() }
    }

    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("AutoEq headphones", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth().focusRequester(searchFocusRequester),
                placeholder = { Text("Search brand or model") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }, modifier = HandCursor) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = onImportProfile, modifier = HandCursor) {
                    Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("Import .txt", Modifier.padding(start = 8.dp))
                }
                if (selected != null) {
                    TextButton(onClick = { onSelectProfile(null) }, modifier = HandCursor) { Text("Clear profile") }
                }
            }
            HorizontalDivider()

            val rowFor: @Composable (AutoEqProfile) -> Unit = { profile ->
                ProfileRow(profile, isSelected = profile.id == selected?.id, onClick = { onSelectProfile(profile) })
            }
            if (profiles.isEmpty()) {
                Text("No matching headphones", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(8.dp))
            } else {
                LazyColumn(contentPadding = PaddingValues(bottom = 8.dp)) {
                    items(profiles, key = { it.id }) { rowFor(it) }
                }
            }
        }
    }
}

@Composable
private fun ProfileRow(profile: AutoEqProfile, isSelected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    ListItem(
        headlineContent = { Text(profile.displayName) },
        supportingContent = { Text("${profile.source} · ${profile.target}", style = MaterialTheme.typography.bodySmall) },
        leadingContent = { Icon(Icons.Default.Headphones, contentDescription = null) },
        trailingContent = { if (isSelected) Icon(Icons.Default.Check, contentDescription = "Selected") },
        colors = ListItemDefaults.colors(
            containerColor = if (isSelected) colors.primaryContainer else colors.surfaceContainerLow,
        ),
        modifier = Modifier
            .padding(vertical = 2.dp)
            .then(Modifier.clickable(onClick = onClick))
            .then(HandCursor),
    )
}

@Composable
private fun StudioHeader(
    state: EqState,
    engineStatus: EngineStatus,
    onStateChange: ((EqState) -> EqState) -> Unit,
    onEnterPictureInPicture: () -> Unit,
    onQuit: () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f)) {
            Text("Equinox Audio", style = MaterialTheme.typography.headlineMedium)
            Text(
                buildString {
                    append(state.selectedProfile?.displayName ?: "Manual EQ")
                    append(" · ")
                    append(engineStatus.outputDevice)
                    append(" · ")
                    append(
                        when {
                            engineStatus.globalSessionAttached -> "global mix"
                            engineStatus.attachedSessions.isNotEmpty() -> "${engineStatus.attachedSessions.size} sessions"
                            else -> "waiting for audio sessions"
                        },
                    )
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        FilledTonalIconButton(onClick = onEnterPictureInPicture, modifier = HandCursor) {
            Icon(Icons.Default.PictureInPictureAlt, contentDescription = "Pin as picture-in-picture")
        }
        FilledTonalIconButton(onClick = onQuit, modifier = HandCursor) {
            Icon(Icons.Default.PowerSettingsNew, contentDescription = "Quit and turn off EQ")
        }
        Switch(
            checked = state.isEnabled,
            onCheckedChange = { on -> onStateChange { it.copy(isEnabled = on) } },
            modifier = HandCursor,
        )
    }
}

@Composable
private fun GraphicEqCard(state: EqState, compact: Boolean, onStateChange: ((EqState) -> EqState) -> Unit) {
    Card(shape = MaterialTheme.shapes.extraLarge, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("10-band graphic EQ", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                TextButton(
                    onClick = { onStateChange { it.withFlatGraphicEq() } },
                    modifier = HandCursor,
                ) {
                    Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("Reset", Modifier.padding(start = 6.dp))
                }
            }

            LabeledSlider(
                label = "Preamp",
                valueText = String.format(Locale.US, "%+.1f dB", state.customPreampDb + (state.selectedProfile?.preampDb ?: 0f)),
                value = state.customPreampDb,
                range = -12f..6f,
                onChange = { v -> onStateChange { it.copy(customPreampDb = v) } },
            )

            val setBand: (Int, Float) -> Unit = { index, gain ->
                onStateChange { s ->
                    s.copy(graphicBands = s.graphicBands.mapIndexed { i, b -> if (i == index) BandSetting(b.frequencyHz, gain) else b })
                }
            }
            if (compact) {
                state.graphicBands.forEachIndexed { i, band ->
                    BandSlider(band.frequencyHz, band.gainDb, { setBand(i, it) }, compact = true, enabled = state.isEnabled)
                }
            } else {
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    state.graphicBands.forEachIndexed { i, band ->
                        BandSlider(band.frequencyHz, band.gainDb, { setBand(i, it) }, enabled = state.isEnabled)
                    }
                }
            }

            LabeledSlider(
                label = "Balance",
                valueText = when {
                    state.channelBalance < -0.01f -> "L ${(-state.channelBalance * 100).toInt()}%"
                    state.channelBalance > 0.01f -> "R ${(state.channelBalance * 100).toInt()}%"
                    else -> "Center"
                },
                value = state.channelBalance,
                range = -1f..1f,
                onChange = { v -> onStateChange { it.copy(channelBalance = v) } },
            )
        }
    }
}

@Composable
private fun LabeledSlider(label: String, valueText: String, value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.width(72.dp))
        Slider(value = value, onValueChange = onChange, valueRange = range, modifier = Modifier.weight(1f).then(HandCursor))
        Text(valueText, style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(72.dp).padding(start = 8.dp))
    }
}

@Composable
private fun TweakGrid(state: EqState, compact: Boolean, onStateChange: ((EqState) -> EqState) -> Unit) {
    val cards: List<@Composable (Modifier) -> Unit> = listOf(
        { m ->
            TweakCard(
                "Bass Boost", "Low shelf below ${state.bassCutoffFreqHz.toInt()} Hz", Icons.Default.Waves,
                state.bassBoostEnabled, { on -> onStateChange { it.copy(bassBoostEnabled = on) } }, m,
                value = state.bassBoostStrength, onValueChange = { v -> onStateChange { it.copy(bassBoostStrength = v) } },
            )
        },
        { m ->
            TweakCard(
                "Treble Boost", "High shelf from ${state.trebleShelfFreqHz.toInt() / 1000} kHz", Icons.Default.AutoAwesome,
                state.trebleBoostEnabled, { on -> onStateChange { it.copy(trebleBoostEnabled = on) } }, m,
                value = state.trebleBoostStrength, onValueChange = { v -> onStateChange { it.copy(trebleBoostStrength = v) } },
            )
        },
        { m ->
            TweakCard(
                "Vocal Clarity", "Presence lift around 2.5 kHz", Icons.Default.RecordVoiceOver,
                state.vocalClarityEnabled, { on -> onStateChange { it.copy(vocalClarityEnabled = on) } }, m,
                value = state.vocalClarityStrength, onValueChange = { v -> onStateChange { it.copy(vocalClarityStrength = v) } },
            )
        },
        { m ->
            TweakCard(
                "Spatial Virtualizer", "Headphone virtual surround", Icons.Default.SurroundSound,
                state.virtualizerEnabled, { on -> onStateChange { it.copy(virtualizerEnabled = on) } }, m,
                value = state.virtualizerStrength, valueRange = 0f..1000f,
                onValueChange = { v -> onStateChange { it.copy(virtualizerStrength = v) } },
                valueLabel = { "${(it / 10).toInt()}%" },
            )
        },
        { m ->
            TweakCard(
                "Loudness Equalizer", "Raises perceived loudness", Icons.AutoMirrored.Filled.VolumeUp,
                state.loudnessEqualizerEnabled, { on -> onStateChange { it.copy(loudnessEqualizerEnabled = on) } }, m,
                value = state.loudnessGainDb, valueRange = 0f..10f,
                onValueChange = { v -> onStateChange { it.copy(loudnessGainDb = v) } },
                valueLabel = { String.format(Locale.US, "+%.1f dB", it) },
            )
        },
        { m ->
            TweakCard(
                "Zero-Clip Limiter", "Brickwall at -0.5 dBFS", Icons.Default.Shield,
                state.limiterEnabled, { on -> onStateChange { it.copy(limiterEnabled = on) } }, m,
            )
        },
    )

    Text("On-the-fly DSP", style = MaterialTheme.typography.titleMedium)
    val columns = if (compact) 1 else 2
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        cards.chunked(columns).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { card -> card(Modifier.weight(1f)) }
            }
        }
    }
}

/** Minimal controls for the pinned desktop PiP window. */
@Composable
private fun PictureInPictureContent(state: EqState, onStateChange: ((EqState) -> EqState) -> Unit) {
    Column(Modifier.fillMaxSize().padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                state.selectedProfile?.displayName ?: "Manual EQ",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
                maxLines = 1,
            )
            Switch(checked = state.isEnabled, onCheckedChange = { on -> onStateChange { it.copy(isEnabled = on) } }, modifier = HandCursor)
        }
        EqVisualizerView(state)
    }
}
