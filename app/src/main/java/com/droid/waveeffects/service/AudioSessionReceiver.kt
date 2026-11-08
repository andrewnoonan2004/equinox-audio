package com.droid.waveeffects.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.audiofx.AudioEffect
import android.util.Log
import com.droid.waveeffects.WaveEffectsApp
import com.droid.waveeffects.domain.AudioSessionInfo

class AudioSessionReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "AudioSessionReceiver"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return
        val action = intent.action ?: return

        val sessionId = intent.getIntExtra(AudioEffect.EXTRA_AUDIO_SESSION, -1)
        val packageName = intent.getStringExtra(AudioEffect.EXTRA_PACKAGE_NAME) ?: "unknown"

        if (sessionId <= 0) return

        val pm = context.packageManager
        val appName = try {
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (_: Exception) {
            packageName.substringAfterLast('.')
        }

        val app = context.applicationContext as? WaveEffectsApp ?: WaveEffectsApp.instance

        when (action) {
            AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION -> {
                Log.d(TAG, "Audio session opened: $sessionId for $appName ($packageName)")
                val sessionInfo = AudioSessionInfo(
                    sessionId = sessionId,
                    packageName = packageName,
                    appName = appName
                )
                app.audioSessionRepository.addSession(sessionInfo)
                app.audioSessionRepository.selectSession(sessionId)

                // Start service to ensure audio processing stays alive
                AudioEffectsService.start(context)
            }
            AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION -> {
                Log.d(TAG, "Audio session closed: $sessionId")
                app.audioSessionRepository.removeSession(sessionId)
            }
        }
    }
}
