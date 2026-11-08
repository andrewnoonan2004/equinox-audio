package com.droid.waveeffects.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class PresetRepositoryTest {

    private lateinit var tempFile: File
    private lateinit var repository: PresetRepository

    @Before
    fun setUp() {
        tempFile = File.createTempFile("test_presets", ".json")
        tempFile.deleteOnExit()
        repository = PresetRepository(presetsFile = tempFile)
    }

    @Test
    fun initialPresets_containAllBuiltInPresets() {
        val presets = repository.getAllPresets()
        assertTrue(presets.any { it.name == "Flat" })
        assertTrue(presets.any { it.name == "Bass Boost" })
        assertTrue(presets.any { it.name == "Harman Target" })
        assertTrue(presets.any { it.name == "Club / Electronic" })
        assertTrue(presets.all { it.isBuiltIn })
    }

    @Test
    fun saveCustomPreset_persistsAndCanBeRetrieved() {
        val custom = EqualizerPreset(
            id = "custom_1",
            name = "Late Night Listening",
            isBuiltIn = false,
            bandGains = listOf(1f, 1f, 0f, 0f, 0f, 0f, -1f, -2f, -3f, -4f),
            bassBoostStrength = 300,
            virtualizerStrength = 400,
            loudnessDb = 1.0f
        )

        repository.saveCustomPreset(custom)

        val updated = repository.getAllPresets()
        assertTrue(updated.any { it.id == "custom_1" })

        // Create a new instance pointing to the same file to ensure persistence
        val reloadedRepo = PresetRepository(presetsFile = tempFile)
        val reloadedPresets = reloadedRepo.getAllPresets()
        val found = reloadedPresets.find { it.id == "custom_1" }
        assertNotNull(found)
        assertEquals("Late Night Listening", found?.name)
        assertFalse(found!!.isBuiltIn)
        assertEquals(custom.bandGains, found.bandGains)
    }

    @Test
    fun deleteCustomPreset_removesOnlyTargetPreset() {
        val custom1 = EqualizerPreset("c1", "Custom 1", false, listOf(1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f, 10f))
        val custom2 = EqualizerPreset("c2", "Custom 2", false, listOf(-1f, -2f, -3f, -4f, -5f, -6f, -7f, -8f, -9f, -10f))

        repository.saveCustomPreset(custom1)
        repository.saveCustomPreset(custom2)
        assertEquals(2, repository.getCustomPresets().size)

        val deleted = repository.deleteCustomPreset("c1")
        assertTrue(deleted)

        val remaining = repository.getCustomPresets()
        assertEquals(1, remaining.size)
        assertEquals("c2", remaining[0].id)
    }

    @Test
    fun cannotDeleteBuiltInPreset() {
        val deleted = repository.deleteCustomPreset("preset_flat")
        assertFalse(deleted)
        assertTrue(repository.getAllPresets().any { it.id == "preset_flat" })
    }
}
