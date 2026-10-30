package com.equinox.audioeq.audio

import android.media.audiofx.BassBoost
import android.media.audiofx.DynamicsProcessing
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import android.util.Log
import com.equinox.audioeq.model.EqState
import com.equinox.audioeq.model.GRAPHIC_BAND_FREQUENCIES
import com.equinox.audioeq.model.ResponseCurve
import kotlin.math.log10
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Owns the audio effects attached to one audio session.
 *
 * [DynamicsProcessing] is the primary engine (pre-EQ for graphic bands + profile, post-EQ for
 * tweak modules, brickwall limiter). The legacy [Equalizer] and [BassBoost] are only used when
 * DynamicsProcessing cannot be created, so the curve is never applied twice.
 * [Virtualizer] and [LoudnessEnhancer] have no DynamicsProcessing equivalent and are always used.
 * Virtualizer is deprecated in favour of Spatializer, which apps cannot apply to other apps' audio.
 */
@Suppress("DEPRECATION")
class DspManager(private val sessionId: Int) {

    private val dynamics: DynamicsProcessing? = create("DynamicsProcessing") {
        val config = DynamicsProcessing.Config.Builder(
            DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION,
            CHANNELS,
            true, PRE_EQ_CUTOFFS.size,
            false, 0,
            true, POST_EQ_CENTERS.size,
            true,
        ).build()
        DynamicsProcessing(0, sessionId, config)
    }
    private val equalizer: Equalizer? = if (dynamics == null) create("Equalizer") { Equalizer(0, sessionId) } else null
    private val bassBoost: BassBoost? = if (dynamics == null) create("BassBoost") { BassBoost(0, sessionId) } else null
    private val virtualizer: Virtualizer? = create("Virtualizer") { Virtualizer(0, sessionId) }
    private val loudness: LoudnessEnhancer? = create("LoudnessEnhancer") { LoudnessEnhancer(sessionId) }

    /** True when at least one EQ-capable effect attached to the session. */
    val isAttached: Boolean get() = dynamics != null || equalizer != null

    fun applyState(state: EqState) {
        val on = state.isEnabled
        dynamics?.let { applyDynamics(it, state) }
        equalizer?.let { applyLegacyEqualizer(it, state) }

        bassBoost?.runCatching {
            if (strengthSupported) setStrength((state.bassBoostStrength * 1000f).roundToInt().coerceIn(0, 1000).toShort())
            enabled = on && state.bassBoostEnabled
        }
        virtualizer?.runCatching {
            if (strengthSupported) setStrength(state.virtualizerStrength.roundToInt().coerceIn(0, 1000).toShort())
            enabled = on && state.virtualizerEnabled
        }
        loudness?.runCatching {
            setTargetGain((state.loudnessGainDb * 100f).roundToInt())
            enabled = on && state.loudnessEqualizerEnabled
        }
    }

    private fun applyDynamics(dp: DynamicsProcessing, state: EqState) = runCatching {
        val preEq = DynamicsProcessing.Eq(true, true, PRE_EQ_CUTOFFS.size)
        PRE_EQ_CUTOFFS.forEachIndexed { i, cutoff ->
            val center = GRAPHIC_BAND_FREQUENCIES[i].toFloat()
            preEq.setBand(i, DynamicsProcessing.EqBand(true, cutoff, ResponseCurve.baseGainDb(center, state)))
        }
        val postEq = DynamicsProcessing.Eq(true, true, POST_EQ_CENTERS.size)
        POST_EQ_CENTERS.forEachIndexed { i, center ->
            postEq.setBand(i, DynamicsProcessing.EqBand(true, POST_EQ_CUTOFFS[i], ResponseCurve.tweakGainDb(center, state)))
        }
        val limiter = DynamicsProcessing.Limiter(
            true, state.limiterEnabled, 0,
            1.0f, 60.0f, 50.0f, -0.5f, 0f,
        )
        val preamp = state.customPreampDb + (state.selectedProfile?.preampDb ?: 0f)
        for (channel in 0 until CHANNELS) {
            dp.setPreEqByChannelIndex(channel, preEq)
            dp.setPostEqByChannelIndex(channel, postEq)
            dp.setLimiterByChannelIndex(channel, limiter)
            dp.setInputGainbyChannel(channel, preamp + balanceGainDb(channel, state.channelBalance))
        }
        dp.enabled = state.isEnabled
    }.onFailure { Log.w(TAG, "DynamicsProcessing update failed for session $sessionId", it) }

    private fun applyLegacyEqualizer(eq: Equalizer, state: EqState) = runCatching {
        val (minLevel, maxLevel) = eq.bandLevelRange.let { it[0].toInt() to it[1].toInt() }
        for (band in 0 until eq.numberOfBands) {
            val centerHz = eq.getCenterFreq(band.toShort()) / 1000f
            val millibels = (ResponseCurve.totalGainDb(centerHz, state) * 100f).roundToInt()
            eq.setBandLevel(band.toShort(), millibels.coerceIn(minLevel, maxLevel).toShort())
        }
        eq.enabled = state.isEnabled
    }.onFailure { Log.w(TAG, "Equalizer update failed for session $sessionId", it) }

    /** Attenuates the opposite channel; balance in -1 (left) .. 1 (right). */
    private fun balanceGainDb(channel: Int, balance: Float): Float {
        val attenuation = if (channel == 0) balance.coerceAtLeast(0f) else (-balance).coerceAtLeast(0f)
        val linear = (1f - attenuation).coerceAtLeast(0.001f)
        return 20f * log10(linear)
    }

    fun release() {
        listOfNotNull(dynamics, equalizer, bassBoost, virtualizer, loudness).forEach { runCatching { it.release() } }
    }

    private inline fun <T> create(name: String, block: () -> T): T? = try {
        block()
    } catch (e: Exception) {
        // Session 0 (global mix) effects are deprecated and unsupported on many devices.
        Log.w(TAG, "$name unavailable for session $sessionId: ${e.message}")
        null
    }

    companion object {
        private const val TAG = "DspManager"
        private const val CHANNELS = 2

        /** Upper band edges: geometric midpoints between the 10 graphic band centers. */
        private val PRE_EQ_CUTOFFS: List<Float> = GRAPHIC_BAND_FREQUENCIES.mapIndexed { i, f ->
            GRAPHIC_BAND_FREQUENCIES.getOrNull(i + 1)?.let { sqrt(f.toFloat() * it) } ?: ResponseCurve.MAX_HZ
        }
        private val POST_EQ_CENTERS = listOf(45f, 120f, 400f, 2500f, 7000f, 14000f)
        private val POST_EQ_CUTOFFS = listOf(80f, 220f, 1000f, 4500f, 10000f, ResponseCurve.MAX_HZ)
    }
}
