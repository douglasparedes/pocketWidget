package com.pocketwidget.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test

class AddKeyTest {

    @Test
    fun limitsFractionToCurrencyDigits() {
        assertEquals("12.34", applyKey("12.3", "4", 2))
        assertEquals("12.3", applyKey("12.3", "4", 1))
    }

    @Test
    fun decimalPointOnlyForFractionalCurrencies() {
        assertEquals("12", applyKey("12", ".", 0))
        assertEquals("0.", applyKey("", ".", 2))
        assertEquals("12.", applyKey("12", ".", 3))
    }

    @Test
    fun backspaceAndLeadingZero() {
        assertEquals("1", applyKey("12", "⌫", 2))
        assertEquals("5", applyKey("0", "5", 2))
    }

    @Test
    fun allowsWholeUnitCurrenciesWithoutDecimal() {
        assertEquals("1234", applyKey("123", "4", 0))
    }
}
