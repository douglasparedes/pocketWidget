package com.pocketwidget.data

import java.time.YearMonth
import java.time.ZoneId

/**
 * Month boundaries as half-open epoch-millisecond ranges, resolved in a zone at call time
 * so timezone changes and DST transitions cannot shift a month silently.
 */
object MonthRange {

    fun start(month: YearMonth, zone: ZoneId): Long =
        month.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()

    fun end(month: YearMonth, zone: ZoneId): Long =
        month.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()

    fun contains(month: YearMonth, zone: ZoneId, epochMillis: Long): Boolean =
        epochMillis >= start(month, zone) && epochMillis < end(month, zone)
}
