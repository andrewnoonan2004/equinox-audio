package com.droid.waveeffects

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.droid.waveeffects.audio.AudioDemoPlayer
import com.droid.waveeffects.dsp.AndroidDspEngine
import com.droid.waveeffects.service.AudioEffectsService
import com.droid.waveeffects.theme.WaveEffectsTheme
import com.droid.waveeffects.ui.AudioEffectsViewModel
import com.droid.waveeffects.ui.main.MainScreen

class MainActivity : ComponentActivity() {

    private val demoPlayer = AudioDemoPlayer()

    private val viewModel: AudioEffectsViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val app = WaveEffectsApp.instance
                return AudioEffectsViewModel(
                    dspEngine = app.dspEngine,
                    presetRepository = app.presetRepository,
                    audioSessionRepository = app.audioSessionRepository
                ) as T
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        startServiceSafely()
        if (permissions[Manifest.permission.RECORD_AUDIO] == true) {
            (WaveEffectsApp.instance.dspEngine as? AndroidDspEngine)?.refreshVisualizer()
        }
    }

    private fun startServiceSafely() {
        runCatching {
            AudioEffectsService.start(this)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Request runtime permissions for notifications and visualizer
        val permissionsToRequest = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.RECORD_AUDIO)
        }
        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            startServiceSafely()
        }

        setContent {
            val state by viewModel.uiState.collectAsState()
            WaveEffectsTheme(dynamicColor = state.useDynamicColor) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        viewModel = viewModel,
                        demoPlayer = demoPlayer
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        startServiceSafely()
    }

    override fun onDestroy() {
        super.onDestroy()
        demoPlayer.stop()
    }
}
