package com.droid.waveeffects.dsp

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.audiofx.BassBoost
import android.media.audiofx.DynamicsProcessing
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.PresetReverb
import android.media.audiofx.Virtualizer
import android.media.audiofx.Visualizer
import android.util.Log
import androidx.core.content.ContextCompat
import com.droid.waveeffects.domain.CompressorConfig
import com.droid.waveeffects.domain.EqualizerBand
import com.droid.waveeffects.domain.LimiterConfig
import com.droid.waveeffects.domain.ReverbPreset

class AndroidDspEngine(
    private val context: Context
) : DspEngine {

    companion object {
        private const val TAG = "WaveEffectsDSP"
        private const val CHANNEL_COUNT = 2 // Stereo processing
    }

    override var currentSessionId: Int = 0
        private set

    override var isMasterEnabled: Boolean = true
        private set

    private var dynamicsProcessing: DynamicsProcessing? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var presetReverb: PresetReverb? = null
    private var visualizer: Visualizer? = null

    // Cached states
    private var cachedBands = EqualizerBand.createDefault10Bands()
    private var cachedBassBoostEnabled = false
    private var cachedBassBoostStrength = 0
    private var cachedVirtualizerEnabled = false
    private var cachedVirtualizerStrength = 0
    private var cachedLoudnessEnabled = false
    private var cachedLoudnessGainMb = 0
    private var cachedReverbPreset = ReverbPreset.NONE
    private var cachedCompressor = CompressorConfig.defaultConfig()
    private var cachedLimiter = LimiterConfig.defaultConfig()

    init {
        attachSession(0)
    }

    override fun attachSession(sessionId: Int) {
        if (currentSessionId == sessionId && dynamicsProcessing != null) {
            return
        }

        releaseCurrentEffects()
        currentSessionId = sessionId
        initializeEffects(sessionId)
    }

    override fun detachSession(sessionId: Int) {
        if (currentSessionId == sessionId) {
            attachSession(0) // Fallback to global mix session
        }
    }

    private fun initializeEffects(sessionId: Int) {
        runCatching {
            // 1. DynamicsProcessing (PreEQ 10 bands, MBC, Limiter)
            val dpConfigBuilder = DynamicsProcessing.Config.Builder(
                DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION,
                CHANNEL_COUNT,
                true, // preEqInUse
                EqualizerBand.DEFAULT_FREQUENCIES.size, // 10 bands
                true, // mbcInUse
                1, // mbcBandCount
                false, // postEqInUse
                0,
                true // limiterInUse
            )
            val dpConfig = dpConfigBuilder.build()
            val dp = DynamicsProcessing(0, sessionId, dpConfig)
            dp.enabled = isMasterEnabled
            dynamicsProcessing = dp
        }.onFailure {
            Log.w(TAG, "DynamicsProcessing not supported on session $sessionId: ${it.message}")
        }

        runCatching {
            // 2. Bass Boost
            val bb = BassBoost(0, sessionId)
            bb.enabled = isMasterEnabled && cachedBassBoostEnabled
            if (bb.strengthSupported) {
                bb.setStrength(cachedBassBoostStrength.toShort())
            }
            bassBoost = bb
        }.onFailure {
            Log.w(TAG, "BassBoost init failed: ${it.message}")
        }

        runCatching {
            // 3. Virtualizer (Spatializer)
            val virt = Virtualizer(0, sessionId)
            virt.enabled = isMasterEnabled && cachedVirtualizerEnabled
            if (virt.strengthSupported) {
                virt.setStrength(cachedVirtualizerStrength.toShort())
            }
            virtualizer = virt
        }.onFailure {
            Log.w(TAG, "Virtualizer init failed: ${it.message}")
        }

        runCatching {
            // 4. Loudness Enhancer
            val le = LoudnessEnhancer(sessionId)
            le.enabled = isMasterEnabled && cachedLoudnessEnabled
            le.setTargetGain(cachedLoudnessGainMb)
            loudnessEnhancer = le
        }.onFailure {
            Log.w(TAG, "LoudnessEnhancer init failed: ${it.message}")
        }

        runCatching {
            // 5. Preset Reverb
            val pr = PresetReverb(0, sessionId)
            pr.enabled = isMasterEnabled && (cachedReverbPreset != ReverbPreset.NONE)
            pr.preset = cachedReverbPreset.id
            presetReverb = pr
        }.onFailure {
            Log.w(TAG, "PresetReverb init failed: ${it.message}")
        }

        // 6. Visualizer (only if RECORD_AUDIO granted)
        initializeVisualizer(sessionId)

        // Apply cached configurations
        applyBandsToDp()
        applyCompressorToDp()
        applyLimiterToDp()
    }

    private fun releaseCurrentEffects() {
        runCatching { dynamicsProcessing?.release() }
        dynamicsProcessing = null

        runCatching { bassBoost?.release() }
        bassBoost = null

        runCatching { virtualizer?.release() }
        virtualizer = null

        runCatching { loudnessEnhancer?.release() }
        loudnessEnhancer = null

        runCatching { presetReverb?.release() }
        presetReverb = null

        runCatching { visualizer?.release() }
        visualizer = null
    }

    override fun setMasterEnabled(enabled: Boolean) {
        isMasterEnabled = enabled
        runCatching { dynamicsProcessing?.enabled = enabled }
        runCatching { bassBoost?.enabled = enabled && cachedBassBoostEnabled }
        runCatching { virtualizer?.enabled = enabled && cachedVirtualizerEnabled }
        runCatching { loudnessEnhancer?.enabled = enabled && cachedLoudnessEnabled }
        runCatching { presetReverb?.enabled = enabled && (cachedReverbPreset != ReverbPreset.NONE) }
        runCatching { visualizer?.enabled = enabled }
    }

    override fun setBands(bands: List<EqualizerBand>) {
        cachedBands = bands
        applyBandsToDp()
    }

    override fun setBandGain(bandIndex: Int, gainDb: Float) {
        if (bandIndex in cachedBands.indices) {
            val updated = cachedBands.toMutableList()
            updated[bandIndex] = updated[bandIndex].withGain(gainDb)
            cachedBands = updated
            applySingleBandGain(bandIndex, cachedBands[bandIndex].frequencyHz.toFloat(), cachedBands[bandIndex].gainDb)
        }
    }

    private fun applyBandsToDp() {
        val dp = dynamicsProcessing ?: return
        runCatching {
            cachedBands.forEachIndexed { index, band ->
                val eqBand = DynamicsProcessing.EqBand(
                    true,
                    band.frequencyHz.toFloat(),
                    band.gainDb
                )
                dp.setPreEqBandAllChannelsTo(index, eqBand)
            }
        }.onFailure {
            Log.w(TAG, "Failed to apply EQ bands to DynamicsProcessing: ${it.message}")
        }
    }

    private fun applySingleBandGain(bandIndex: Int, freqHz: Float, gainDb: Float) {
        val dp = dynamicsProcessing ?: return
        runCatching {
            val eqBand = DynamicsProcessing.EqBand(true, freqHz, gainDb)
            dp.setPreEqBandAllChannelsTo(bandIndex, eqBand)
        }.onFailure {
            Log.w(TAG, "Failed to apply band gain: ${it.message}")
        }
    }

    override fun setBassBoost(enabled: Boolean, strength: Int) {
        cachedBassBoostEnabled = enabled
        cachedBassBoostStrength = strength.coerceIn(0, 1000)
        runCatching {
            bassBoost?.let {
                it.enabled = isMasterEnabled && enabled
                if (it.strengthSupported) {
                    it.setStrength(cachedBassBoostStrength.toShort())
                }
            }
        }
    }

    override fun setVirtualizer(enabled: Boolean, strength: Int) {
        cachedVirtualizerEnabled = enabled
        cachedVirtualizerStrength = strength.coerceIn(0, 1000)
        runCatching {
            virtualizer?.let {
                it.enabled = isMasterEnabled && enabled
                if (it.strengthSupported) {
                    it.setStrength(cachedVirtualizerStrength.toShort())
                }
            }
        }
    }

    override fun setLoudnessGain(enabled: Boolean, gainMb: Int) {
        cachedLoudnessEnabled = enabled
        cachedLoudnessGainMb = gainMb.coerceIn(0, 1200)
        runCatching {
            loudnessEnhancer?.let {
                it.enabled = isMasterEnabled && enabled
                it.setTargetGain(cachedLoudnessGainMb)
            }
        }
    }

    override fun setReverb(preset: ReverbPreset) {
        cachedReverbPreset = preset
        runCatching {
            presetReverb?.let {
                it.enabled = isMasterEnabled && (preset != ReverbPreset.NONE)
                it.preset = preset.id
            }
        }
    }

    override fun setCompressor(config: CompressorConfig) {
        cachedCompressor = config
        applyCompressorToDp()
    }

    private fun applyCompressorToDp() {
        val dp = dynamicsProcessing ?: return
        runCatching {
            val mbcConfig = DynamicsProcessing.Mbc(
                true,
                cachedCompressor.enabled,
                1
            )
            dp.setMbcAllChannelsTo(mbcConfig)

            val mbcBand = DynamicsProcessing.MbcBand(
                cachedCompressor.enabled,
                20000f, // Full band cutoff
                cachedCompressor.attackMs,
                cachedCompressor.releaseMs,
                cachedCompressor.ratio,
                cachedCompressor.thresholdDb,
                cachedCompressor.kneeWidthDb,
                -90f, // Noise gate threshold
                1f,   // Expander ratio
                0f,   // Pre gain
                cachedCompressor.makeupGainDb
            )
            dp.setMbcBandAllChannelsTo(0, mbcBand)
        }.onFailure {
            Log.w(TAG, "Failed to apply Compressor to DynamicsProcessing: ${it.message}")
        }
    }

    override fun setLimiter(config: LimiterConfig) {
        cachedLimiter = config
        applyLimiterToDp()
    }

    private fun applyLimiterToDp() {
        val dp = dynamicsProcessing ?: return
        runCatching {
            val limiter = DynamicsProcessing.Limiter(
                true,
                cachedLimiter.enabled,
                0, // Link group
                cachedLimiter.attackMs,
                cachedLimiter.releaseMs,
                cachedLimiter.ratio,
                cachedLimiter.thresholdDb,
                cachedLimiter.postGainDb
            )
            dp.setLimiterAllChannelsTo(limiter)
        }.onFailure {
            Log.w(TAG, "Failed to apply Limiter to DynamicsProcessing: ${it.message}")
        }
    }

    private fun initializeVisualizer(sessionId: Int) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "RECORD_AUDIO permission not granted; Visualizer deferred")
            return
        }

        runCatching {
            runCatching { visualizer?.release() }
            visualizer = null

            val vis = Visualizer(sessionId)
            val range = Visualizer.getCaptureSizeRange()
            if (range != null && range.size >= 2) {
                vis.captureSize = range[1].coerceAtMost(512)
            }
            vis.enabled = isMasterEnabled
            visualizer = vis
        }.onFailure {
            Log.w(TAG, "Visualizer init failed: ${it.message}")
            visualizer = null
        }
    }

    fun refreshVisualizer() {
        initializeVisualizer(currentSessionId)
    }

    override fun getVisualizerFft(bytes: ByteArray): Boolean {
        return runCatching {
            val vis = visualizer ?: return false
            if (!vis.enabled) return false
            val capSize = vis.captureSize
            val buf = if (bytes.size == capSize) bytes else ByteArray(capSize)
            val success = vis.getFft(buf) == Visualizer.SUCCESS
            if (success && buf !== bytes) {
                System.arraycopy(buf, 0, bytes, 0, minOf(buf.size, bytes.size))
            }
            success
        }.getOrDefault(false)
    }

    override fun getVisualizerWaveform(bytes: ByteArray): Boolean {
        return runCatching {
            val vis = visualizer ?: return false
            if (!vis.enabled) return false
            val capSize = vis.captureSize
            val buf = if (bytes.size == capSize) bytes else ByteArray(capSize)
            val success = vis.getWaveForm(buf) == Visualizer.SUCCESS
            if (success && buf !== bytes) {
                System.arraycopy(buf, 0, bytes, 0, minOf(buf.size, bytes.size))
            }
            success
        }.getOrDefault(false)
    }

    override fun release() {
        releaseCurrentEffects()
    }
}
