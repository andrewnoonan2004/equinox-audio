package com.equinox.audioeq.model

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Single source of truth for the combined frequency response, shared by the visualizer
 * and [com.equinox.audioeq.audio.DspManager] so what the user sees is what gets applied.
 */
object ResponseCurve {
    const val MIN_HZ = 20f
    const val MAX_HZ = 20_000f

    const val BASS_MAX_DB = 12f
    const val TREBLE_MAX_DB = 10f
    const val VOCAL_MAX_DB = 6f
    const val VOCAL_CENTER_HZ = 2500f

    private const val SAMPLE_RATE = 48_000.0

    /** Graphic sliders + selected profile, excluding tweak modules. */
    fun baseGainDb(freqHz: Float, state: EqState): Float {
        val graphic = interpolateLog(freqHz, state.graphicBands.associate { it.frequencyHz to it.gainDb })
        return graphic + profileGainDb(freqHz, state.selectedProfile)
    }

    /** Bass, treble and vocal tweak modules only. */
    fun tweakGainDb(freqHz: Float, state: EqState): Float {
        var gain = 0f
        if (state.bassBoostEnabled) {
            gain += state.bassBoostStrength * BASS_MAX_DB * lowShelfShape(freqHz, state.bassCutoffFreqHz)
        }
        if (state.trebleBoostEnabled) {
            gain += state.trebleBoostStrength * TREBLE_MAX_DB * highShelfShape(freqHz, state.trebleShelfFreqHz)
        }
        if (state.vocalClarityEnabled) {
            val octaves = log2(freqHz / VOCAL_CENTER_HZ)
            gain += state.vocalClarityStrength * VOCAL_MAX_DB * exp(-(octaves * octaves) / 0.5f)
        }
        return gain
    }

    fun totalGainDb(freqHz: Float, state: EqState): Float =
        baseGainDb(freqHz, state) + tweakGainDb(freqHz, state)

    fun profileGainDb(freqHz: Float, profile: AutoEqProfile?): Float {
        profile ?: return 0f
        return if (profile.graphicEqBands.size >= 10 || profile.parametricFilters.isEmpty()) {
            interpolateLog(freqHz, profile.graphicEqBands)
        } else {
            profile.parametricFilters.sumOf { biquadMagnitudeDb(it, freqHz.toDouble()) }.toFloat()
        }
    }

    /** Full gain below [cutoffHz], fading to zero by twice the cutoff. */
    private fun lowShelfShape(freqHz: Float, cutoffHz: Float): Float = when {
        freqHz <= cutoffHz -> 1f
        freqHz >= cutoffHz * 2f -> 0f
        else -> smoothStep(1f - log2(freqHz / cutoffHz))
    }

    /** Zero below 0.7x the shelf frequency, full gain from the shelf frequency upward. */
    private fun highShelfShape(freqHz: Float, shelfHz: Float): Float {
        val start = shelfHz * 0.7f
        return when {
            freqHz <= start -> 0f
            freqHz >= shelfHz -> 1f
            else -> smoothStep(ln(freqHz / start) / ln(shelfHz / start))
        }
    }

    private fun smoothStep(t: Float): Float = t * t * (3f - 2f * t)

    private fun log2(x: Float): Float = (ln(x.toDouble()) / ln(2.0)).toFloat()

    fun interpolateLog(freqHz: Float, points: Map<Int, Float>): Float {
        if (points.isEmpty()) return 0f
        val sorted = points.entries.sortedBy { it.key }
        if (freqHz <= sorted.first().key) return sorted.first().value
        if (freqHz >= sorted.last().key) return sorted.last().value
        val upperIndex = sorted.indexOfFirst { it.key >= freqHz }
        val lo = sorted[upperIndex - 1]
        val hi = sorted[upperIndex]
        val t = ln(freqHz / lo.key) / ln(hi.key.toFloat() / lo.key)
        return lo.value + (hi.value - lo.value) * t
    }

    /** RBJ cookbook biquad magnitude response, matching how AutoEq defines its filters. */
    fun biquadMagnitudeDb(filter: ParametricFilter, freqHz: Double): Double {
        val a = 10.0.pow(filter.gainDb / 40.0)
        val w0 = 2 * PI * filter.frequencyHz / SAMPLE_RATE
        val cw = cos(w0)
        val alpha = sin(w0) / (2 * filter.qFactor.coerceAtLeast(0.05f))
        val sa = 2 * sqrt(a) * alpha
        val (b, d) = when (filter.type) {
            FilterType.PEAKING -> doubleArrayOf(1 + alpha * a, -2 * cw, 1 - alpha * a) to
                doubleArrayOf(1 + alpha / a, -2 * cw, 1 - alpha / a)
            FilterType.LOW_SHELF -> doubleArrayOf(
                a * ((a + 1) - (a - 1) * cw + sa), 2 * a * ((a - 1) - (a + 1) * cw), a * ((a + 1) - (a - 1) * cw - sa),
            ) to doubleArrayOf((a + 1) + (a - 1) * cw + sa, -2 * ((a - 1) + (a + 1) * cw), (a + 1) + (a - 1) * cw - sa)
            FilterType.HIGH_SHELF -> doubleArrayOf(
                a * ((a + 1) + (a - 1) * cw + sa), -2 * a * ((a - 1) + (a + 1) * cw), a * ((a + 1) + (a - 1) * cw - sa),
            ) to doubleArrayOf((a + 1) - (a - 1) * cw + sa, 2 * ((a - 1) - (a + 1) * cw), (a + 1) - (a - 1) * cw - sa)
            FilterType.LOW_PASS -> doubleArrayOf((1 - cw) / 2, 1 - cw, (1 - cw) / 2) to
                doubleArrayOf(1 + alpha, -2 * cw, 1 - alpha)
            FilterType.HIGH_PASS -> doubleArrayOf((1 + cw) / 2, -(1 + cw), (1 + cw) / 2) to
                doubleArrayOf(1 + alpha, -2 * cw, 1 - alpha)
        }
        val w = 2 * PI * freqHz / SAMPLE_RATE
        val (c1, s1, c2, s2) = listOf(cos(w), -sin(w), cos(2 * w), -sin(2 * w))
        val numRe = b[0] + b[1] * c1 + b[2] * c2
        val numIm = b[1] * s1 + b[2] * s2
        val denRe = d[0] + d[1] * c1 + d[2] * c2
        val denIm = d[1] * s1 + d[2] * s2
        val ratio = sqrt((numRe * numRe + numIm * numIm) / (denRe * denRe + denIm * denIm))
        return 20 * log10(ratio.coerceAtLeast(1e-9))
    }

    /** Log-spaced frequencies between [MIN_HZ] and [MAX_HZ]. */
    fun logFrequencies(count: Int): List<Float> = List(count) { i ->
        MIN_HZ * (MAX_HZ / MIN_HZ).pow(i / (count - 1f))
    }
}
