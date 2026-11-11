package com.pocketwidget.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId

class MonthMathTest {

    private val zone = ZoneId.of("Europe/Budapest")
    private val month = YearMonth.of(2026, 11)
    private val now = LocalDateTime.of(2026, 11, 9, 12, 0).atZone(zone).toInstant().toEpochMilli()

    private fun tx(cents: Long, categoryId: String, date: LocalDate, hour: Int = 12) =
        Transaction(
            amountMinor = cents,
            categoryId = categoryId,
            occurredAt = date.atTime(hour, 0).atZone(zone).toInstant().toEpochMilli(),
        )

    @Test
    fun sumsTotalsAndGroupsByCategory() {
        val transactions = listOf(
            tx(1200, "food", LocalDate.of(2026, 11, 8)),
            tx(800, "food", LocalDate.of(2026, 11, 9)),
            tx(300, "coffee", LocalDate.of(2026, 11, 9)),
        )
        val summary = MonthMath.summarize(month, transactions, DefaultCategories.ALL, 10000, now, zone)

        assertEquals(2300L, summary.totalCents)
        assertEquals(3, summary.transactionCount)
        assertEquals(listOf("food", "coffee"), summary.byCategory.map { it.category.id })
        assertEquals(0.87f, summary.byCategory.first().share, 0.01f)
        assertEquals(1100L, summary.todayCents)
    }

    @Test
    fun computesProjectionFromElapsedDays() {
        val transactions = listOf(tx(9000, "food", LocalDate.of(2026, 11, 5)))
        val summary = MonthMath.summarize(month, transactions, DefaultCategories.ALL, 0, now, zone)

        assertEquals(1000L, summary.dailyAverageCents)
        assertEquals(30000L, summary.projectedCents)
    }

    @Test
    fun statusFollowsLimitThresholds() {
        val transactions = listOf(tx(50000, "food", LocalDate.of(2026, 11, 5)))

        assertEquals(BudgetStatus.NO_LIMIT, summary(transactions, 0).status)
        assertEquals(BudgetStatus.ON_TRACK, summary(transactions, 100000).status)
        assertEquals(BudgetStatus.CLOSE_TO_LIMIT, summary(transactions, 60000).status)
        assertEquals(BudgetStatus.OVER, summary(transactions, 40000).status)
    }

    @Test
    fun reportsRemainingAndOverrun() {
        val transactions = listOf(tx(15000, "food", LocalDate.of(2026, 11, 5)))

        assertEquals(5000L, summary(transactions, 20000).remainingCents)
        assertEquals(0L, summary(transactions, 20000).overByCents)

        assertEquals(0L, summary(transactions, 10000).remainingCents)
        assertEquals(5000L, summary(transactions, 10000).overByCents)
        assertEquals(1f, summary(transactions, 10000).progress, 0.001f)
    }

    @Test
    fun unknownCategoryFallsBackGracefully() {
        val transactions = listOf(tx(1000, "ghost", LocalDate.of(2026, 11, 1)))
        val summary = MonthMath.summarize(month, transactions, DefaultCategories.ALL, 0, now, zone)

        assertEquals("Unknown", summary.byCategory.single().category.name)
        assertTrue(summary.byCategory.single().share == 1f)
    }

    private fun summary(transactions: List<Transaction>, limit: Long) =
        MonthMath.summarize(month, transactions, DefaultCategories.ALL, limit, now, zone)
}
