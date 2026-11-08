package com.droid.waveeffects.dsp

import com.droid.waveeffects.domain.CompressorConfig
import com.droid.waveeffects.domain.EqualizerBand
import com.droid.waveeffects.domain.LimiterConfig
import com.droid.waveeffects.domain.ReverbPreset

interface DspEngine {
    val currentSessionId: Int
    val isMasterEnabled: Boolean

    fun attachSession(sessionId: Int)
    fun detachSession(sessionId: Int)
    fun setMasterEnabled(enabled: Boolean)

    fun setBands(bands: List<EqualizerBand>)
    fun setBandGain(bandIndex: Int, gainDb: Float)

    fun setBassBoost(enabled: Boolean, strength: Int)
    fun setVirtualizer(enabled: Boolean, strength: Int)
    fun setLoudnessGain(enabled: Boolean, gainMb: Int)
    fun setReverb(preset: ReverbPreset)

    fun setCompressor(config: CompressorConfig)
    fun setLimiter(config: LimiterConfig)

    fun getVisualizerFft(bytes: ByteArray): Boolean
    fun getVisualizerWaveform(bytes: ByteArray): Boolean

    fun release()
}
