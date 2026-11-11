package com.pocketwidget.widget

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.net.Uri
import android.view.View
import android.widget.RemoteViews
import com.pocketwidget.R
import com.pocketwidget.data.BudgetStatus
import com.pocketwidget.data.Category
import com.pocketwidget.data.Money
import com.pocketwidget.data.Settings
import com.pocketwidget.data.WidgetSnapshot
import com.pocketwidget.data.WidgetTheme
import com.pocketwidget.ui.MainActivity
import java.time.format.DateTimeFormatter
import java.util.Locale

object WidgetRenderer {

    fun isDark(context: Context, theme: WidgetTheme): Boolean = when (theme) {
        WidgetTheme.LIGHT -> false
        WidgetTheme.DARK -> true
        WidgetTheme.SYSTEM -> (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
    }

    fun monthRemoteViews(
        context: Context,
        snapshot: WidgetSnapshot,
        settings: Settings,
        widthDp: Int = 300,
    ): RemoteViews {
        val dark = isDark(context, settings.widgetTheme)
        val views = RemoteViews(context.packageName, R.layout.widget_month)

        val text = if (dark) 0xFFF2EDE4.toInt() else 0xFF1B1A17.toInt()
        val muted = if (dark) 0xFFB4AB9C.toInt() else 0xFF6B6459.toInt()

        views.setInt(R.id.widget_root, "setBackgroundResource", background(dark))
        views.setTextViewText(
            R.id.widget_month,
            snapshot.month.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()))
                .uppercase(Locale.getDefault()),
        )
        views.setTextColor(R.id.widget_month, muted)

        val showAmounts = settings.showWidgetAmounts
        views.setTextViewText(
            R.id.widget_amount,
            if (showAmounts) {
                Money.format(snapshot.totalCents, snapshot.currencyCode, showDecimalsWhenWhole = false)
            } else {
                "••••"
            },
        )
        views.setTextColor(R.id.widget_amount, text)

        val caption = when {
            snapshot.limitCents <= 0L -> "No monthly limit"
            showAmounts -> "of " + Money.format(snapshot.limitCents, snapshot.currencyCode, showDecimalsWhenWhole = false)
            else -> "of your limit"
        }
        views.setTextViewText(R.id.widget_caption, caption)
        views.setTextColor(R.id.widget_caption, muted)

        val statusLabel = when (snapshot.status) {
            BudgetStatus.NO_LIMIT -> ""
            BudgetStatus.ON_TRACK -> "ON TRACK"
            BudgetStatus.CLOSE_TO_LIMIT -> "CLOSE"
            BudgetStatus.OVER -> "OVER"
        }
        views.setTextViewText(R.id.widget_status, statusLabel)
        views.setTextColor(R.id.widget_status, statusColor(snapshot.status, dark))
        views.setViewVisibility(R.id.widget_status, if (statusLabel.isEmpty()) View.GONE else View.VISIBLE)

        if (settings.showWidgetMeter && snapshot.limitCents > 0L) {
            val widthPx = dpToPx(context, (widthDp - 36).coerceAtLeast(60))
            val heightPx = dpToPx(context, 8)
            views.setImageViewBitmap(
                R.id.widget_meter,
                meterBitmap(widthPx, heightPx, snapshot.progress, meterColor(snapshot, settings, dark), dark),
            )
            views.setViewVisibility(R.id.widget_meter, View.VISIBLE)
        } else {
            views.setViewVisibility(R.id.widget_meter, View.GONE)
        }

        val top = snapshot.topCategory
        if (top == null) {
            views.setViewVisibility(R.id.widget_footer, View.GONE)
        } else {
            views.setViewVisibility(R.id.widget_footer, View.VISIBLE)
            views.setTextViewText(R.id.widget_top_emoji, top.category.emoji)
            views.setTextViewText(R.id.widget_top_name, top.category.name)
            views.setTextColor(R.id.widget_top_name, muted)
            views.setTextViewText(
                R.id.widget_top_amount,
                if (showAmounts) {
                    Money.format(top.cents, snapshot.currencyCode, showDecimalsWhenWhole = false)
                } else {
                    "••"
                },
            )
            views.setTextColor(R.id.widget_top_amount, text)
            views.setTextViewText(
                R.id.widget_more,
                if (snapshot.otherCategoryCount > 0) "+${snapshot.otherCategoryCount}" else "",
            )
        }

