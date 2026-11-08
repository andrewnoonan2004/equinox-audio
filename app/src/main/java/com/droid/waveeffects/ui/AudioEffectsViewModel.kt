package com.droid.waveeffects.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.droid.waveeffects.domain.AutoEqDatabase
import com.droid.waveeffects.domain.AutoEqHeadphone
import com.droid.waveeffects.domain.AutoEqParser
import com.droid.waveeffects.domain.AudioSessionInfo
import com.droid.waveeffects.domain.AudioSessionRepository
import com.droid.waveeffects.domain.CompressorConfig
import com.droid.waveeffects.domain.EqualizerBand
import com.droid.waveeffects.domain.EqualizerPreset
import com.droid.waveeffects.domain.LimiterConfig
import com.droid.waveeffects.domain.PresetRepository
import com.droid.waveeffects.domain.ReverbPreset
import com.droid.waveeffects.dsp.DspEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.hypot

data class AudioEffectsUiState(
    val masterEnabled: Boolean = true,
    val currentPreset: EqualizerPreset = EqualizerPreset.builtInPresets[0],
    val allPresets: List<EqualizerPreset> = EqualizerPreset.builtInPresets,
    val bands: List<EqualizerBand> = EqualizerBand.createDefault10Bands(),
    val bassBoostEnabled: Boolean = false,
    val bassBoostStrength: Int = 0,
    val virtualizerEnabled: Boolean = false,
    val virtualizerStrength: Int = 0,
    val loudnessEnabled: Boolean = false,
    val loudnessGainMb: Int = 0,
    val reverbPreset: ReverbPreset = ReverbPreset.NONE,
    val compressorConfig: CompressorConfig = CompressorConfig.defaultConfig(),
    val limiterConfig: LimiterConfig = LimiterConfig.defaultConfig(),
    val activeSessions: List<AudioSessionInfo> = emptyList(),
    val selectedSession: AudioSessionInfo? = null,
    val autoEqProfiles: List<AutoEqHeadphone> = AutoEqDatabase.headphoneProfiles,
    val visualizerMagnitudes: FloatArray = FloatArray(16) { 0f },
    val visualizerWaveform: FloatArray = FloatArray(64) { 0f },
    val useDynamicColor: Boolean = true
)

