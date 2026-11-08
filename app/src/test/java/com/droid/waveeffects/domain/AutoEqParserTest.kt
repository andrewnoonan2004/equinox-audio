package com.droid.waveeffects.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoEqParserTest {

    @Test
    fun parseGraphicEq_extractsFrequenciesAndInterpolatesTo10Bands() {
        val sampleGraphicEq = """
            GraphicEQ: 20 2.5; 31 4.0; 62 3.0; 125 1.5; 250 -0.5; 500 -1.0; 1000 0.5; 2000 2.0; 4000 1.0; 8000 -2.0; 16000 -3.5; 20000 -4.0
        """.trimIndent()

        val parsed = AutoEqParser.parse(
            name = "Test Headphone GraphicEQ",
            content = sampleGraphicEq
        )

        assertNotNull(parsed)
        assertEquals("Test Headphone GraphicEQ", parsed.name)
        assertEquals(10, parsed.bandGains.size)

        // 31 Hz should be close to 4.0 dB
        assertEquals(4.0f, parsed.bandGains[0], 0.2f)
        // 62 Hz should be close to 3.0 dB
        assertEquals(3.0f, parsed.bandGains[1], 0.2f)
        // 500 Hz should be close to -1.0 dB
        assertEquals(-1.0f, parsed.bandGains[4], 0.2f)
        // 16000 Hz should be close to -3.5 dB
        assertEquals(-3.5f, parsed.bandGains[9], 0.2f)
    }

    @Test
    fun parseParametricEq_calculatesBiquadFilterGainsAtCenterFrequencies() {
        val sampleParametric = """
            Preamp: -3.5 dB
            Filter 1: ON PK Fc 31 Hz Gain 4.5 dB Q 1.41
            Filter 2: ON PK Fc 250 Hz Gain -2.0 dB Q 1.8
            Filter 3: ON PK Fc 1000 Hz Gain 1.5 dB Q 2.0
            Filter 4: ON PK Fc 4000 Hz Gain 3.0 dB Q 2.5
            Filter 5: OFF PK Fc 8000 Hz Gain -5.0 dB Q 2.0
        """.trimIndent()

        val parsed = AutoEqParser.parse(
            name = "Test Headphone PEQ",
            content = sampleParametric
        )

        assertNotNull(parsed)
        assertEquals("Test Headphone PEQ", parsed.name)
        assertEquals(10, parsed.bandGains.size)

        // Filter 1 is at 31 Hz: Preamp (-3.5) + Filter1 (4.5) = +1.0 dB approx
        assertEquals(1.0f, parsed.bandGains[0], 0.3f)

        // Filter 5 is OFF, so it should not reduce gain at 8000 Hz
        // At 8000 Hz, with preamp -3.5 and other filters far away: ~ -3.5 dB
        assertEquals(-3.5f, parsed.bandGains[8], 0.5f)
    }

    @Test
    fun autoEqDatabase_containsPopularHeadphones() {
        val database = AutoEqDatabase.headphoneProfiles
        assertTrue(database.isNotEmpty())
        val names = database.map { it.name }

        assertTrue(names.any { it.contains("Sony WH-1000XM5") })
        assertTrue(names.any { it.contains("Apple AirPods Pro") })
        assertTrue(names.any { it.contains("Sennheiser HD 600") })
        assertTrue(names.any { it.contains("Beyerdynamic DT 770") })
        assertTrue(names.any { it.contains("Audio-Technica ATH-M50x") })

        val sonyM5 = database.first { it.name.contains("Sony WH-1000XM5") }
        assertEquals(10, sonyM5.bandGains.size)
        // Sony XM5 typically has bass taming or upper mid boost in Harman target
        assertNotNull(sonyM5.id)
    }
}
