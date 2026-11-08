package com.droid.waveeffects.dsp

import com.droid.waveeffects.domain.CompressorConfig
import com.droid.waveeffects.domain.EqualizerBand
import com.droid.waveeffects.domain.LimiterConfig
import com.droid.waveeffects.domain.ReverbPreset
import kotlin.math.sin

class FakeDspEngine : DspEngine {

    override var currentSessionId: Int = 0
        private set

    override var isMasterEnabled: Boolean = true
        private set

    var bands: List<EqualizerBand> = EqualizerBand.createDefault10Bands()
        private set

    var bassBoostEnabled: Boolean = false
        private set
    var bassBoostStrength: Int = 0
        private set

    var virtualizerEnabled: Boolean = false
        private set
    var virtualizerStrength: Int = 0
        private set

    var loudnessEnabled: Boolean = false
        private set
    var loudnessGainMb: Int = 0
        private set

    var reverbPreset: ReverbPreset = ReverbPreset.NONE
        private set

    var compressorConfig: CompressorConfig = CompressorConfig.defaultConfig()
        private set

    var limiterConfig: LimiterConfig = LimiterConfig.defaultConfig()
        private set

    var isReleased: Boolean = false
        private set

    override fun attachSession(sessionId: Int) {
        currentSessionId = sessionId
    }

    override fun detachSession(sessionId: Int) {
        if (currentSessionId == sessionId) {
            currentSessionId = 0
        }
    }

    override fun setMasterEnabled(enabled: Boolean) {
        isMasterEnabled = enabled
    }

    override fun setBands(bands: List<EqualizerBand>) {
        this.bands = bands
    }

    override fun setBandGain(bandIndex: Int, gainDb: Float) {
        if (bandIndex in bands.indices) {
            val updated = bands.toMutableList()
            updated[bandIndex] = updated[bandIndex].withGain(gainDb)
            this.bands = updated
        }
    }

    override fun setBassBoost(enabled: Boolean, strength: Int) {
        bassBoostEnabled = enabled
        bassBoostStrength = strength.coerceIn(0, 1000)
    }

    override fun setVirtualizer(enabled: Boolean, strength: Int) {
        virtualizerEnabled = enabled
        virtualizerStrength = strength.coerceIn(0, 1000)
    }

    override fun setLoudnessGain(enabled: Boolean, gainMb: Int) {
        loudnessEnabled = enabled
        loudnessGainMb = gainMb.coerceIn(0, 1200)
    }

    override fun setReverb(preset: ReverbPreset) {
        reverbPreset = preset
    }

    override fun setCompressor(config: CompressorConfig) {
        compressorConfig = config
    }

    override fun setLimiter(config: LimiterConfig) {
        limiterConfig = config
    }

    private var phase = 0.0

    override fun getVisualizerFft(bytes: ByteArray): Boolean {
        if (!isMasterEnabled) return false
        phase += 0.1
        for (i in bytes.indices) {
            val v = (sin(phase + i * 0.2) * 50 + 60).toInt().coerceIn(0, 127).toByte()
            bytes[i] = v
        }
        return true
    }

    override fun getVisualizerWaveform(bytes: ByteArray): Boolean {
        if (!isMasterEnabled) return false
        phase += 0.05
        for (i in bytes.indices) {
            val v = (sin(phase + i * 0.05) * 60 + 128).toInt().coerceIn(0, 255).toByte()
            bytes[i] = v
        }
        return true
    }

    override fun release() {
        isReleased = true
    }
}
