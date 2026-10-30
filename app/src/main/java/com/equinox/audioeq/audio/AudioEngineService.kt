package com.equinox.audioeq.audio

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.AudioPlaybackConfiguration
import android.media.audiofx.AudioEffect
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.equinox.audioeq.MainActivity
import com.equinox.audioeq.R
import com.equinox.audioeq.data.EqStateStore
import com.equinox.audioeq.model.EqState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class EngineStatus(
    val outputDevice: String = "Speaker",
    val activeStreams: Int = 0,
    val globalSessionAttached: Boolean = false,
    val attachedSessions: Set<Int> = emptySet(),
    val dumpPermissionGranted: Boolean = false,
)

class AudioEngineService : Service() {

    inner class LocalBinder : Binder() {
        val status: StateFlow<EngineStatus> get() = this@AudioEngineService.status
    }

    private val binder = LocalBinder()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val audioManager by lazy { getSystemService(AudioManager::class.java) }

    private val _status = MutableStateFlow(EngineStatus())
    val status: StateFlow<EngineStatus> = _status.asStateFlow()

    private var globalDsp: DspManager? = null
    private val sessionDsps = mutableMapOf<Int, DspManager>()
    private val dumpsysSessions = MutableStateFlow<Set<Int>>(emptySet())
    private val sessionReceiver = AudioSessionReceiver()

    private val playbackCallback = object : AudioManager.AudioPlaybackCallback() {
        override fun onPlaybackConfigChanged(configs: MutableList<AudioPlaybackConfiguration>) {
            // Covers Chrome tabs, TWAs and native players alike; per-app session IDs are not exposed here.
            _status.update { it.copy(activeStreams = configs.size) }
        }
    }

