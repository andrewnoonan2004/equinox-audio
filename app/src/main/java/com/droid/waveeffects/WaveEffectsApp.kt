package com.droid.waveeffects

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import com.droid.waveeffects.domain.AudioSessionRepository
import com.droid.waveeffects.domain.PresetRepository
import com.droid.waveeffects.dsp.AndroidDspEngine
import com.droid.waveeffects.dsp.DspEngine
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

class WaveEffectsApp : Application() {

    companion object {
        private const val TAG = "WaveEffectsApp"
        const val CHANNEL_ID = "wave_effects_service_channel"
        const val NOTIFICATION_ID = 1001

        lateinit var instance: WaveEffectsApp
            private set
    }

    lateinit var presetRepository: PresetRepository
        private set

    lateinit var audioSessionRepository: AudioSessionRepository
        private set

    lateinit var dspEngine: DspEngine
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        setupCrashHandler()

        runCatching {
            val presetsFile = File(filesDir, "custom_presets.json")
            presetRepository = PresetRepository(presetsFile)
            audioSessionRepository = AudioSessionRepository()
            dspEngine = AndroidDspEngine(this)
            createNotificationChannel()
        }.onFailure { e ->
            Log.e(TAG, "Initialization failed: ${e.message}", e)
            logCrash(e, "App.onCreate")
        }
    }

    private fun setupCrashHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e(TAG, "FATAL CRASH in thread ${thread.name}: ${throwable.message}", throwable)
            logCrash(throwable, "UncaughtException: ${thread.name}")
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun logCrash(throwable: Throwable, contextInfo: String) {
        val sw = StringWriter()
        throwable.printStackTrace(PrintWriter(sw))
        val stackTrace = sw.toString()
        val logContent = "=== WaveEffects Crash ($contextInfo) ===\n" +
                "Timestamp: ${System.currentTimeMillis()}\n" +
                "Device: ${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE}, API ${Build.VERSION.SDK_INT})\n" +
                "Error: ${throwable.javaClass.name}: ${throwable.message}\n" +
                stackTrace + "\n=====================================\n"

        // Write to internal filesDir
        runCatching {
            File(filesDir, "crash.log").appendText(logContent)
        }
        // Write to project dir in shared storage if accessible
        runCatching {
            File("/storage/emulated/10/Documents/Antigravity/.gemini/antigravity/scratch/WaveEffects/crash.log").appendText(logContent)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "WaveEffects DSP Engine",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps the audio equalizer and DSP engine running in the background"
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }
}
