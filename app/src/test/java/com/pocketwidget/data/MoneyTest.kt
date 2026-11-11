package com.pocketwidget.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class MoneyTest {

    private val us = Locale.US

    @Test
    fun formatsMinorUnitsWithCurrencyDigits() {
        assertEquals("$12.34", Money.format(1234, "USD", us))
        assertEquals("$0.05", Money.format(5, "USD", us))
        assertEquals("$1,200.00", Money.format(120000, "USD", us))
        assertEquals("¥1,234", Money.format(1234, "JPY", us))
        assertTrue(Money.format(1234, "KWD", us).contains("1.234"))
    }

    @Test
    fun knowsCurrencyMinorDigits() {
        assertEquals(2, Money.minorDigits("USD"))
        assertEquals(2, Money.minorDigits("EUR"))
        assertEquals(0, Money.minorDigits("JPY"))
        assertEquals(3, Money.minorDigits("KWD"))
    }

    @Test
    fun canDropDecimalsWhenWhole() {
        assertEquals("$1,200", Money.format(120000, "USD", us, showDecimalsWhenWhole = false))
        assertEquals("$1,200.50", Money.format(120050, "USD", us, showDecimalsWhenWhole = false))
    }

    @Test
    fun splitsWholeAndFraction() {
        val (whole, fraction) = Money.parts(123450, "USD", us)
        assertEquals("$1,234", whole)
        assertEquals(".50", fraction)

        val (yen, none) = Money.parts(1234, "JPY", us)
        assertEquals("¥1,234", yen)
        assertEquals("", none)
    }

    @Test
    fun parsesKeypadInputWithCurrencyScale() {
        assertEquals(1200L, Money.parseMinor("12", "USD"))
        assertEquals(1230L, Money.parseMinor("12.3", "USD"))
        assertEquals(1234L, Money.parseMinor("12.34", "USD"))
        assertEquals(5L, Money.parseMinor("0.05", "USD"))
        assertEquals(0L, Money.parseMinor("0", "USD"))
        assertEquals(0L, Money.parseMinor(".", "USD"))

        assertEquals(12L, Money.parseMinor("12", "JPY"))
        assertEquals(12L, Money.parseMinor("12.", "JPY"))
        assertNull(Money.parseMinor("12.5", "JPY"))

        assertEquals(1234L, Money.parseMinor("1.234", "KWD"))
        assertEquals(1000L, Money.parseMinor("1", "KWD"))
    }

    @Test
    fun rejectsInvalidInput() {
        assertNull(Money.parseMinor("", "USD"))
        assertNull(Money.parseMinor("abc", "USD"))
        assertNull(Money.parseMinor("1.234", "USD"))
        assertNull(Money.parseMinor("1.2.3", "USD"))
        assertNull(Money.parseMinor("-5", "USD"))
    }

    @Test
    fun roundTripsThroughKeypadText() {
        assertEquals("12.34", Money.toKeypad(1234, "USD"))
        assertEquals("12", Money.toKeypad(1200, "USD"))
        assertEquals("1234", Money.toKeypad(1234, "JPY"))
        assertEquals("1.234", Money.toKeypad(1234, "KWD"))
    }

    @Test
    fun symbolResolvesForKnownCurrency() {
        assertEquals("$", Money.symbol("USD", us))
    }
}
