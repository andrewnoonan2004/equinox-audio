package com.equinox.audioeq.ui

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.os.Bundle
import android.view.Display
import android.view.View
import android.widget.RemoteViews
import com.equinox.audioeq.MainActivity
import com.equinox.audioeq.R
import com.equinox.audioeq.audio.AudioEngineService
import com.equinox.audioeq.data.EqStateStore

/**
 * Desktop homescreen widget. Uses only text and the vector app icon (no bitmaps), which keeps
 * every RemoteViews update far below the platform's bitmap memory cap.
 */
class EquinoxDesktopWidgetReceiver : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        EqStateStore.init(context)
        if (intent.action == ACTION_TOGGLE) {
            // The app-level observer in EquinoxApp refreshes all widgets after the change.
            EqStateStore.toggleEnabled()
            return
        }
        super.onReceive(context, intent)
    }

    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { update(context, manager, it) }
    }

    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle) {
        update(context, manager, appWidgetId)
    }

    companion object {
        private const val ACTION_TOGGLE = "com.equinox.audioeq.action.TOGGLE_EQ"
        private const val COMPACT_HEIGHT_DP = 90

        fun refreshAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            manager.getAppWidgetIds(ComponentName(context, EquinoxDesktopWidgetReceiver::class.java))
                .forEach { update(context, manager, it) }
        }

        private fun update(context: Context, manager: AppWidgetManager, widgetId: Int) {
            val options = manager.getAppWidgetOptions(widgetId)
            // Resolve resources against the display hosting the widget (external monitors may differ in density).
            val displayId = options.getInt(AppWidgetManager.OPTION_APPWIDGET_DISPLAY_ID, Display.DEFAULT_DISPLAY)
            val display = context.getSystemService(DisplayManager::class.java).getDisplay(displayId)
            val displayContext = display?.let { context.createDisplayContext(it) } ?: context
            val compact = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 110) < COMPACT_HEIGHT_DP

            val state = EqStateStore.state.value
            val running = AudioEngineService.isRunning.value
            val buttonIntent = if (running) {
                PendingIntent.getBroadcast(
                    context, 0,
                    Intent(context, EquinoxDesktopWidgetReceiver::class.java).setAction(ACTION_TOGGLE),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                )
            } else {
                // Opening the app restarts the engine with the saved profile.
                PendingIntent.getActivity(
                    context, 1,
                    Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    PendingIntent.FLAG_IMMUTABLE,
                )
            }
            val statusText = when {
                !running -> R.string.status_stopped
                state.isEnabled -> R.string.widget_status_on
                else -> R.string.widget_status_off
            }
            val views = RemoteViews(context.packageName, R.layout.widget_equinox).apply {
                setTextViewText(
                    R.id.widget_profile,
                    state.selectedProfile?.displayName ?: displayContext.getString(R.string.notification_no_profile),
                )
                setTextViewText(R.id.widget_status, displayContext.getString(statusText))
                setTextViewText(
                    R.id.widget_toggle,
                    displayContext.getString(if (running) R.string.widget_toggle else R.string.widget_open),
                )
                setViewVisibility(R.id.widget_title, if (compact) View.GONE else View.VISIBLE)
                setOnClickPendingIntent(R.id.widget_toggle, buttonIntent)
            }
            manager.updateAppWidget(widgetId, views)
        }
    }
}