class AudioEffectsViewModel(
    private val dspEngine: DspEngine,
    private val presetRepository: PresetRepository,
    private val audioSessionRepository: AudioSessionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AudioEffectsUiState(
            allPresets = presetRepository.getAllPresets(),
            currentPreset = presetRepository.getAllPresets().firstOrNull() ?: EqualizerPreset.builtInPresets[0],
            bands = EqualizerBand.createDefault10Bands()
        )
    )
    val uiState: StateFlow<AudioEffectsUiState> = _uiState.asStateFlow()

    private val fftRawBuffer = ByteArray(256)
    private val waveRawBuffer = ByteArray(256)

    init {
        // Collect active sessions
        viewModelScope.launch {
            audioSessionRepository.activeSessions.collect { sessions ->
                _uiState.update { it.copy(activeSessions = sessions) }
            }
        }

        // Collect selected session
        viewModelScope.launch {
            audioSessionRepository.selectedSession.collect { session ->
                _uiState.update { it.copy(selectedSession = session) }
                if (session != null) {
                    dspEngine.attachSession(session.sessionId)
                }
            }
        }

        // Sync initial state to DSP engine
        dspEngine.setMasterEnabled(_uiState.value.masterEnabled)
        dspEngine.setBands(_uiState.value.bands)
        dspEngine.setLimiter(_uiState.value.limiterConfig)
        dspEngine.setCompressor(_uiState.value.compressorConfig)
    }

    fun toggleMaster() {
        val nextState = !_uiState.value.masterEnabled
        _uiState.update { it.copy(masterEnabled = nextState) }
        dspEngine.setMasterEnabled(nextState)
    }

    fun toggleDynamicColor() {
        _uiState.update { it.copy(useDynamicColor = !it.useDynamicColor) }
    }

    fun selectPreset(preset: EqualizerPreset) {
        val updatedBands = _uiState.value.bands.mapIndexed { index, band ->
            val gain = preset.bandGains.getOrElse(index) { 0f }
            band.withGain(gain)
        }
        val bbEnabled = preset.bassBoostStrength > 0
        val virtEnabled = preset.virtualizerStrength > 0
        val loudEnabled = preset.loudnessDb > 0f
        val loudMb = (preset.loudnessDb * 100).toInt()

        _uiState.update {
            it.copy(
                currentPreset = preset,
                bands = updatedBands,
                bassBoostEnabled = bbEnabled,
                bassBoostStrength = preset.bassBoostStrength,
                virtualizerEnabled = virtEnabled,
                virtualizerStrength = preset.virtualizerStrength,
                loudnessEnabled = loudEnabled,
                loudnessGainMb = loudMb
            )
        }

        dspEngine.setBands(updatedBands)
        dspEngine.setBassBoost(bbEnabled, preset.bassBoostStrength)
        dspEngine.setVirtualizer(virtEnabled, preset.virtualizerStrength)
        dspEngine.setLoudnessGain(loudEnabled, loudMb)
    }

    fun setBandGain(bandIndex: Int, gainDb: Float) {
        if (bandIndex !in _uiState.value.bands.indices) return

        val currentBands = _uiState.value.bands.toMutableList()
        currentBands[bandIndex] = currentBands[bandIndex].withGain(gainDb)

        val customPreset = EqualizerPreset(
            id = "preset_custom",
            name = "Custom",
            isBuiltIn = false,
            bandGains = currentBands.map { it.gainDb },
            bassBoostStrength = _uiState.value.bassBoostStrength,
            virtualizerStrength = _uiState.value.virtualizerStrength,
            loudnessDb = _uiState.value.loudnessGainMb / 100f
        )

        _uiState.update {
            it.copy(
                bands = currentBands,
                currentPreset = customPreset
            )
        }

        dspEngine.setBandGain(bandIndex, currentBands[bandIndex].gainDb)
    }

    fun resetToFlat() {
        val flatPreset = presetRepository.getAllPresets().find { it.name == "Flat" }
            ?: EqualizerPreset.builtInPresets[0]
        selectPreset(flatPreset)
    }

    fun saveCustomPreset(name: String) {
        if (name.isBlank()) return
        val newPreset = EqualizerPreset(
            id = "custom_" + UUID.randomUUID().toString().take(8),
            name = name.trim(),
            isBuiltIn = false,
            bandGains = _uiState.value.bands.map { it.gainDb },
            bassBoostStrength = _uiState.value.bassBoostStrength,
            virtualizerStrength = _uiState.value.virtualizerStrength,
            loudnessDb = _uiState.value.loudnessGainMb / 100f
        )
        presetRepository.saveCustomPreset(newPreset)
        val all = presetRepository.getAllPresets()
        _uiState.update {
            it.copy(
                allPresets = all,
                currentPreset = newPreset
            )
        }
    }

    fun importAutoEqPreset(name: String, content: String): Boolean {
        return try {
            val preset = AutoEqParser.parse(name, content)
            presetRepository.saveCustomPreset(preset)
            val all = presetRepository.getAllPresets()
            _uiState.update {
                it.copy(
                    allPresets = all,
                    currentPreset = preset
                )
            }
            selectPreset(preset)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun applyAutoEqProfile(profile: AutoEqHeadphone) {
        val preset = profile.toPreset()
        presetRepository.saveCustomPreset(preset)
        val all = presetRepository.getAllPresets()
        _uiState.update {
            it.copy(
                allPresets = all,
                currentPreset = preset
            )
        }
        selectPreset(preset)
    }

    fun deleteCustomPreset(id: String) {
        if (presetRepository.deleteCustomPreset(id)) {
            val all = presetRepository.getAllPresets()
            val nextPreset = if (_uiState.value.currentPreset.id == id) {
                all.firstOrNull() ?: EqualizerPreset.builtInPresets[0]
            } else {
                _uiState.value.currentPreset
            }
            _uiState.update {
                it.copy(
                    allPresets = all,
                    currentPreset = nextPreset
                )
            }
        }
    }

    fun setBassBoost(enabled: Boolean, strength: Int) {
        val clamped = strength.coerceIn(0, 1000)
        _uiState.update { it.copy(bassBoostEnabled = enabled, bassBoostStrength = clamped) }
        dspEngine.setBassBoost(enabled, clamped)
    }

    fun setVirtualizer(enabled: Boolean, strength: Int) {
        val clamped = strength.coerceIn(0, 1000)
        _uiState.update { it.copy(virtualizerEnabled = enabled, virtualizerStrength = clamped) }
        dspEngine.setVirtualizer(enabled, clamped)
    }

    fun setLoudness(enabled: Boolean, gainMb: Int) {
        val clamped = gainMb.coerceIn(0, 1200)
        _uiState.update { it.copy(loudnessEnabled = enabled, loudnessGainMb = clamped) }
        dspEngine.setLoudnessGain(enabled, clamped)
    }

    fun setReverb(preset: ReverbPreset) {
        _uiState.update { it.copy(reverbPreset = preset) }
        dspEngine.setReverb(preset)
    }

    fun setCompressor(config: CompressorConfig) {
        _uiState.update { it.copy(compressorConfig = config) }
        dspEngine.setCompressor(config)
    }

    fun setLimiter(config: LimiterConfig) {
        _uiState.update { it.copy(limiterConfig = config) }
        dspEngine.setLimiter(config)
    }

    fun selectSession(sessionId: Int) {
        audioSessionRepository.selectSession(sessionId)
    }

    fun pollVisualizer() {
        if (!_uiState.value.masterEnabled) {
            _uiState.update {
                it.copy(
                    visualizerMagnitudes = FloatArray(16) { 0f },
                    visualizerWaveform = FloatArray(64) { 0f }
                )
            }
            return
        }

        // Capture FFT
        if (dspEngine.getVisualizerFft(fftRawBuffer)) {
            val bars = FloatArray(16)
            val step = (fftRawBuffer.size / 2) / 16
            for (i in 0 until 16) {
                var maxMag = 0f
                for (j in 0 until step) {
                    val idx = (i * step + j) * 2
                    if (idx + 1 < fftRawBuffer.size) {
                        val re = fftRawBuffer[idx].toFloat()
                        val im = fftRawBuffer[idx + 1].toFloat()
                        val mag = hypot(re, im) / 128f
                        if (mag > maxMag) maxMag = mag
                    }
                }
                bars[i] = maxMag.coerceIn(0.05f, 1.0f)
            }
            _uiState.update { it.copy(visualizerMagnitudes = bars) }
        }

        // Capture Waveform
        if (dspEngine.getVisualizerWaveform(waveRawBuffer)) {
            val wave = FloatArray(64)
            val step = waveRawBuffer.size / 64
            for (i in 0 until 64) {
                val byteVal = (waveRawBuffer[i * step].toInt() and 0xFF) - 128
                wave[i] = byteVal / 128f
            }
            _uiState.update { it.copy(visualizerWaveform = wave) }
        }
    }
}
