package com.droid.waveeffects.domain

data class AutoEqHeadphone(
    val id: String,
    val name: String,
    val brand: String,
    val targetType: String, // "Harman Over-Ear", "Harman In-Ear", "Diffuse Field"
    val bandGains: List<Float>,
    val preampDb: Float = 0f
) {
    fun toPreset(): EqualizerPreset {
        return EqualizerPreset(
            id = "autoeq_$id",
            name = name,
            isBuiltIn = false,
            bandGains = bandGains,
            bassBoostStrength = 0,
            virtualizerStrength = 0,
            loudnessDb = 0f
        )
    }
}

object AutoEqDatabase {

    val headphoneProfiles = listOf(
        AutoEqHeadphone(
            id = "sony_wh1000xm5",
            name = "Sony WH-1000XM5 (Harman Target)",
            brand = "Sony",
            targetType = "Harman Over-Ear",
            bandGains = listOf(-2.5f, -3.0f, -2.0f, -0.5f, 0.5f, 1.8f, 2.5f, -1.0f, 1.5f, -2.0f),
            preampDb = -2.5f
        ),
        AutoEqHeadphone(
            id = "sony_wh1000xm4",
            name = "Sony WH-1000XM4 (Harman Target)",
            brand = "Sony",
            targetType = "Harman Over-Ear",
            bandGains = listOf(-4.0f, -4.5f, -2.5f, -0.5f, 0.8f, 2.2f, 3.0f, 0.5f, 1.0f, -1.5f),
            preampDb = -3.0f
        ),
        AutoEqHeadphone(
            id = "apple_airpods_pro_2",
            name = "Apple AirPods Pro 2 (Harman In-Ear)",
            brand = "Apple",
            targetType = "Harman In-Ear",
            bandGains = listOf(0.5f, 0.0f, -0.5f, -0.8f, 0.0f, 0.8f, 1.2f, -1.5f, 2.0f, 0.5f),
            preampDb = -1.2f
        ),
        AutoEqHeadphone(
            id = "apple_airpods_max",
            name = "Apple AirPods Max (Harman Over-Ear)",
            brand = "Apple",
            targetType = "Harman Over-Ear",
            bandGains = listOf(-1.0f, -1.5f, -0.5f, 0.0f, 0.5f, 1.0f, 1.8f, 0.0f, -1.5f, -1.0f),
            preampDb = -1.8f
        ),
        AutoEqHeadphone(
            id = "sennheiser_hd600",
            name = "Sennheiser HD 600 (Harman Bass Extension)",
            brand = "Sennheiser",
            targetType = "Harman Over-Ear",
            bandGains = listOf(6.0f, 4.5f, 2.0f, 0.5f, -0.5f, 0.0f, 0.5f, -1.0f, 1.5f, 2.0f),
            preampDb = -6.0f
        ),
        AutoEqHeadphone(
            id = "sennheiser_hd650",
            name = "Sennheiser HD 650 / 6XX (Harman Target)",
            brand = "Sennheiser",
            targetType = "Harman Over-Ear",
            bandGains = listOf(5.5f, 4.0f, 1.5f, 0.0f, -0.8f, 0.0f, 1.0f, -0.5f, 1.8f, 2.5f),
            preampDb = -5.5f
        ),
        AutoEqHeadphone(
            id = "beyer_dt770_80",
            name = "Beyerdynamic DT 770 Pro 80Ω (Harman Target)",
            brand = "Beyerdynamic",
            targetType = "Harman Over-Ear",
            bandGains = listOf(-1.5f, -2.0f, -1.0f, 0.5f, 1.0f, 1.5f, 2.0f, -4.5f, -3.0f, 1.0f),
            preampDb = -2.0f
        ),
        AutoEqHeadphone(
            id = "beyer_dt990_pro",
            name = "Beyerdynamic DT 990 Pro (Treble Tamed)",
            brand = "Beyerdynamic",
            targetType = "Harman Over-Ear",
            bandGains = listOf(4.5f, 3.0f, 0.5f, -0.5f, 0.0f, 0.5f, 1.0f, -5.5f, -4.0f, 0.5f),
            preampDb = -4.5f
        ),
        AutoEqHeadphone(
            id = "audiotechnica_m50x",
            name = "Audio-Technica ATH-M50x (Harman Target)",
            brand = "Audio-Technica",
            targetType = "Harman Over-Ear",
            bandGains = listOf(-3.0f, -2.5f, -1.5f, -0.5f, 0.8f, 1.5f, 2.2f, -1.0f, -2.0f, 0.5f),
            preampDb = -2.2f
        ),
        AutoEqHeadphone(
            id = "moondrop_chu_2",
            name = "Moondrop Chu II (Harman In-Ear)",
            brand = "Moondrop",
            targetType = "Harman In-Ear",
            bandGains = listOf(-0.5f, -0.5f, 0.0f, 0.2f, 0.5f, 0.8f, 1.5f, -2.0f, 1.0f, -0.5f),
            preampDb = -1.5f
        ),
        AutoEqHeadphone(
            id = "samsung_buds2_pro",
            name = "Samsung Galaxy Buds2 Pro (Harman In-Ear)",
            brand = "Samsung",
            targetType = "Harman In-Ear",
            bandGains = listOf(0.0f, -0.5f, -0.5f, 0.0f, 0.2f, 0.5f, 1.0f, -1.0f, 1.2f, 0.0f),
            preampDb = -1.0f
        )
    )
}
