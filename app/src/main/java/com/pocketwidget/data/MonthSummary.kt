package com.pocketwidget.data

import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

enum class BudgetStatus { NO_LIMIT, ON_TRACK, CLOSE_TO_LIMIT, OVER }

data class CategorySpend(
    val category: Category,
    val cents: Long,
    val share: Float,
)

data class MonthSummary(
    val month: YearMonth,
    val totalCents: Long,
    val limitCents: Long,
    val transactionCount: Int,
    val byCategory: List<CategorySpend>,
    val todayCents: Long,
    val dailyAverageCents: Long,
    val projectedCents: Long,
) {
    val status: BudgetStatus
        get() = when {
            limitCents <= 0L -> BudgetStatus.NO_LIMIT
            totalCents > limitCents -> BudgetStatus.OVER
            totalCents * 100L >= limitCents * 80L -> BudgetStatus.CLOSE_TO_LIMIT
            else -> BudgetStatus.ON_TRACK
        }

    /** 0..1 (clamped) for display; the raw overrun is available via [overByCents]. */
    val progress: Float
        get() = if (limitCents <= 0L) 0f else (totalCents.toDouble() / limitCents).coerceIn(0.0, 1.0).toFloat()

    val remainingCents: Long
        get() = (limitCents - totalCents).coerceAtLeast(0L)

    val overByCents: Long
        get() = (totalCents - limitCents).coerceAtLeast(0L)

    companion object {
        fun empty(month: YearMonth, limitCents: Long = 0L) = MonthSummary(
            month = month,
            totalCents = 0L,
            limitCents = limitCents,
            transactionCount = 0,
            byCategory = emptyList(),
            todayCents = 0L,
            dailyAverageCents = 0L,
            projectedCents = 0L,
        )
    }
}

object MonthMath {

    fun summarize(
        month: YearMonth,
        transactions: List<Transaction>,
        categories: List<Category>,
        limitCents: Long,
        nowMillis: Long,
        zone: ZoneId,
    ): MonthSummary {
        val total = transactions.sumOf { it.amountMinor }
        val byId = categories.associateBy { it.id }
        val spends = transactions
            .groupBy { it.categoryId }
            .map { (categoryId, list) ->
                val category = byId[categoryId] ?: Category(
                    id = categoryId,
                    name = "Unknown",
                    emoji = "•",
                    color = 0xFF9A8C98,
                    sortOrder = Int.MAX_VALUE,
                )
                CategorySpend(category, list.sumOf { it.amountMinor }, 0f)
            }
            .sortedWith(compareByDescending<CategorySpend> { it.cents }.thenBy { it.category.sortOrder })
            .map { it.copy(share = if (total > 0L) it.cents / total.toFloat() else 0f) }

        val today = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
        val todayCents = transactions
            .filter { Instant.ofEpochMilli(it.occurredAt).atZone(zone).toLocalDate() == today }
            .sumOf { it.amountMinor }

        val currentMonth = YearMonth.from(today)
        val daysInMonth = month.lengthOfMonth()
        val elapsedDays = when {
            month.isAfter(currentMonth) -> 0
            month.isBefore(currentMonth) -> daysInMonth
            else -> today.dayOfMonth
        }
        val dailyAverage = if (elapsedDays > 0) total / elapsedDays else 0L
        val projected = dailyAverage * daysInMonth

        return MonthSummary(
            month = month,
            totalCents = total,
            limitCents = limitCents,
            transactionCount = transactions.size,
            byCategory = spends,
            todayCents = todayCents,
            dailyAverageCents = dailyAverage,
            projectedCents = projected,
        )
    }
}
