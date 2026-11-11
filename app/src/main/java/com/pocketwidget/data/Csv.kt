package com.pocketwidget.data

import java.time.LocalDate
import java.util.Locale

data class CsvRow(
    val date: LocalDate,
    val amountMinor: Long,
    val currencyCode: String,
    val category: String,
    val note: String,
)

/**
 * RFC 4180 style CSV with a UTF-8 BOM so spreadsheet apps detect the encoding.
 */
object Csv {

    const val HEADER = "date,amount,currency,category,note"

    fun build(rows: List<CsvRow>): String {
        val builder = StringBuilder("\uFEFF")
        builder.append(HEADER).append("\r\n")
        rows.forEach { row ->
            builder
                .append(row.date.toString()).append(',')
                .append(formatAmount(row.amountMinor, row.currencyCode)).append(',')
                .append(row.currencyCode).append(',')
                .append(escape(row.category)).append(',')
                .append(escape(row.note)).append("\r\n")
        }
        return builder.toString()
    }

    fun formatAmount(minor: Long, currencyCode: String): String {
        val digits = Money.minorDigits(currencyCode)
        val sign = if (minor < 0) "-" else ""
        val abs = if (minor < 0) -minor else minor
        if (digits == 0) return sign + abs
        val scale = Money.pow10(digits)
        return String.format(Locale.US, "%s%d.%0${digits}d", sign, abs / scale, abs % scale)
    }

    fun escape(value: String): String {
        val needsQuotes = value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
        val escaped = value.replace("\"", "\"\"")
        return if (needsQuotes) "\"$escaped\"" else escaped
    }
}
