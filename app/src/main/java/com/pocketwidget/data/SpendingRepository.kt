package com.pocketwidget.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.combine
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

data class WidgetSnapshot(
    val month: YearMonth,
    /** Total in minor units of [currencyCode]. */
    val totalCents: Long,
    val limitCents: Long,
    val currencyCode: String,
    val topCategory: CategorySpend?,
    val otherCategoryCount: Int,
    val status: BudgetStatus,
    val progress: Float,
)

class SpendingRepository(
    private val db: PocketDatabase,
    private val settingsStore: SettingsStore,
    private val zoneProvider: () -> ZoneId = { ZoneId.systemDefault() },
    private val now: () -> Long = System::currentTimeMillis,
) {

    private val _dataChanges = MutableSharedFlow<Unit>(extraBufferCapacity = 4)
    val dataChanges: SharedFlow<Unit> = _dataChanges

    val settings = settingsStore.settings
    val categories: Flow<List<Category>> = db.categories().observeAll()
    val transactionCount: Flow<Int> = db.transactions().observeCount()

    fun observeMonth(month: YearMonth): Flow<MonthSummary> {
        val zone = zoneProvider()
        return combine(
            db.transactions().observeBetween(monthStart(month, zone), monthEnd(month, zone)),
            db.categories().observeAll(),
            settingsStore.settings,
        ) { transactions, categories, settings ->
            MonthMath.summarize(
                month = month,
                transactions = transactions,
                categories = categories,
                limitCents = settings.monthlyLimitCents,
                nowMillis = now(),
                zone = zone,
            )
        }
    }

    fun observeTransactions(month: YearMonth): Flow<List<Transaction>> {
        val zone = zoneProvider()
        return db.transactions().observeBetween(monthStart(month, zone), monthEnd(month, zone))
    }

    suspend fun transaction(id: Long): Transaction? = db.transactions().findById(id)

    suspend fun ensureSeeded() {
        if (db.categories().count() == 0) {
            DefaultCategories.ALL.forEach { db.categories().upsert(it) }
        }
        // v1 rows had no currency; adopt the user's current setting for them.
        db.transactions().backfillCurrency(settingsStore.settings.value.currencyCode)
    }

    suspend fun addTransaction(amountMinor: Long, categoryId: String, note: String, occurredAt: Long) {
        db.transactions().insert(
            Transaction(
                amountMinor = amountMinor,
                categoryId = categoryId,
                note = note.trim(),
                currencyCode = settingsStore.settings.value.currencyCode,
                occurredAt = occurredAt,
            ),
        )
        _dataChanges.tryEmit(Unit)
    }

    suspend fun updateTransaction(
        id: Long,
        amountMinor: Long,
        categoryId: String,
        note: String,
        occurredAt: Long,
    ) {
        val existing = db.transactions().findById(id) ?: return
        db.transactions().update(
            existing.copy(
                amountMinor = amountMinor,
                categoryId = categoryId,
                note = note.trim(),
                occurredAt = occurredAt,
                // Preserve the historical currency; a single expense is never relabelled.
                currencyCode = existing.currencyCode.ifBlank {
                    settingsStore.settings.value.currencyCode
                },
            ),
        )
        _dataChanges.tryEmit(Unit)
    }

    suspend fun deleteTransaction(id: Long) {
        val existing = db.transactions().findById(id) ?: return
        db.transactions().delete(existing)
        _dataChanges.tryEmit(Unit)
    }

    suspend fun hasTransactions(): Boolean = db.transactions().count() > 0

    suspend fun clearAllData() {
        db.transactions().deleteAll()
        settingsStore.setMonthlyLimit(0L)
        _dataChanges.tryEmit(Unit)
    }

    suspend fun addCategory(name: String, emoji: String, color: Long) {
        val order = (db.categories().all().maxOfOrNull { it.sortOrder } ?: 0) + 1
        db.categories().upsert(
            Category(
                id = "custom_" + java.util.UUID.randomUUID().toString().take(8),
                name = name.trim(),
                emoji = emoji,
                color = color,
                sortOrder = order,
            ),
        )
        _dataChanges.tryEmit(Unit)
    }

    suspend fun setCategoryArchived(id: String, archived: Boolean) {
        if (id == DefaultCategories.FALLBACK_ID) return
        db.categories().setArchived(id, archived)
        _dataChanges.tryEmit(Unit)
    }

    suspend fun monthlySnapshot(month: YearMonth): WidgetSnapshot {
        val settings = settingsStore.settings.value
        val zone = zoneProvider()
        val transactions = db.transactions().between(monthStart(month, zone), monthEnd(month, zone))
        val categories = db.categories().all()
        val summary = MonthMath.summarize(
            month = month,
            transactions = transactions,
            categories = categories,
            limitCents = settings.monthlyLimitCents,
            nowMillis = now(),
            zone = zone,
        )
        return WidgetSnapshot(
            month = month,
            totalCents = summary.totalCents,
            limitCents = summary.limitCents,
            currencyCode = settings.currencyCode,
            topCategory = summary.byCategory.firstOrNull(),
            otherCategoryCount = (summary.byCategory.size - 1).coerceAtLeast(0),
            status = summary.status,
            progress = summary.progress,
        )
    }

    suspend fun csvContent(): String {
        val zone = zoneProvider()
        val categories = db.categories().all().associateBy { it.id }
        val rows = db.transactions().all().map { transaction ->
            CsvRow(
                date = Instant.ofEpochMilli(transaction.occurredAt).atZone(zone).toLocalDate(),
                amountMinor = transaction.amountMinor,
                currencyCode = transaction.currencyCode.ifBlank {
                    settingsStore.settings.value.currencyCode
                },
                category = categories[transaction.categoryId]?.name ?: "Unknown",
                note = transaction.note,
            )
        }
        return Csv.build(rows)
    }

    suspend fun seedDemoData() {
        val zone = zoneProvider()
        val today = LocalDate.now(zone)
        val maxOffset = (today.dayOfMonth - 1).coerceAtLeast(0)
        val currency = settingsStore.settings.value.currencyCode
        DemoData.SAMPLES.forEach { sample ->
            val offset = sample.daysAgo.coerceAtMost(maxOffset)
            val date = today.minusDays(offset.toLong())
            db.transactions().insert(
                Transaction(
                    amountMinor = sample.minor,
                    categoryId = sample.categoryId,
                    note = sample.note,
                    currencyCode = currency,
                    occurredAt = date.atTime(sample.hour, 12).atZone(zone).toInstant().toEpochMilli(),
                ),
            )
        }
        _dataChanges.tryEmit(Unit)
    }

    fun monthStart(month: YearMonth): Long = MonthRange.start(month, zoneProvider())

    fun monthEnd(month: YearMonth): Long = MonthRange.end(month, zoneProvider())

    private fun monthStart(month: YearMonth, zone: ZoneId): Long = MonthRange.start(month, zone)

    private fun monthEnd(month: YearMonth, zone: ZoneId): Long = MonthRange.end(month, zone)

    fun toLocalDate(millis: Long): LocalDate =
        Instant.ofEpochMilli(millis).atZone(zoneProvider()).toLocalDate()

    fun zone(): ZoneId = zoneProvider()
}

internal object DemoData {

    data class Sample(val daysAgo: Int, val categoryId: String, val minor: Long, val note: String, val hour: Int)

    val SAMPLES: List<Sample> = listOf(
        Sample(8, "bills", 78000L, "Rent — November", 9),
        Sample(7, "groceries", 4230L, "Weekly shop", 18),
        Sample(7, "coffee", 380L, "Flat white", 8),
        Sample(6, "transport", 275L, "Metro card top-up", 8),
        Sample(5, "food", 1850L, "Ramen with Anna", 20),
        Sample(4, "coffee", 420L, "Oat latte", 9),
        Sample(3, "groceries", 2765L, "Veggies + bread", 19),
        Sample(3, "fun", 2400L, "Cinema tickets", 21),
        Sample(2, "transport", 1240L, "Taxi home", 23),
        Sample(2, "food", 950L, "Sandwich", 13),
        Sample(1, "shopping", 4590L, "Running socks", 17),
        Sample(1, "coffee", 350L, "Espresso", 8),
        Sample(0, "food", 1290L, "Lunch bowl", 13),
        Sample(0, "groceries", 1840L, "Fruit + yogurt", 18),
    )
}
