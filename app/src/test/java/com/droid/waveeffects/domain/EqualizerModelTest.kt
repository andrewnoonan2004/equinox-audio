package com.droid.waveeffects.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EqualizerModelTest {

    @Test
    fun default10Bands_haveCorrectFrequencies() {
        val defaultBands = EqualizerBand.createDefault10Bands()
        assertEquals(10, defaultBands.size)
        val expectedFreqs = listOf(31, 62, 125, 250, 500, 1000, 2000, 4000, 8000, 16000)
        assertEquals(expectedFreqs, defaultBands.map { it.frequencyHz })
        assertTrue(defaultBands.all { it.gainDb == 0f })
    }

    @Test
    fun bandGain_isClampedToMinMax() {
        val band = EqualizerBand(id = 0, frequencyHz = 1000, gainDb = 0f, minGainDb = -12f, maxGainDb = 12f)
        val clampedHigh = band.withGain(15f)
        assertEquals(12f, clampedHigh.gainDb, 0.001f)

        val clampedLow = band.withGain(-20f)
        assertEquals(-12f, clampedLow.gainDb, 0.001f)

        val normal = band.withGain(4.5f)
        assertEquals(4.5f, normal.gainDb, 0.001f)
    }

    @Test
    fun builtInPresets_containStandardAudiophileProfiles() {
        val presets = EqualizerPreset.builtInPresets
        assertTrue(presets.isNotEmpty())
        val presetNames = presets.map { it.name }
        assertTrue(presetNames.contains("Flat"))
        assertTrue(presetNames.contains("Bass Boost"))
        assertTrue(presetNames.contains("Vocal Clarity"))
        assertTrue(presetNames.contains("Harman Target"))
        assertTrue(presetNames.contains("Club / Electronic"))

        val bassBoost = presets.first { it.name == "Bass Boost" }
        assertEquals(10, bassBoost.bandGains.size)
        // Bass frequencies should have positive gain
        assertTrue(bassBoost.bandGains[0] > 0f)
        assertTrue(bassBoost.bandGains[1] > 0f)
        assertTrue(bassBoost.bassBoostStrength > 0)
    }

    @Test
    fun customPreset_serializationAndDeserialization() {
        val custom = EqualizerPreset(
            id = "custom_test_1",
            name = "My Custom EQ",
            isBuiltIn = false,
            bandGains = listOf(3f, 2f, 1f, 0f, -1f, -1f, 0f, 2f, 4f, 5f),
            bassBoostStrength = 400,
            virtualizerStrength = 300,
            loudnessDb = 3.5f
        )

        val json = custom.toJson()
        assertNotNull(json)
        val decoded = EqualizerPreset.fromJson(json)
        assertEquals(custom.id, decoded.id)
        assertEquals(custom.name, decoded.name)
        assertFalse(decoded.isBuiltIn)
        assertEquals(custom.bandGains, decoded.bandGains)
        assertEquals(custom.bassBoostStrength, decoded.bassBoostStrength)
        assertEquals(custom.virtualizerStrength, decoded.virtualizerStrength)
        assertEquals(custom.loudnessDb, decoded.loudnessDb, 0.001f)
    }

    @Test
    fun compressorConfig_defaultValuesAreSane() {
        val compressor = CompressorConfig.defaultConfig()
        assertFalse(compressor.enabled)
        assertEquals(-24f, compressor.thresholdDb, 0.001f)
        assertEquals(4.0f, compressor.ratio, 0.001f)
        assertTrue(compressor.attackMs in 1f..100f)
        assertTrue(compressor.releaseMs in 10f..1000f)
    }

    @Test
    fun limiterConfig_defaultValuesPreventClipping() {
        val limiter = LimiterConfig.defaultConfig()
        assertTrue(limiter.enabled)
        assertEquals(-0.5f, limiter.thresholdDb, 0.001f)
        assertTrue(limiter.attackMs < 5f) // fast attack for limiting
    }
}
