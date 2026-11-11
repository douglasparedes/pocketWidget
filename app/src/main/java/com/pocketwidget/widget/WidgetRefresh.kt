package com.pocketwidget.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import com.pocketwidget.PocketApp
import kotlinx.coroutines.flow.first
import java.time.YearMonth

object WidgetRefresh {

    suspend fun updateAll(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val monthIds = manager.getAppWidgetIds(ComponentName(context, MonthWidgetProvider::class.java))
        if (monthIds.isNotEmpty()) updateMonth(context, monthIds)
        val quickIds = manager.getAppWidgetIds(ComponentName(context, QuickAddWidgetProvider::class.java))
        if (quickIds.isNotEmpty()) updateQuickAdd(context, quickIds)
    }

    suspend fun updateMonth(context: Context, ids: IntArray) {
        val app = context.applicationContext as? PocketApp ?: return
        val settings = app.container.settingsStore.settings.value
        val snapshot = app.container.repository.monthlySnapshot(YearMonth.now(app.container.repository.zone()))
        val manager = AppWidgetManager.getInstance(context)
        ids.forEach { id ->
            val widthDp = manager.getAppWidgetOptions(id)
                .getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0)
                .takeIf { it > 0 }
                ?: 300
            manager.updateAppWidget(
                id,
                WidgetRenderer.monthRemoteViews(context, snapshot, settings, widthDp),
            )
        }
    }

    suspend fun updateQuickAdd(context: Context, ids: IntArray) {
        val app = context.applicationContext as? PocketApp ?: return
        val settings = app.container.settingsStore.settings.value
        val categories = app.container.repository.categories.first().filterNot { it.archived }
        val manager = AppWidgetManager.getInstance(context)
        ids.forEach { id ->
            val chosen = app.container.settingsStore.quickCategoryIds(id)
                .mapNotNull { categoryId -> categories.firstOrNull { it.id == categoryId } }
                .ifEmpty { categories.take(4) }
            manager.updateAppWidget(id, WidgetRenderer.quickAddRemoteViews(context, chosen, settings, id))
        }
    }
}
