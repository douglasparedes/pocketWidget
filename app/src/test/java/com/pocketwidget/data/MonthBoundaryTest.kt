package com.pocketwidget.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId

class MonthBoundaryTest {

    private val zone = ZoneId.of("Europe/Budapest")

    private fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int, inZone: ZoneId = zone): Long =
        LocalDateTime.of(year, month, day, hour, minute).atZone(inZone).toInstant().toEpochMilli()

    @Test
    fun includesTheLastMinuteAndExcludesTheNextMonth() {
        val march = YearMonth.of(2026, 3)
        assertTrue(MonthRange.contains(march, zone, at(2026, 3, 31, 23, 59)))
        assertFalse(MonthRange.contains(march, zone, at(2026, 4, 1, 0, 0)))
    }

    @Test
    fun handlesSpringForwardDstBoundary() {
        val march = YearMonth.of(2026, 3)
        // 1 March 00:00 CET = 28 Feb 23:00Z; 1 April 00:00 CEST = 31 March 22:00Z.
        assertEquals(
            Instant.parse("2026-02-28T23:00:00Z").toEpochMilli(),
            MonthRange.start(march, zone),
        )
        assertEquals(
            Instant.parse("2026-03-31T22:00:00Z").toEpochMilli(),
            MonthRange.end(march, zone),
        )
    }

    @Test
    fun handlesFallBackDstBoundary() {
        val october = YearMonth.of(2026, 10)
        // 1 October 00:00 CEST = 30 Sep 22:00Z; 1 November 00:00 CET = 31 Oct 23:00Z.
        assertEquals(
            Instant.parse("2026-09-30T22:00:00Z").toEpochMilli(),
            MonthRange.start(october, zone),
        )
        assertEquals(
            Instant.parse("2026-10-31T23:00:00Z").toEpochMilli(),
            MonthRange.end(october, zone),
        )
    }

    @Test
    fun sameInstantCanBelongToDifferentMonthsByZone() {
        val instant = Instant.parse("2026-03-31T16:00:00Z").toEpochMilli()
        val march = YearMonth.of(2026, 3)
        assertTrue(MonthRange.contains(march, ZoneId.of("UTC"), instant))
        // In Tokyo this is already 1 April 01:00.
        assertFalse(MonthRange.contains(march, ZoneId.of("Asia/Tokyo"), instant))
    }
}
