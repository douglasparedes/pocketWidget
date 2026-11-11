package com.pocketwidget.data

import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/**
 * Money is stored in the currency's minor units (cents, yen, fils) as integers.
 * The number of minor digits comes from the currency itself, never from a fixed 2.
 */
object Money {

    private const val MAX_MAJOR = 9_999_999L

    fun minorDigits(currencyCode: String): Int =
        runCatching { Currency.getInstance(currencyCode).defaultFractionDigits }
            .getOrDefault(2)
            .coerceIn(0, 3)

    fun symbol(currencyCode: String, locale: Locale = Locale.US): String =
        runCatching { Currency.getInstance(currencyCode).getSymbol(locale) }
            .getOrDefault(currencyCode)

    fun format(
        minor: Long,
        currencyCode: String,
        locale: Locale = Locale.US,
        showDecimalsWhenWhole: Boolean = true,
    ): String {
        val digits = minorDigits(currencyCode)
        val format = currencyFormat(currencyCode, locale, digits)
        if (!showDecimalsWhenWhole && minor % pow10(digits) == 0L) {
            format.minimumFractionDigits = 0
            format.maximumFractionDigits = 0
        }
        return format.format(BigDecimal.valueOf(minor).movePointLeft(digits))
    }

    /** Splits a formatted amount into whole and fractional parts, e.g. "€1,284" + ".50". */
    fun parts(minor: Long, currencyCode: String, locale: Locale = Locale.US): Pair<String, String> {
        val digits = minorDigits(currencyCode)
        if (digits == 0) return format(minor, currencyCode, locale) to ""
        val formatted = format(minor, currencyCode, locale)
        val separator = decimalSeparator(locale)
        val index = formatted.lastIndexOf(separator)
        return if (index > 0 && formatted.length - index == digits + 1) {
            formatted.substring(0, index) to formatted.substring(index)
        } else {
            formatted to ""
        }
    }

    /**
     * Parses keypad text ("12", "12.3", "12.34") into minor units for the given currency.
     * Returns null for unparseable input.
     */
    fun parseMinor(text: String, currencyCode: String): Long? {
        val digits = minorDigits(currencyCode)
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return null
        if (trimmed.count { it == '.' } > 1) return null
        val parts = trimmed.split('.')
        val majorText = parts[0]
        if (majorText.any { !it.isDigit() }) return null
        val minorText = parts.getOrNull(1).orEmpty()
        if (minorText.any { !it.isDigit() }) return null
        if (digits == 0 && minorText.isNotEmpty()) return null
        if (minorText.length > digits) return null
        val major = if (majorText.isEmpty()) 0L else majorText.toLongOrNull() ?: return null
        if (major > MAX_MAJOR) return null
        val scale = pow10(digits)
        val minor = if (digits == 0 || minorText.isEmpty()) {
            0L
        } else {
            minorText.padEnd(digits, '0').toLong()
        }
        return major * scale + minor
    }

    /** Renders minor units as keypad text ("12", "12.30", "1234" for JPY). */
    fun toKeypad(minor: Long, currencyCode: String): String {
        val digits = minorDigits(currencyCode)
        val scale = pow10(digits)
        val major = minor / scale
        val fraction = minor % scale
        return if (digits == 0 || fraction == 0L) {
            major.toString()
        } else {
            String.format(Locale.US, "%d.%0${digits}d", major, fraction)
        }
    }

    fun pow10(digits: Int): Long = when (digits) {
        0 -> 1L
        1 -> 10L
        2 -> 100L
        else -> 1000L
    }

    private fun currencyFormat(currencyCode: String, locale: Locale, digits: Int): NumberFormat {
        val format = NumberFormat.getCurrencyInstance(locale)
        runCatching { format.currency = Currency.getInstance(currencyCode) }
        format.minimumFractionDigits = digits
        format.maximumFractionDigits = digits
        return format
    }

    private fun decimalSeparator(locale: Locale): Char =
        java.text.DecimalFormatSymbols.getInstance(locale).monetaryDecimalSeparator
}
