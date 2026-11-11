package com.pocketwidget.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MonthWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        refresh(context, appWidgetIds)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: android.os.Bundle,
    ) {
        refresh(context, intArrayOf(appWidgetId))
    }

    /** Month labels and ranges depend on the clock, date and timezone. */
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            -> {
                val manager = AppWidgetManager.getInstance(context)
                refresh(
                    context,
                    manager.getAppWidgetIds(ComponentName(context, MonthWidgetProvider::class.java)),
                )
            }
            else -> super.onReceive(context, intent)
        }
    }

    private fun refresh(context: Context, ids: IntArray) {
        if (ids.isEmpty()) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                WidgetRefresh.updateMonth(context, ids)
            } finally {
                pending.finish()
            }
        }
    }
}

class QuickAddWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        if (appWidgetIds.isEmpty()) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                WidgetRefresh.updateQuickAdd(context, appWidgetIds)
            } finally {
                pending.finish()
            }
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        val app = context.applicationContext as? com.pocketwidget.PocketApp ?: return
        appWidgetIds.forEach { app.container.settingsStore.clearQuickCategoryIds(it) }
    }

    override fun onRestored(context: Context, oldWidgetIds: IntArray, newWidgetIds: IntArray) {
        val app = context.applicationContext as? com.pocketwidget.PocketApp ?: return
        oldWidgetIds.zip(newWidgetIds).forEach { (oldId, newId) ->
            app.container.settingsStore.setQuickCategoryIds(newId, app.container.settingsStore.quickCategoryIds(oldId))
        }
    }
}
