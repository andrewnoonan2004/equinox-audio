package com.equinox.audioeq.model

val GRAPHIC_BAND_FREQUENCIES = listOf(31, 62, 125, 250, 500, 1000, 2000, 4000, 8000, 16000)

data class BandSetting(val frequencyHz: Int, val gainDb: Float = 0f)

data class EqState(
    val isEnabled: Boolean = true,
    val selectedProfile: AutoEqProfile? = null,
    val customPreampDb: Float = 0f,
    val graphicBands: List<BandSetting> = GRAPHIC_BAND_FREQUENCIES.map { BandSetting(it) },

    val bassBoostEnabled: Boolean = false,
    val bassBoostStrength: Float = 0.5f,
    val bassCutoffFreqHz: Float = 90f,

    val trebleBoostEnabled: Boolean = false,
    val trebleBoostStrength: Float = 0.5f,
    val trebleShelfFreqHz: Float = 9000f,

    val vocalClarityEnabled: Boolean = false,
    val vocalClarityStrength: Float = 0.5f,

    val loudnessEqualizerEnabled: Boolean = false,
    val loudnessGainDb: Float = 3f,

    val virtualizerEnabled: Boolean = false,
    val virtualizerStrength: Float = 500f,

    val channelBalance: Float = 0f,

    val limiterEnabled: Boolean = true,
)
