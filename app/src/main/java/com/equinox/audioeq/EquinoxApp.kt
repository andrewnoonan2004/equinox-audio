package com.equinox.audioeq

import android.app.Application
import android.content.ComponentName
import android.service.quicksettings.TileService
import com.equinox.audioeq.audio.AudioEngineService
import com.equinox.audioeq.data.EqStateStore
import com.equinox.audioeq.ui.EquinoxDesktopWidgetReceiver
import com.equinox.audioeq.ui.QuickEqTileService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class EquinoxApp : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        EqStateStore.init(this)

        // Keep the tile and widget in sync with changes made anywhere in the app.
        appScope.launch {
            combine(EqStateStore.state, AudioEngineService.isRunning) { state, running ->
                Triple(state.isEnabled, state.selectedProfile?.displayName, running)
            }
                .distinctUntilChanged()
                .drop(1)
                .collect {
                    EquinoxDesktopWidgetReceiver.refreshAll(this@EquinoxApp)
                    TileService.requestListeningState(
                        this@EquinoxApp,
                        ComponentName(this@EquinoxApp, QuickEqTileService::class.java),
                    )
                }
        }
    }
}