        views.setOnClickPendingIntent(R.id.widget_root, openApp(context, Uri.parse("pocketwidget://home"), requestCode = 1001))
        return views
    }

    fun quickAddRemoteViews(
        context: Context,
        categories: List<Category>,
        settings: Settings,
        appWidgetId: Int,
    ): RemoteViews {
        val dark = isDark(context, settings.widgetTheme)
        val views = RemoteViews(context.packageName, R.layout.widget_quick_add)
        views.setInt(R.id.quick_root, "setBackgroundResource", background(dark))
        views.removeAllViews(R.id.quick_row)

        val muted = if (dark) 0xFFB4AB9C.toInt() else 0xFF6B6459.toInt()
        val text = if (dark) 0xFFF2EDE4.toInt() else 0xFF1B1A17.toInt()

        categories.take(4).forEachIndexed { index, category ->
            val item = RemoteViews(context.packageName, R.layout.widget_quick_add_item)
            item.setInt(R.id.quick_item_root, "setBackgroundResource", itemBackground(dark))
            item.setTextViewText(R.id.quick_item_emoji, category.emoji)
            item.setTextViewText(R.id.quick_item_name, category.name)
            item.setTextColor(R.id.quick_item_name, muted)
            item.setOnClickPendingIntent(
                R.id.quick_item_root,
                openApp(
                    context,
                    Uri.parse("pocketwidget://add?category=${Uri.encode(category.id)}"),
                    requestCode = (appWidgetId * 100 + index) and 0x7FFFFFF,
                ),
            )
            views.addView(R.id.quick_row, item)
        }

        val plus = RemoteViews(context.packageName, R.layout.widget_quick_add_item)
        plus.setInt(R.id.quick_item_root, "setBackgroundResource", itemBackground(dark))
        plus.setTextViewText(R.id.quick_item_emoji, "＋")
        plus.setTextViewText(R.id.quick_item_name, "Log")
        plus.setTextColor(R.id.quick_item_name, text)
        plus.setOnClickPendingIntent(
            R.id.quick_item_root,
            openApp(context, Uri.parse("pocketwidget://add"), requestCode = (appWidgetId * 100 + 9) and 0x7FFFFFF),
        )
        views.addView(R.id.quick_row, plus)

        views.setOnClickPendingIntent(
            R.id.quick_root,
            openApp(context, Uri.parse("pocketwidget://home"), requestCode = (appWidgetId * 100 + 1) and 0x7FFFFFF),
        )
        return views
    }

    private fun meterColor(snapshot: WidgetSnapshot, settings: Settings, dark: Boolean): Int =
        when (snapshot.status) {
            BudgetStatus.OVER -> statusColor(BudgetStatus.OVER, dark)
            BudgetStatus.CLOSE_TO_LIMIT -> statusColor(BudgetStatus.CLOSE_TO_LIMIT, dark)
            // The accent is a real user choice: it tints the gauge while the budget is healthy.
            else -> settings.widgetAccent.argb.toInt()
        }

    fun statusColor(status: BudgetStatus, dark: Boolean): Int = when (status) {
        BudgetStatus.NO_LIMIT -> if (dark) 0xFFB4AB9C.toInt() else 0xFF6B6459.toInt()
        BudgetStatus.ON_TRACK -> if (dark) 0xFF9FD3B4.toInt() else 0xFF2F6F4E.toInt()
        BudgetStatus.CLOSE_TO_LIMIT -> if (dark) 0xFFE9C46A.toInt() else 0xFFA66A12.toInt()
        BudgetStatus.OVER -> if (dark) 0xFFF2B8B5.toInt() else 0xFFB3261E.toInt()
    }

    private fun background(dark: Boolean): Int = if (dark) R.drawable.widget_bg_dark else R.drawable.widget_bg_light

    private fun itemBackground(dark: Boolean): Int =
        if (dark) R.drawable.widget_item_dark else R.drawable.widget_item_light

    private fun openApp(context: Context, uri: Uri, requestCode: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = uri
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun dpToPx(context: Context, dp: Int): Int =
        (dp * context.resources.displayMetrics.density).toInt().coerceAtLeast(1)

    private fun meterBitmap(width: Int, height: Int, progress: Float, color: Int, dark: Boolean): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val radius = height / 2f
        paint.color = if (dark) 0x33FFFFFF else 0x1A000000
        canvas.drawRoundRect(0f, 0f, width.toFloat(), height.toFloat(), radius, radius, paint)
        val filled = width * progress.coerceIn(0f, 1f)
        if (filled > 0.5f) {
            paint.color = color
            canvas.drawRoundRect(0f, 0f, filled, height.toFloat(), radius, radius, paint)
        }
        return bitmap
    }
}
