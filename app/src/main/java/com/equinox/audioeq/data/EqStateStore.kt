package com.equinox.audioeq.data

import android.content.Context
import android.content.SharedPreferences
import com.equinox.audioeq.data.autoeq.AutoEqRepository
import com.equinox.audioeq.model.BandSetting
import com.equinox.audioeq.model.EqState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Process-wide EQ state shared by the activity, audio service, Quick Settings tile and widget.
 * Persisted to SharedPreferences so the tile and widget work before the UI is ever opened.
 */
object EqStateStore {
    private val _state = MutableStateFlow(EqState())
    val state: StateFlow<EqState> = _state.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var prefs: SharedPreferences
    lateinit var repository: AutoEqRepository
        private set

    fun init(context: Context) {
        if (::prefs.isInitialized) return
        prefs = context.applicationContext.getSharedPreferences("eq_state", Context.MODE_PRIVATE)
        repository = AutoEqRepository(context)
        _state.value = readPrefs()
        scope.launch { restoreProfile() }
    }

    fun update(transform: (EqState) -> EqState) {
        _state.update(transform)
        writePrefs(_state.value)
    }

    fun toggleEnabled() = update { it.copy(isEnabled = !it.isEnabled) }

    /** Imported profiles have no database entry, so their source text is kept for restore. */
    fun saveImportedProfileText(text: String?) {
        prefs.edit().apply { if (text == null) remove(KEY_IMPORTED_TEXT) else putString(KEY_IMPORTED_TEXT, text) }.apply()
    }

    private suspend fun restoreProfile() {
        val id = prefs.getString(KEY_PROFILE_ID, null) ?: return
        val profile = if (id.startsWith(AutoEqRepository.IMPORTED_ID_PREFIX)) {
            prefs.getString(KEY_IMPORTED_TEXT, null)?.let {
                AutoEqRepository.parseProfileText(id.removePrefix(AutoEqRepository.IMPORTED_ID_PREFIX), it)
            }
        } else {
            repository.findById(id)
        }
        if (profile != null) {
            _state.update { if (it.selectedProfile == null) it.copy(selectedProfile = profile) else it }
        }
    }

    private fun readPrefs(): EqState {
        val d = EqState()
        return EqState(
            isEnabled = prefs.getBoolean("isEnabled", d.isEnabled),
            customPreampDb = prefs.getFloat("customPreampDb", d.customPreampDb),
            graphicBands = d.graphicBands.map { BandSetting(it.frequencyHz, prefs.getFloat("band_${it.frequencyHz}", 0f)) },
            bassBoostEnabled = prefs.getBoolean("bassBoostEnabled", d.bassBoostEnabled),
            bassBoostStrength = prefs.getFloat("bassBoostStrength", d.bassBoostStrength),
            bassCutoffFreqHz = prefs.getFloat("bassCutoffFreqHz", d.bassCutoffFreqHz),
            trebleBoostEnabled = prefs.getBoolean("trebleBoostEnabled", d.trebleBoostEnabled),
            trebleBoostStrength = prefs.getFloat("trebleBoostStrength", d.trebleBoostStrength),
            trebleShelfFreqHz = prefs.getFloat("trebleShelfFreqHz", d.trebleShelfFreqHz),
            vocalClarityEnabled = prefs.getBoolean("vocalClarityEnabled", d.vocalClarityEnabled),
            vocalClarityStrength = prefs.getFloat("vocalClarityStrength", d.vocalClarityStrength),
            loudnessEqualizerEnabled = prefs.getBoolean("loudnessEqualizerEnabled", d.loudnessEqualizerEnabled),
            loudnessGainDb = prefs.getFloat("loudnessGainDb", d.loudnessGainDb),
            virtualizerEnabled = prefs.getBoolean("virtualizerEnabled", d.virtualizerEnabled),
            virtualizerStrength = prefs.getFloat("virtualizerStrength", d.virtualizerStrength),
            channelBalance = prefs.getFloat("channelBalance", d.channelBalance),
            limiterEnabled = prefs.getBoolean("limiterEnabled", d.limiterEnabled),
        )
    }

    private fun writePrefs(s: EqState) {
        prefs.edit().apply {
            putBoolean("isEnabled", s.isEnabled)
            putString(KEY_PROFILE_ID, s.selectedProfile?.id)
            putFloat("customPreampDb", s.customPreampDb)
            s.graphicBands.forEach { putFloat("band_${it.frequencyHz}", it.gainDb) }
            putBoolean("bassBoostEnabled", s.bassBoostEnabled)
            putFloat("bassBoostStrength", s.bassBoostStrength)
            putFloat("bassCutoffFreqHz", s.bassCutoffFreqHz)
            putBoolean("trebleBoostEnabled", s.trebleBoostEnabled)
            putFloat("trebleBoostStrength", s.trebleBoostStrength)
            putFloat("trebleShelfFreqHz", s.trebleShelfFreqHz)
            putBoolean("vocalClarityEnabled", s.vocalClarityEnabled)
            putFloat("vocalClarityStrength", s.vocalClarityStrength)
            putBoolean("loudnessEqualizerEnabled", s.loudnessEqualizerEnabled)
            putFloat("loudnessGainDb", s.loudnessGainDb)
            putBoolean("virtualizerEnabled", s.virtualizerEnabled)
            putFloat("virtualizerStrength", s.virtualizerStrength)
            putFloat("channelBalance", s.channelBalance)
            putBoolean("limiterEnabled", s.limiterEnabled)
        }.apply()
    }

    private const val KEY_PROFILE_ID = "selectedProfileId"
    private const val KEY_IMPORTED_TEXT = "importedProfileText"
}
