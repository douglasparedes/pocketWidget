package com.pocketwidget.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.pocketwidget.PocketApp
import com.pocketwidget.widget.WidgetRefresh
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Debug builds only. Lets automated runs and designers set up a realistic month:
 *
 * adb shell am broadcast -a com.pocketwidget.debug.SETUP -p com.pocketwidget \
 *   --ez seed true --ei limit 150000 --es currency EUR
 */
class DebugSetupReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as? PocketApp ?: return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (intent.getBooleanExtra("clear", false)) {
                    app.container.repository.clearAllData()
                }
                if (intent.hasExtra("limit")) {
                    app.container.settingsStore.setMonthlyLimit(intent.getIntExtra("limit", 0).toLong())
                }
                intent.getStringExtra("currency")?.let { app.container.settingsStore.setCurrency(it) }
                if (intent.getBooleanExtra("seed", false)) {
                    app.container.repository.seedDemoData()
                }
                WidgetRefresh.updateAll(context)
            } finally {
                pending.finish()
            }
        }
    }
}
