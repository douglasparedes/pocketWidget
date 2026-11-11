package com.pocketwidget.ui

import android.app.Application
import android.content.pm.ApplicationInfo
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pocketwidget.PocketApp
import com.pocketwidget.categorize.RuleBasedCategorizer
import com.pocketwidget.data.Category
import com.pocketwidget.data.DefaultCategories
import com.pocketwidget.data.MonthSummary
import com.pocketwidget.data.Settings
import com.pocketwidget.data.Transaction
import com.pocketwidget.data.WidgetAccent
import com.pocketwidget.data.WidgetTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

sealed interface Route {
    data object Home : Route
    data object History : Route
    data object Settings : Route
    data object Categories : Route
    data class AddExpense(
        val transactionId: Long? = null,
        val categoryId: String? = null,
        val occurredAt: Long? = null,
    ) : Route
}

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as PocketApp
    private val repository = app.container.repository
    private val settingsStore = app.container.settingsStore

    val categorizer = RuleBasedCategorizer()

    private val _month = MutableStateFlow(YearMonth.now(repository.zone()))
    val month: StateFlow<YearMonth> = _month.asStateFlow()

    private val _backStack = MutableStateFlow<List<Route>>(listOf(Route.Home))
    val backStack: StateFlow<List<Route>> = _backStack.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _saveError = MutableStateFlow<String?>(null)
    val saveError: StateFlow<String?> = _saveError.asStateFlow()

    val settings: StateFlow<Settings> = settingsStore.settings

    val categories: StateFlow<List<Category>> = repository.categories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DefaultCategories.ALL)

    val transactionCount: StateFlow<Int> = repository.transactionCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val summary: StateFlow<MonthSummary> = _month
        .flatMapLatest { repository.observeMonth(it) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            MonthSummary.empty(YearMonth.now(repository.zone()), settingsStore.settings.value.monthlyLimitCents),
        )

    val transactions: StateFlow<List<Transaction>> = _month
        .flatMapLatest { repository.observeTransactions(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch { repository.ensureSeeded() }
    }

    // region navigation

    fun navigate(route: Route) {
        _backStack.update { stack ->
            if (stack.lastOrNull() == route) stack else stack + route
        }
    }

    fun openFromIntent(route: Route) {
        _backStack.value = if (route == Route.Home) listOf(Route.Home) else listOf(Route.Home, route)
    }

    fun pop() {
        _backStack.update { if (it.size > 1) it.dropLast(1) else it }
    }

    // endregion

    fun previousMonth() {
        _month.update { it.minusMonths(1) }
    }

    fun nextMonth() {
        _month.update { it.plusMonths(1) }
    }

    fun localDate(millis: Long): LocalDate = repository.toLocalDate(millis)

    fun zone(): ZoneId = repository.zone()

    suspend fun loadTransaction(id: Long): Transaction? = repository.transaction(id)

    fun dayLabel(millis: Long): String {
        val date = repository.toLocalDate(millis)
        val today = LocalDate.now(repository.zone())
        return when (date) {
            today -> "Today"
            today.minusDays(1) -> "Yesterday"
            else -> date.format(DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault()))
        }
    }

    fun saveTransaction(
        id: Long?,
        amountMinor: Long,
        categoryId: String,
        note: String,
        occurredAt: Long,
    ) {
        if (_isSaving.value) return
        viewModelScope.launch {
            _isSaving.value = true
            _saveError.value = null
            runCatching {
                if (id == null) {
                    repository.addTransaction(amountMinor, categoryId, note, occurredAt)
                } else {
                    repository.updateTransaction(id, amountMinor, categoryId, note, occurredAt)
                }
            }.onSuccess {
                pop()
            }.onFailure {
                _saveError.value = "Could not save this expense. Please try again."
            }
            _isSaving.value = false
        }
    }

    fun clearSaveError() {
        _saveError.value = null
    }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch {
            runCatching { repository.deleteTransaction(id) }
                .onSuccess { pop() }
                .onFailure { _saveError.value = "Could not delete this expense." }
        }
    }

    fun clearAllData() {
        viewModelScope.launch { repository.clearAllData() }
    }

    fun saveCategory(name: String, emoji: String, color: Long) {
        viewModelScope.launch { repository.addCategory(name, emoji, color) }
    }

    fun setCategoryArchived(id: String, archived: Boolean) {
        viewModelScope.launch { repository.setCategoryArchived(id, archived) }
    }

    fun setMonthlyLimit(cents: Long) = settingsStore.setMonthlyLimit(cents)

    /** Currency is locked once data exists so history can never be silently relabelled. */
    fun setCurrency(code: String) {
        viewModelScope.launch {
            if (repository.hasTransactions()) return@launch
            if (code == settingsStore.settings.value.currencyCode) return@launch
            settingsStore.setCurrency(code)
            // Minor-unit scale changes with the currency; the old limit is meaningless.
            settingsStore.setMonthlyLimit(0L)
        }
    }

    fun setWidgetTheme(theme: WidgetTheme) = settingsStore.setWidgetTheme(theme)

    fun setWidgetAccent(accent: WidgetAccent) = settingsStore.setWidgetAccent(accent)

    fun setShowWidgetAmounts(show: Boolean) = settingsStore.setShowWidgetAmounts(show)

    fun setShowWidgetMeter(show: Boolean) = settingsStore.setShowWidgetMeter(show)

    fun exportCsv(uri: Uri, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    val csv = repository.csvContent()
                    getApplication<Application>().contentResolver.openOutputStream(uri)?.use { stream ->
                        stream.write(csv.toByteArray(Charsets.UTF_8))
                    } ?: error("Unable to open the chosen file")
                }.isSuccess
            }
            withContext(Dispatchers.Main) { onResult(ok) }
        }
    }

    fun seedDemoData() {
        viewModelScope.launch { repository.seedDemoData() }
    }

    fun isDebuggable(): Boolean =
        (getApplication<Application>().applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
}
