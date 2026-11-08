package com.droid.waveeffects.ui

import com.droid.waveeffects.domain.AudioSessionInfo
import com.droid.waveeffects.domain.AudioSessionRepository
import com.droid.waveeffects.domain.EqualizerPreset
import com.droid.waveeffects.domain.PresetRepository
import com.droid.waveeffects.domain.ReverbPreset
import com.droid.waveeffects.dsp.FakeDspEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class AudioEffectsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeDspEngine: FakeDspEngine
    private lateinit var presetRepo: PresetRepository
    private lateinit var sessionRepo: AudioSessionRepository
    private lateinit var viewModel: AudioEffectsViewModel
    private lateinit var tempFile: File

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        tempFile = File.createTempFile("vm_test_presets", ".json")
        tempFile.deleteOnExit()

        fakeDspEngine = FakeDspEngine()
        presetRepo = PresetRepository(tempFile)
        sessionRepo = AudioSessionRepository()

        viewModel = AudioEffectsViewModel(
            dspEngine = fakeDspEngine,
            presetRepository = presetRepo,
            audioSessionRepository = sessionRepo
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialUiState_hasCorrectDefaults() = runTest {
        val state = viewModel.uiState.value
        assertTrue(state.masterEnabled)
        assertEquals("Flat", state.currentPreset.name)
        assertEquals(10, state.bands.size)
        assertTrue(state.bands.all { it.gainDb == 0f })
        assertFalse(state.bassBoostEnabled)
        assertFalse(state.virtualizerEnabled)
        assertFalse(state.loudnessEnabled)
        assertTrue(state.limiterConfig.enabled)
        assertEquals(1, state.activeSessions.size)
        assertEquals(0, state.selectedSession?.sessionId)
    }

    @Test
    fun toggleMaster_updatesStateAndDspEngine() = runTest {
        assertTrue(viewModel.uiState.value.masterEnabled)
        viewModel.toggleMaster()
        assertFalse(viewModel.uiState.value.masterEnabled)
        assertFalse(fakeDspEngine.isMasterEnabled)

        viewModel.toggleMaster()
        assertTrue(viewModel.uiState.value.masterEnabled)
        assertTrue(fakeDspEngine.isMasterEnabled)
    }

    @Test
    fun setBandGain_updatesSpecificBandAndCustomizesPreset() = runTest {
        viewModel.setBandGain(0, 6.0f)

        val state = viewModel.uiState.value
        assertEquals(6.0f, state.bands[0].gainDb, 0.001f)
        assertEquals("Custom", state.currentPreset.name)
        assertEquals(6.0f, fakeDspEngine.bands[0].gainDb, 0.001f)
    }

    @Test
    fun selectPreset_appliesPresetGainsToDspEngine() = runTest {
        val bassBoostPreset = presetRepo.getAllPresets().first { it.name == "Bass Boost" }
        viewModel.selectPreset(bassBoostPreset)

        val state = viewModel.uiState.value
        assertEquals("Bass Boost", state.currentPreset.name)
        assertEquals(bassBoostPreset.bandGains[0], state.bands[0].gainDb, 0.001f)
        assertEquals(bassBoostPreset.bassBoostStrength, state.bassBoostStrength)
        assertTrue(state.bassBoostEnabled)
        assertEquals(bassBoostPreset.bassBoostStrength, fakeDspEngine.bassBoostStrength)
    }

    @Test
    fun saveCustomPreset_addsToPresetList() = runTest {
        viewModel.setBandGain(2, 4.0f)
        viewModel.saveCustomPreset("My Preset")

        val state = viewModel.uiState.value
        assertEquals("My Preset", state.currentPreset.name)
        assertTrue(state.allPresets.any { it.name == "My Preset" })
    }

    @Test
    fun setEffects_updatesStateAndEngine() = runTest {
        viewModel.setBassBoost(enabled = true, strength = 750)
        assertTrue(viewModel.uiState.value.bassBoostEnabled)
        assertEquals(750, viewModel.uiState.value.bassBoostStrength)
        assertTrue(fakeDspEngine.bassBoostEnabled)
        assertEquals(750, fakeDspEngine.bassBoostStrength)

        viewModel.setVirtualizer(enabled = true, strength = 600)
        assertTrue(viewModel.uiState.value.virtualizerEnabled)
        assertEquals(600, viewModel.uiState.value.virtualizerStrength)

        viewModel.setLoudness(enabled = true, gainMb = 400)
        assertTrue(viewModel.uiState.value.loudnessEnabled)
        assertEquals(400, viewModel.uiState.value.loudnessGainMb)

        viewModel.setReverb(ReverbPreset.LARGE_HALL)
        assertEquals(ReverbPreset.LARGE_HALL, viewModel.uiState.value.reverbPreset)
        assertEquals(ReverbPreset.LARGE_HALL, fakeDspEngine.reverbPreset)
    }

    @Test
    fun sessionSelection_updatesDspEngineAttachment() = runTest {
        val spotify = AudioSessionInfo(sessionId = 101, packageName = "com.spotify.music", appName = "Spotify")
        sessionRepo.addSession(spotify)
        testScheduler.advanceUntilIdle()

        viewModel.selectSession(101)
        testScheduler.advanceUntilIdle()

        assertEquals(101, viewModel.uiState.value.selectedSession?.sessionId)
        assertEquals(101, fakeDspEngine.currentSessionId)
    }
}
