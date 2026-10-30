package com.equinox.audioeq

import android.Manifest
import android.app.PictureInPictureParams
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.OpenableColumns
import android.util.Rational
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.equinox.audioeq.audio.AudioEngineService
import com.equinox.audioeq.audio.EngineStatus
import com.equinox.audioeq.data.EqStateStore
import com.equinox.audioeq.data.autoeq.AutoEqRepository
import com.equinox.audioeq.model.AutoEqProfile
import com.equinox.audioeq.ui.screens.HomeScreen
import com.equinox.audioeq.ui.screens.StudioLayout
import com.equinox.audioeq.ui.theme.EquinoxTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private val engineStatus = MutableStateFlow(EngineStatus())
    private var statusJob: Job? = null
    private var isInPip by mutableStateOf(false)

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val binder = service as AudioEngineService.LocalBinder
            statusJob = lifecycleScope.launch { binder.status.collect { engineStatus.value = it } }
        }

        override fun onServiceDisconnected(name: ComponentName) {
            statusJob?.cancel()
        }
    }

    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    private val importProfile = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(::importProfileFrom)
    }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        openWindows += this
        isInPip = isInPictureInPictureMode

        AudioEngineService.start(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            EquinoxTheme {
                val windowSizeClass = calculateWindowSizeClass(this)
                val state by EqStateStore.state.collectAsStateWithLifecycle()
                val status by engineStatus.collectAsStateWithLifecycle()
                var query by rememberSaveable { mutableStateOf("") }
                var profiles by remember { mutableStateOf(emptyList<AutoEqProfile>()) }
                LaunchedEffect(query) { profiles = EqStateStore.repository.search(query) }

                HomeScreen(
                    state = state,
                    engineStatus = status,
                    profiles = profiles,
                    query = query,
                    onQueryChange = { query = it },
                    layout = when {
                        windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact -> StudioLayout.Drawer
                        windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded &&
                            windowSizeClass.heightSizeClass != WindowHeightSizeClass.Compact -> StudioLayout.DualPane
                        else -> StudioLayout.Rail
                    },
                    isInPictureInPicture = isInPip,
                    onStateChange = EqStateStore::update,
                    onSelectProfile = { profile ->
                        if (profile?.id?.startsWith(AutoEqRepository.IMPORTED_ID_PREFIX) != true) {
                            EqStateStore.saveImportedProfileText(null)
                        }
                        EqStateStore.update { it.copy(selectedProfile = profile) }
                    },
                    onImportProfile = { importProfile.launch(arrayOf("text/plain", "application/octet-stream")) },
                    onEnterPictureInPicture = ::enterDesktopPip,
                    onQuit = { quitApp(this) },
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        bindService(Intent(this, AudioEngineService::class.java), connection, Context.BIND_AUTO_CREATE)
    }

    override fun onStop() {
        statusJob?.cancel()
        unbindService(connection)
        super.onStop()
    }

    /** Closing a window leaves the engine running in the background; only Quit stops it. */
    override fun onDestroy() {
        openWindows -= this
        super.onDestroy()
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isInPip = isInPictureInPictureMode
    }

    /** On Android 17 desktop windowing this opens an interactive, pinned PiP window. */
    private fun enterDesktopPip() {
        runCatching {
            enterPictureInPictureMode(PictureInPictureParams.Builder().setAspectRatio(Rational(16, 10)).build())
        }.onFailure { Toast.makeText(this, "Picture-in-picture is unavailable", Toast.LENGTH_SHORT).show() }
    }

    companion object {
        /** Live windows; the app supports multiple instances on desktop. */
        private val openWindows = mutableSetOf<MainActivity>()

        /**
         * Closes every Equinox window and stops the engine, releasing all effects.
         * The saved profile and settings are kept for the next launch.
         */
        fun quitApp(context: Context) {
            openWindows.toList().forEach { it.finishAndRemoveTask() }
            AudioEngineService.stop(context)
        }
    }

    private fun importProfileFrom(uri: Uri) {
        lifecycleScope.launch {
            val (name, text) = withContext(Dispatchers.IO) {
                val name = contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                    if (c.moveToFirst()) c.getString(0) else null
                } ?: "Imported profile"
                name to runCatching { contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } }.getOrNull()
            }
            val profile = text?.let { AutoEqRepository.parseProfileText(name, it) }
            if (profile == null) {
                Toast.makeText(this@MainActivity, "No Preamp/GraphicEQ/Filter lines found", Toast.LENGTH_LONG).show()
                return@launch
            }
            EqStateStore.saveImportedProfileText(text)
            EqStateStore.update { it.copy(selectedProfile = profile) }
            Toast.makeText(this@MainActivity, "Loaded ${profile.displayName}", Toast.LENGTH_SHORT).show()
        }
    }
}
