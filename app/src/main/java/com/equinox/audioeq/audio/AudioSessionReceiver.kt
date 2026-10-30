package com.equinox.audioeq.audio

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.audiofx.AudioEffect
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Audio sessions announced by media players through the standard audio-effect broadcasts. */
object SessionRegistry {
    private val _sessions = MutableStateFlow<Map<Int, String>>(emptyMap())
    val sessions: StateFlow<Map<Int, String>> = _sessions.asStateFlow()

    fun open(sessionId: Int, packageName: String) = _sessions.update { it + (sessionId to packageName) }
    fun close(sessionId: Int) = _sessions.update { it - sessionId }
}

/**
 * Declared in the manifest and also registered at runtime by [AudioEngineService], because these
 * broadcasts are implicit and not reliably delivered to manifest receivers on modern Android.
 */
class AudioSessionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val sessionId = intent.getIntExtra(AudioEffect.EXTRA_AUDIO_SESSION, AudioManager.ERROR)
        if (sessionId <= 0) return
        when (intent.action) {
            AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION ->
                SessionRegistry.open(sessionId, intent.getStringExtra(AudioEffect.EXTRA_PACKAGE_NAME).orEmpty())
            AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION -> SessionRegistry.close(sessionId)
        }
    }
}
