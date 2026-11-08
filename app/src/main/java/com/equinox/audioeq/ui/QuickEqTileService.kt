package com.equinox.audioeq.ui

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.equinox.audioeq.MainActivity
import com.equinox.audioeq.R
import com.equinox.audioeq.audio.AudioEngineService
import com.equinox.audioeq.data.EqStateStore

/** Quick Settings toggle for master EQ bypass. */
class QuickEqTileService : TileService() {

    override fun onCreate() {
        super.onCreate()
        EqStateStore.init(this)
    }

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        if (!AudioEngineService.isRunning.value) {
            // The engine was quit; reopening the app restarts it with the saved profile.
            val intent = Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startActivityAndCollapse(PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE))
            } else {
                @Suppress("DEPRECATION", "StartActivityAndCollapseDeprecated")
                startActivityAndCollapse(intent)
            }
            return
        }
        EqStateStore.toggleEnabled()
        updateTile()
    }

    private fun updateTile() {
        val tile = qsTile ?: return
        val state = EqStateStore.state.value
        val running = AudioEngineService.isRunning.value
        tile.state = if (running && state.isEnabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = getString(R.string.tile_label)
        tile.subtitle = when {
            !running -> getString(R.string.status_stopped)
            else -> state.selectedProfile?.displayName
                ?: getString(if (state.isEnabled) R.string.widget_status_on else R.string.widget_status_off)
        }
        tile.updateTile()
    }
}
