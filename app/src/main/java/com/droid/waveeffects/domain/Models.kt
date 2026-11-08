package com.droid.waveeffects.domain

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val jsonFormat = Json {
    ignoreUnknownKeys = true
    isLenient = true
    prettyPrint = true
}

@Serializable
data class EqualizerBand(
    val id: Int,
    val frequencyHz: Int,
    val gainDb: Float = 0f,
    val minGainDb: Float = -12f,
    val maxGainDb: Float = 12f
) {
    fun withGain(newGainDb: Float): EqualizerBand {
        val clamped = newGainDb.coerceIn(minGainDb, maxGainDb)
        return copy(gainDb = clamped)
    }

    val displayFrequency: String
        get() = if (frequencyHz >= 1000) {
            "${frequencyHz / 1000} kHz"
        } else {
            "$frequencyHz Hz"
        }

    val displayGain: String
        get() = if (gainDb > 0) "+%.1f dB".format(gainDb) else "%.1f dB".format(gainDb)

    companion object {
        val DEFAULT_FREQUENCIES = listOf(31, 62, 125, 250, 500, 1000, 2000, 4000, 8000, 16000)

        fun createDefault10Bands(): List<EqualizerBand> {
            return DEFAULT_FREQUENCIES.mapIndexed { index, freq ->
                EqualizerBand(id = index, frequencyHz = freq, gainDb = 0f)
            }
        }
    }
}

@Serializable
data class EqualizerPreset(
    val id: String,
    val name: String,
    val isBuiltIn: Boolean = false,
    val bandGains: List<Float>,
    val bassBoostStrength: Int = 0,
    val virtualizerStrength: Int = 0,
    val loudnessDb: Float = 0f
) {
    fun toJson(): String = jsonFormat.encodeToString(this)

    companion object {
        fun fromJson(json: String): EqualizerPreset = jsonFormat.decodeFromString(json)

        val builtInPresets = listOf(
            EqualizerPreset(
                id = "preset_flat",
                name = "Flat",
                isBuiltIn = true,
                bandGains = listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f),
                bassBoostStrength = 0,
                virtualizerStrength = 0,
                loudnessDb = 0f
            ),
            EqualizerPreset(
                id = "preset_bass_boost",
                name = "Bass Boost",
                isBuiltIn = true,
                bandGains = listOf(6.0f, 5.0f, 3.5f, 2.0f, 0.5f, 0f, 0f, 0.5f, 1.0f, 1.5f),
                bassBoostStrength = 600,
                virtualizerStrength = 150,
                loudnessDb = 1.5f
            ),
            EqualizerPreset(
                id = "preset_harman_target",
                name = "Harman Target",
                isBuiltIn = true,
                bandGains = listOf(5.5f, 4.0f, 2.5f, 0.5f, -0.5f, 0f, 1.5f, 3.0f, 2.0f, 1.0f),
                bassBoostStrength = 250,
                virtualizerStrength = 200,
                loudnessDb = 1.0f
            ),
            EqualizerPreset(
                id = "preset_vocal_clarity",
                name = "Vocal Clarity",
                isBuiltIn = true,
                bandGains = listOf(-2.5f, -1.5f, 0f, 1.5f, 3.0f, 4.0f, 3.5f, 2.0f, 0.5f, -1.0f),
                bassBoostStrength = 0,
                virtualizerStrength = 100,
                loudnessDb = 2.0f
            ),
            EqualizerPreset(
                id = "preset_club_electronic",
                name = "Club / Electronic",
                isBuiltIn = true,
                bandGains = listOf(5.0f, 4.5f, 2.0f, 0f, -1.0f, 1.0f, 2.5f, 4.0f, 4.5f, 5.0f),
                bassBoostStrength = 500,
                virtualizerStrength = 350,
                loudnessDb = 2.0f
            ),
            EqualizerPreset(
                id = "preset_rock_metal",
                name = "Rock & Metal",
                isBuiltIn = true,
                bandGains = listOf(4.5f, 3.5f, 1.5f, -1.0f, -1.5f, 0.5f, 2.5f, 3.5f, 4.0f, 4.5f),
                bassBoostStrength = 350,
                virtualizerStrength = 200,
                loudnessDb = 1.5f
            ),
            EqualizerPreset(
                id = "preset_acoustic_classical",
                name = "Acoustic / Classical",
                isBuiltIn = true,
                bandGains = listOf(3.0f, 2.5f, 1.5f, 1.0f, 0.5f, 1.0f, 2.0f, 2.5f, 3.0f, 3.5f),
                bassBoostStrength = 100,
                virtualizerStrength = 400,
                loudnessDb = 0.5f
            ),
            EqualizerPreset(
                id = "preset_treble_boost",
                name = "Treble Boost",
                isBuiltIn = true,
                bandGains = listOf(-1.0f, -0.5f, 0f, 0f, 0.5f, 1.5f, 3.0f, 4.5f, 6.0f, 7.0f),
                bassBoostStrength = 0,
                virtualizerStrength = 200,
                loudnessDb = 0f
            )
        )
    }
}

@Serializable
data class CompressorConfig(
    val enabled: Boolean = false,
    val thresholdDb: Float = -24.0f,
    val ratio: Float = 4.0f,
    val attackMs: Float = 15.0f,
    val releaseMs: Float = 100.0f,
    val kneeWidthDb: Float = 6.0f,
    val makeupGainDb: Float = 0.0f
) {
    companion object {
        fun defaultConfig() = CompressorConfig()
    }
}

@Serializable
data class LimiterConfig(
    val enabled: Boolean = true,
    val thresholdDb: Float = -0.5f,
    val attackMs: Float = 1.5f,
    val releaseMs: Float = 60.0f,
    val ratio: Float = 10.0f,
    val postGainDb: Float = 0.0f
) {
    companion object {
        fun defaultConfig() = LimiterConfig()
    }
}

enum class ReverbPreset(val id: Short, val displayName: String) {
    NONE(0, "None"),
    SMALL_ROOM(1, "Small Room"),
    MEDIUM_ROOM(2, "Medium Room"),
    LARGE_ROOM(3, "Large Room"),
    MEDIUM_HALL(4, "Medium Hall"),
    LARGE_HALL(5, "Large Hall"),
    PLATE(6, "Plate Reverb")
}

@Serializable
data class AudioSessionInfo(
    val sessionId: Int,
    val packageName: String,
    val appName: String,
    val timestamp: Long = System.currentTimeMillis()
)