    private val deviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) = refreshOutputDevice()
        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) = refreshOutputDevice()
    }

    override fun onCreate() {
        super.onCreate()
        EqStateStore.init(this)
        _isRunning.value = true
        createNotificationChannel()
        startForeground(
            NOTIFICATION_ID,
            buildNotification(EqStateStore.state.value),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
        )

        globalDsp = DspManager(GLOBAL_SESSION).takeIf { it.isAttached } ?: run {
            Log.w(TAG, "Global session 0 effects unavailable; falling back to per-session attachment")
            null
        }
        _status.update {
            it.copy(
                globalSessionAttached = globalDsp != null,
                dumpPermissionGranted = checkSelfPermission(Manifest.permission.DUMP) == PackageManager.PERMISSION_GRANTED,
            )
        }

        audioManager.registerAudioPlaybackCallback(playbackCallback, mainHandler)
        _status.update { it.copy(activeStreams = audioManager.activePlaybackConfigurations.size) }
        audioManager.registerAudioDeviceCallback(deviceCallback, mainHandler)
        refreshOutputDevice()

        registerReceiver(
            sessionReceiver,
            IntentFilter().apply {
                addAction(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION)
                addAction(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION)
            },
            RECEIVER_EXPORTED,
        )

        scope.launch {
            EqStateStore.state.collect { state ->
                globalDsp?.applyState(state)
                sessionDsps.values.forEach { it.applyState(state) }
                notificationManager().notify(NOTIFICATION_ID, buildNotification(state))
            }
        }
        scope.launch {
            combine(SessionRegistry.sessions, dumpsysSessions) { broadcast, dumped -> broadcast.keys + dumped }
                .collect(::syncSessions)
        }
        scope.launch { pollAudioFlinger() }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_QUIT) {
            // Notification "Quit": close any open windows (which unbinds them) and stop the engine.
            MainActivity.quitApp(this)
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        _isRunning.value = false
        scope.cancel()
        runCatching { unregisterReceiver(sessionReceiver) }
        audioManager.unregisterAudioPlaybackCallback(playbackCallback)
        audioManager.unregisterAudioDeviceCallback(deviceCallback)
        globalDsp?.release()
        sessionDsps.values.forEach { it.release() }
        sessionDsps.clear()
        super.onDestroy()
    }

    /** Per-session effects are only used when the global mix could not be attached, to avoid double processing. */
    private fun syncSessions(sessions: Set<Int>) {
        val wanted = if (globalDsp != null) emptySet() else sessions
        (sessionDsps.keys - wanted).forEach { sessionDsps.remove(it)?.release() }
        (wanted - sessionDsps.keys).forEach { id ->
            val dsp = DspManager(id)
            if (dsp.isAttached) {
                dsp.applyState(EqStateStore.state.value)
                sessionDsps[id] = dsp
            } else {
                dsp.release()
            }
        }
        _status.update { it.copy(attachedSessions = sessionDsps.keys.toSet()) }
    }

    /**
     * Discovers active sessions from AudioFlinger. Requires the development permission DUMP
     * (granted via adb); SELinux may still block the exec on some builds, which is tolerated.
     */
    private suspend fun pollAudioFlinger() {
        if (checkSelfPermission(Manifest.permission.DUMP) != PackageManager.PERMISSION_GRANTED) return
        while (scope.isActive) {
            val ids = withContext(Dispatchers.IO) {
                runCatching {
                    val process = ProcessBuilder("dumpsys", "media.audio_flinger").redirectErrorStream(true).start()
                    val output = process.inputStream.bufferedReader().use { it.readText() }
                    process.waitFor()
                    SESSION_REGEX.findAll(output).mapNotNull { it.groupValues[1].toIntOrNull() }.filter { it > 0 }.toSet()
                }.onFailure { Log.w(TAG, "dumpsys media.audio_flinger failed", it) }.getOrDefault(emptySet())
            }
            dumpsysSessions.value = ids
            delay(POLL_INTERVAL_MS)
        }
    }

    private fun refreshOutputDevice() {
        val outputs = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        val device = HEADPHONE_TYPES.firstNotNullOfOrNull { type -> outputs.firstOrNull { it.type == type } }
        val name = device?.let { "${it.productName}".ifBlank { typeLabel(it.type) } } ?: "Speaker"
        _status.update { it.copy(outputDevice = name) }
        notificationManager().notify(NOTIFICATION_ID, buildNotification(EqStateStore.state.value))
    }

    private fun typeLabel(type: Int): String = when (type) {
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> "Bluetooth headphones"
        AudioDeviceInfo.TYPE_BLE_HEADSET -> "LE Audio headset"
        AudioDeviceInfo.TYPE_BLE_HEARING_AID -> "LE Audio hearing aid"
        AudioDeviceInfo.TYPE_WIRED_HEADPHONES, AudioDeviceInfo.TYPE_WIRED_HEADSET -> "Wired headphones"
        AudioDeviceInfo.TYPE_USB_HEADSET, AudioDeviceInfo.TYPE_USB_DEVICE -> "USB audio"
        else -> "Speaker"
    }

    private fun notificationManager() = getSystemService(NotificationManager::class.java)

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(R.string.notification_channel_description)
            setSound(null, null)
            enableVibration(false)
        }
        notificationManager().createNotificationChannel(channel)
    }

    private fun buildNotification(state: EqState): Notification {
        val contentIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val profile = state.selectedProfile?.displayName ?: getString(R.string.notification_no_profile)
        val quitIntent = PendingIntent.getService(
            this, 1,
            Intent(this, AudioEngineService::class.java).setAction(ACTION_QUIT),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_equinox)
            .setContentTitle(
                getString(if (state.isEnabled) R.string.notification_title_active else R.string.notification_title_bypassed),
            )
            .setContentText("$profile · ${_status.value.outputDevice}")
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            // Status-bar Live Update chip (Android 16+); ignored on older releases.
            .setRequestPromotedOngoing(Build.VERSION.SDK_INT >= 36)
            .setShortCriticalText(getString(R.string.notification_short_on))
            .addAction(R.drawable.ic_power, getString(R.string.notification_action_quit), quitIntent)
            .build()
    }

    companion object {
        private const val TAG = "AudioEngineService"
        private const val CHANNEL_ID = "audio_engine"
        private const val NOTIFICATION_ID = 1001
        private const val ACTION_QUIT = "com.equinox.audioeq.action.QUIT"
        private const val GLOBAL_SESSION = 0
        private const val POLL_INTERVAL_MS = 4_000L
        private val SESSION_REGEX = Regex("""(?:session id|Session:)\s*(\d+)""", RegexOption.IGNORE_CASE)

        /** Preferred output order when several devices are connected. */
        private val HEADPHONE_TYPES = listOf(
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
            AudioDeviceInfo.TYPE_WIRED_HEADSET,
            AudioDeviceInfo.TYPE_USB_HEADSET,
            AudioDeviceInfo.TYPE_USB_DEVICE,
            AudioDeviceInfo.TYPE_BLE_HEADSET,
            AudioDeviceInfo.TYPE_BLE_HEARING_AID,
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
        )

        private val _isRunning = MutableStateFlow(false)

        /** Whether effects are currently attached; the tile and widget reflect this. */
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

        fun start(context: Context) {
            context.startForegroundService(Intent(context, AudioEngineService::class.java))
        }

        /** Stops the engine, releasing every attached effect so audio returns to unprocessed output. */
        fun stop(context: Context) {
            context.stopService(Intent(context, AudioEngineService::class.java))
        }
    }
}
