package com.droid.waveeffects.domain

import java.util.UUID
import kotlin.math.log10
import kotlin.math.pow

data class ParametricFilter(
    val enabled: Boolean,
    val type: String, // "PK", "LS", "HS"
    val frequencyHz: Float,
    val gainDb: Float,
    val q: Float
) {
    fun calculateGainAt(f: Float): Float {
        if (!enabled) return 0f
        return when (type) {
            "PK", "PEAK", "PEAKING" -> {
                val f0 = frequencyHz
                if (f0 <= 0f || f <= 0f) return 0f
                val delta = (f * f - f0 * f0) / (f * f0)
                val denom = 1.0 + (q * delta).toDouble().pow(2.0)
                (gainDb / denom).toFloat()
            }
            "LS", "LOW_SHELF" -> {
                if (f <= frequencyHz) gainDb else (gainDb * (frequencyHz / f)).coerceIn(-12f, 12f)
            }
            "HS", "HIGH_SHELF" -> {
                if (f >= frequencyHz) gainDb else (gainDb * (f / frequencyHz)).coerceIn(-12f, 12f)
            }
            else -> 0f
        }
    }
}

object AutoEqParser {

    private val targetFrequencies = EqualizerBand.DEFAULT_FREQUENCIES.map { it.toFloat() }

    fun parse(name: String, content: String): EqualizerPreset {
        val trimmed = content.trim()
        val bandGains = if (trimmed.contains("GraphicEQ:", ignoreCase = true)) {
            parseGraphicEq(trimmed)
        } else {
            parseParametricEq(trimmed)
        }

        return EqualizerPreset(
            id = "autoeq_" + UUID.randomUUID().toString().take(8),
            name = name.ifBlank { "AutoEQ Preset" },
            isBuiltIn = false,
            bandGains = bandGains.map { it.coerceIn(-12f, 12f) },
            bassBoostStrength = 0,
            virtualizerStrength = 0,
            loudnessDb = 0f
        )
    }

    private fun parseGraphicEq(content: String): List<Float> {
        val afterPrefix = content.substringAfter("GraphicEQ:", "").ifBlank { content }
        val points = mutableListOf<Pair<Float, Float>>()

        val tokens = afterPrefix.split(';', '\n', '\r')
        for (token in tokens) {
            val parts = token.trim().split("\\s+".toRegex())
            if (parts.size >= 2) {
                val freq = parts[0].toFloatOrNull()
                val gain = parts[1].toFloatOrNull()
                if (freq != null && gain != null) {
                    points.add(freq to gain)
                }
            }
        }

        if (points.isEmpty()) {
            return targetFrequencies.map { 0f }
        }

        points.sortBy { it.first }

        return targetFrequencies.map { targetF ->
            interpolateLog(points, targetF)
        }
    }

    private fun interpolateLog(points: List<Pair<Float, Float>>, targetF: Float): Float {
        // Direct match
        val exact = points.find { kotlin.math.abs(it.first - targetF) < 0.5f }
        if (exact != null) return exact.second

        // Below lowest
        if (targetF <= points.first().first) return points.first().second
        // Above highest
        if (targetF >= points.last().first) return points.last().second

        // Find surrounding points
        var lower = points.first()
        var upper = points.last()

        for (i in 0 until points.size - 1) {
            if (points[i].first <= targetF && points[i + 1].first >= targetF) {
                lower = points[i]
                upper = points[i + 1]
                break
            }
        }

        val logTarget = log10(targetF.toDouble())
        val logLower = log10(lower.first.toDouble())
        val logUpper = log10(upper.first.toDouble())
        val fraction = ((logTarget - logLower) / (logUpper - logLower)).toFloat().coerceIn(0f, 1f)

        return lower.second + fraction * (upper.second - lower.second)
    }

    private fun parseParametricEq(content: String): List<Float> {
        var preamp = 0f
        val filters = mutableListOf<ParametricFilter>()

        content.lines().forEach { line ->
            val clean = line.trim()
            if (clean.startsWith("Preamp:", ignoreCase = true)) {
                val preampStr = clean.substringAfter("Preamp:").replace("dB", "").trim()
                preamp = preampStr.toFloatOrNull() ?: 0f
            } else if (clean.startsWith("Filter", ignoreCase = true)) {
                // Example: Filter 1: ON PK Fc 31 Hz Gain 4.5 dB Q 1.41
                val isEnabled = clean.contains(" ON ", ignoreCase = true)
                val type = when {
                    clean.contains(" LS ", ignoreCase = true) -> "LS"
                    clean.contains(" HS ", ignoreCase = true) -> "HS"
                    else -> "PK"
                }

                val fc = extractNumberAfter(clean, "Fc ")
                val gain = extractNumberAfter(clean, "Gain ")
                val q = extractNumberAfter(clean, "Q ") ?: 1.41f

                if (fc != null && gain != null) {
                    filters.add(
                        ParametricFilter(
                            enabled = isEnabled,
                            type = type,
                            frequencyHz = fc,
                            gainDb = gain,
                            q = q
                        )
                    )
                }
            }
        }

        return targetFrequencies.map { f ->
            val filterSum = filters.sumOf { it.calculateGainAt(f).toDouble() }.toFloat()
            (preamp + filterSum)
        }
    }

    private fun extractNumberAfter(line: String, prefix: String): Float? {
        val idx = line.indexOf(prefix, ignoreCase = true)
        if (idx < 0) return null
        val sub = line.substring(idx + prefix.length).trim()
        val numStr = sub.takeWhile { it.isDigit() || it == '.' || it == '-' }
        return numStr.toFloatOrNull()
    }
}
