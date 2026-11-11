package com.pocketwidget.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Currency
import java.util.Locale

enum class WidgetTheme { SYSTEM, LIGHT, DARK }

enum class WidgetAccent(val argb: Long, val label: String) {
    FOREST(0xFF1F4D3A, "Forest"),
    TERRACOTTA(0xFFB4552D, "Terracotta"),
    OCEAN(0xFF2A6F97, "Ocean"),
    PLUM(0xFF6D4C7D, "Plum"),
}

data class Settings(
    val monthlyLimitCents: Long = 0L,
    val currencyCode: String = defaultCurrencyCode(),
    val widgetTheme: WidgetTheme = WidgetTheme.SYSTEM,
    val widgetAccent: WidgetAccent = WidgetAccent.FOREST,
    val showWidgetAmounts: Boolean = true,
    val showWidgetMeter: Boolean = true,
)

fun defaultCurrencyCode(): String =
    runCatching { Currency.getInstance(Locale.getDefault()).currencyCode }.getOrDefault("EUR")

object Currencies {
    val COMMON: List<Pair<String, String>> = listOf(
        "EUR" to "Euro",
        "USD" to "US dollar",
        "GBP" to "British pound",
        "CHF" to "Swiss franc",
        "HUF" to "Hungarian forint",
        "PLN" to "Polish złoty",
        "CZK" to "Czech koruna",
        "SEK" to "Swedish krona",
        "DKK" to "Danish krone",
        "CAD" to "Canadian dollar",
        "AUD" to "Australian dollar",
        "JPY" to "Japanese yen",
    )
}

class SettingsStore(context: Context) {

    private val prefs = context.getSharedPreferences("pocketwidget_settings", Context.MODE_PRIVATE)

    private val state = MutableStateFlow(read())
    val settings: StateFlow<Settings> = state.asStateFlow()

    fun setMonthlyLimit(cents: Long) = update { it.copy(monthlyLimitCents = cents) }

    fun setCurrency(code: String) = update { it.copy(currencyCode = code) }

    fun setWidgetTheme(theme: WidgetTheme) = update { it.copy(widgetTheme = theme) }

    fun setWidgetAccent(accent: WidgetAccent) = update { it.copy(widgetAccent = accent) }

    fun setShowWidgetAmounts(show: Boolean) = update { it.copy(showWidgetAmounts = show) }

    fun setShowWidgetMeter(show: Boolean) = update { it.copy(showWidgetMeter = show) }

    fun quickCategoryIds(appWidgetId: Int): List<String> {
        val raw = prefs.getString("widget_categories_$appWidgetId", null) ?: return emptyList()
        return raw.split(',').filter { it.isNotBlank() }
    }

    fun setQuickCategoryIds(appWidgetId: Int, ids: List<String>) {
        prefs.edit().putString("widget_categories_$appWidgetId", ids.joinToString(",")).apply()
    }

    fun clearQuickCategoryIds(appWidgetId: Int) {
        prefs.edit().remove("widget_categories_$appWidgetId").apply()
    }

    private fun read(): Settings = Settings(
        monthlyLimitCents = prefs.getLong(KEY_LIMIT, 0L),
        currencyCode = prefs.getString(KEY_CURRENCY, null) ?: defaultCurrencyCode(),
        widgetTheme = prefs.getString(KEY_THEME, null)
            ?.let { runCatching { WidgetTheme.valueOf(it) }.getOrNull() }
            ?: WidgetTheme.SYSTEM,
        widgetAccent = prefs.getString(KEY_ACCENT, null)
            ?.let { runCatching { WidgetAccent.valueOf(it) }.getOrNull() }
            ?: WidgetAccent.FOREST,
        showWidgetAmounts = prefs.getBoolean(KEY_SHOW_AMOUNTS, true),
        showWidgetMeter = prefs.getBoolean(KEY_SHOW_METER, true),
    )

    private fun update(transform: (Settings) -> Settings) {
        val next = transform(state.value)
        prefs.edit()
            .putLong(KEY_LIMIT, next.monthlyLimitCents)
            .putString(KEY_CURRENCY, next.currencyCode)
            .putString(KEY_THEME, next.widgetTheme.name)
            .putString(KEY_ACCENT, next.widgetAccent.name)
            .putBoolean(KEY_SHOW_AMOUNTS, next.showWidgetAmounts)
            .putBoolean(KEY_SHOW_METER, next.showWidgetMeter)
            .apply()
        state.value = next
    }

    private companion object {
        const val KEY_LIMIT = "monthly_limit_cents"
        const val KEY_CURRENCY = "currency_code"
        const val KEY_THEME = "widget_theme"
        const val KEY_ACCENT = "widget_accent"
        const val KEY_SHOW_AMOUNTS = "widget_show_amounts"
        const val KEY_SHOW_METER = "widget_show_meter"
    }
}
