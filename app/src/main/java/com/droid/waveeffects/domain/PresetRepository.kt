package com.droid.waveeffects.domain

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class PresetRepository(
    private val presetsFile: File
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        prettyPrint = true
    }

    private val customPresets = mutableListOf<EqualizerPreset>()

    init {
        loadCustomPresets()
    }

    private fun loadCustomPresets() {
        customPresets.clear()
        if (presetsFile.exists()) {
            try {
                val content = presetsFile.readText()
                if (content.isNotBlank()) {
                    val loaded = json.decodeFromString<List<EqualizerPreset>>(content)
                    customPresets.addAll(loaded)
                }
            } catch (e: Exception) {
                // If corrupted, fallback gracefully
                customPresets.clear()
            }
        }
    }

    private fun persist() {
        try {
            val content = json.encodeToString(customPresets)
            presetsFile.parentFile?.mkdirs()
            presetsFile.writeText(content)
        } catch (_: Exception) {
        }
    }

    fun getAllPresets(): List<EqualizerPreset> {
        return EqualizerPreset.builtInPresets + customPresets
    }

    fun getCustomPresets(): List<EqualizerPreset> {
        return customPresets.toList()
    }

    fun getPresetById(id: String): EqualizerPreset? {
        return getAllPresets().find { it.id == id }
    }

    fun saveCustomPreset(preset: EqualizerPreset) {
        val normalized = preset.copy(isBuiltIn = false)
        val index = customPresets.indexOfFirst { it.id == normalized.id }
        if (index >= 0) {
            customPresets[index] = normalized
        } else {
            customPresets.add(normalized)
        }
        persist()
    }

    fun deleteCustomPreset(id: String): Boolean {
        // Built-in presets cannot be deleted
        if (EqualizerPreset.builtInPresets.any { it.id == id }) {
            return false
        }
        val removed = customPresets.removeAll { it.id == id }
        if (removed) {
            persist()
        }
        return removed
    }
}
