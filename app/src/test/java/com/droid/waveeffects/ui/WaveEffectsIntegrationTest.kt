package com.droid.waveeffects.ui

import com.droid.waveeffects.domain.AudioSessionInfo
import com.droid.waveeffects.domain.AudioSessionRepository
import com.droid.waveeffects.domain.EqualizerBand
import com.droid.waveeffects.domain.EqualizerPreset
import com.droid.waveeffects.domain.PresetRepository
import com.droid.waveeffects.domain.ReverbPreset
import com.droid.waveeffects.dsp.FakeDspEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class WaveEffectsIntegrationTest {

    private lateinit var dspEngine: FakeDspEngine
    private lateinit var presetRepo: PresetRepository
    private lateinit var sessionRepo: AudioSessionRepository
    private lateinit var tempFile: File

    @Before
    fun setUp() {
        tempFile = File.createTempFile("integration_presets", ".json")
        tempFile.deleteOnExit()
        dspEngine = FakeDspEngine()
        presetRepo = PresetRepository(tempFile)
        sessionRepo = AudioSessionRepository()
    }

    @Test
    fun endToEnd_audioPipelineFlow_appliesEqualizerToDspEngine() {
        val viewModel = AudioEffectsViewModel(dspEngine, presetRepo, sessionRepo)

        // 1. Initial State
        assertTrue(dspEngine.isMasterEnabled)
        assertEquals(0, dspEngine.currentSessionId)

        // 2. Simulate Media App (e.g. Spotify) opening an audio session
        val spotifySession = AudioSessionInfo(
            sessionId = 88,
            packageName = "com.spotify.music",
            appName = "Spotify"
        )
        sessionRepo.addSession(spotifySession)
        sessionRepo.selectSession(88)

        assertEquals(88, sessionRepo.selectedSession.value?.sessionId)

        // 3. User selects "Bass Boost" preset
        val bassBoost = presetRepo.getAllPresets().first { it.name == "Bass Boost" }
        viewModel.selectPreset(bassBoost)

        // Verify DSP Engine receives the EQ bands & Bass Boost strength
        assertEquals(bassBoost.bandGains[0], dspEngine.bands[0].gainDb, 0.001f)
        assertEquals(bassBoost.bassBoostStrength, dspEngine.bassBoostStrength)
        assertTrue(dspEngine.bassBoostEnabled)

        // 4. User fine-tunes 1 kHz band by +3.5 dB
        viewModel.setBandGain(5, 3.5f)
        assertEquals(3.5f, dspEngine.bands[5].gainDb, 0.001f)
        assertEquals("Custom", viewModel.uiState.value.currentPreset.name)

        // 5. User adjusts Dynamics / Compressor
        val compConfig = viewModel.uiState.value.compressorConfig.copy(
            enabled = true,
            thresholdDb = -18f,
            ratio = 6f
        )
        viewModel.setCompressor(compConfig)
        assertTrue(dspEngine.compressorConfig.enabled)
        assertEquals(-18f, dspEngine.compressorConfig.thresholdDb, 0.001f)
        assertEquals(6f, dspEngine.compressorConfig.ratio, 0.001f)

        // 6. User sets Reverb to Large Hall
        viewModel.setReverb(ReverbPreset.LARGE_HALL)
        assertEquals(ReverbPreset.LARGE_HALL, dspEngine.reverbPreset)

        // 7. Master Bypass
        viewModel.toggleMaster()
        assertFalse(dspEngine.isMasterEnabled)

        // 8. Session close (e.g. Spotify paused/closed)
        sessionRepo.removeSession(88)
        assertEquals(0, sessionRepo.selectedSession.value?.sessionId)
    }

    @Test
    fun customPresetLifecycle_persistsAcrossAppRestarts() {
        val viewModel1 = AudioEffectsViewModel(dspEngine, presetRepo, sessionRepo)
        viewModel1.setBandGain(0, 5.0f)
        viewModel1.setBandGain(1, 4.0f)
        viewModel1.setBassBoost(true, 500)
        viewModel1.saveCustomPreset("Audiophile Warm")

        val savedPreset = presetRepo.getAllPresets().find { it.name == "Audiophile Warm" }
        assertNotNull(savedPreset)
        assertEquals(5.0f, savedPreset!!.bandGains[0], 0.001f)

        // Simulate app restart with fresh repository reading same file
        val reloadedRepo = PresetRepository(tempFile)
        val viewModel2 = AudioEffectsViewModel(FakeDspEngine(), reloadedRepo, AudioSessionRepository())

        assertTrue(viewModel2.uiState.value.allPresets.any { it.name == "Audiophile Warm" })
    }
}
