package com.droid.waveeffects.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.droid.waveeffects.MainActivity
import com.droid.waveeffects.R
import com.droid.waveeffects.WaveEffectsApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class AudioEffectsService : Service() {

    companion object {
        private const val TAG = "AudioEffectsService"
        const val ACTION_START = "com.droid.waveeffects.ACTION_START"
        const val ACTION_STOP = "com.droid.waveeffects.ACTION_STOP"
        const val ACTION_TOGGLE_MASTER = "com.droid.waveeffects.ACTION_TOGGLE_MASTER"

        fun start(context: Context) {
            runCatching {
                val intent = Intent(context, AudioEffectsService::class.java).apply {
                    action = ACTION_START
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            }.onFailure { e ->
                Log.w(TAG, "Failed to start AudioEffectsService: ${e.message}")
            }
        }

        fun stop(context: Context) {
            runCatching {
                val intent = Intent(context, AudioEffectsService::class.java).apply {
                    action = ACTION_STOP
                }
                context.startService(intent)
            }.onFailure { e ->
                Log.w(TAG, "Failed to stop AudioEffectsService: ${e.message}")
            }
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startInForeground()

        val app = application as? WaveEffectsApp ?: WaveEffectsApp.instance

        // Listen for session changes to update notification
        serviceScope.launch {
            app.audioSessionRepository.selectedSession.collect { session ->
                updateNotification(session?.appName ?: "Global Output", app.dspEngine.isMasterEnabled)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val app = application as? WaveEffectsApp ?: WaveEffectsApp.instance

        when (intent?.action) {
            ACTION_STOP -> {
                runCatching {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
                return START_NOT_STICKY
            }
            ACTION_TOGGLE_MASTER -> {
                runCatching {
                    val nextState = !app.dspEngine.isMasterEnabled
                    app.dspEngine.setMasterEnabled(nextState)
                    val currentSession = app.audioSessionRepository.selectedSession.value?.appName ?: "Global Output"
                    updateNotification(currentSession, nextState)
                }
            }
            else -> {
                runCatching {
                    val currentSession = app.audioSessionRepository.selectedSession.value?.appName ?: "Global Output"
                    updateNotification(currentSession, app.dspEngine.isMasterEnabled)
                }
            }
        }

        return START_STICKY
    }

    private fun startInForeground() {
        runCatching {
            val notification = buildNotification("Global Output", true)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                runCatching {
                    startForeground(
                        WaveEffectsApp.NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                    )
                }.onFailure { fgsEx ->
                    Log.w(TAG, "startForeground with mediaPlayback failed, falling back: ${fgsEx.message}")
                    startForeground(WaveEffectsApp.NOTIFICATION_ID, notification)
                }
            } else {
                startForeground(WaveEffectsApp.NOTIFICATION_ID, notification)
            }
        }.onFailure { e ->
            Log.e(TAG, "startInForeground failed: ${e.message}", e)
        }
    }

    private fun updateNotification(sessionName: String, isEnabled: Boolean) {
        runCatching {
            val notification = buildNotification(sessionName, isEnabled)
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.notify(WaveEffectsApp.NOTIFICATION_ID, notification)
        }.onFailure { e ->
            Log.w(TAG, "updateNotification failed: ${e.message}")
        }
    }

    private fun buildNotification(sessionName: String, isEnabled: Boolean): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java)
        val openPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val toggleIntent = Intent(this, AudioEffectsService::class.java).apply {
            action = ACTION_TOGGLE_MASTER
        }
        val togglePendingIntent = PendingIntent.getService(
            this,
            1,
            toggleIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val statusText = if (isEnabled) "DSP Active • $sessionName" else "DSP Bypassed • $sessionName"
        val toggleActionTitle = if (isEnabled) "Bypass" else "Enable"

        return NotificationCompat.Builder(this, WaveEffectsApp.CHANNEL_ID)
            .setContentTitle("WaveEffects DSP Engine")
            .setContentText(statusText)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(openPendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(R.drawable.ic_notification, toggleActionTitle, togglePendingIntent)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
