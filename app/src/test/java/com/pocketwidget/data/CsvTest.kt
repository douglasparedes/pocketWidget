package com.pocketwidget.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CsvTest {

    @Test
    fun writesHeaderAndRows() {
        val rows = listOf(
            CsvRow(LocalDate.of(2026, 11, 9), 1234, "EUR", "Food", "Ramen"),
            CsvRow(LocalDate.of(2026, 11, 8), 500, "EUR", "Coffee", ""),
        )
        val csv = Csv.build(rows)
        val lines = csv.removePrefix("\uFEFF").trim().lines()
        assertEquals(Csv.HEADER, lines[0])
        assertEquals("2026-11-09,12.34,EUR,Food,Ramen", lines[1])
        assertEquals("2026-11-08,5.00,EUR,Coffee,", lines[2])
        assertTrue(csv.startsWith("\uFEFF"))
    }

    @Test
    fun escapesCommasQuotesAndNewlines() {
        assertEquals("\"a,b\"", Csv.escape("a,b"))
        assertEquals("\"say \"\"hi\"\"\"", Csv.escape("say \"hi\""))
        assertEquals("\"line1\nline2\"", Csv.escape("line1\nline2"))
        assertEquals("plain", Csv.escape("plain"))
    }

    @Test
    fun formatsAmountsPerCurrencyPrecision() {
        assertEquals("12.34", Csv.formatAmount(1234, "USD"))
        assertEquals("1234", Csv.formatAmount(1234, "JPY"))
        assertEquals("-3.00", Csv.formatAmount(-300, "USD"))
        assertEquals("0.005", Csv.formatAmount(5, "KWD"))
    }
}
