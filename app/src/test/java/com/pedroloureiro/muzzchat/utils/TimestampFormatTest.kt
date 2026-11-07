package com.pedroloureiro.muzzchat.utils

import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class TimestampFormatTest {

    private val thursdayMarch12 = Instant.parse("2026-03-12T11:59:00Z").toEpochMilli()

    @Test
    fun hourMin_showsTimeOnly() {
        assertEquals("11:59", format(TimestampFormat.HOUR_MIN))
    }

    @Test
    fun dayOfWeekHourMin_showsWeekdayAndTime() {
        assertEquals("Thursday 11:59", format(TimestampFormat.DAY_OF_WEEK_HOUR_MIN))
    }

    @Test
    fun dayAbrMonthYearHourMin_showsFullDateAndTime() {
        assertEquals("12 Mar 2026 11:59", format(TimestampFormat.DAY_ABR_MONTH_YEAR_HOUR_MIN))
    }

    @Test
    fun zoneId_convertsFromUtc() {
        assertEquals(
            "20:59",
            formatUtc(thursdayMarch12, ZoneId.of("Asia/Tokyo"), Locale.UK, TimestampFormat.HOUR_MIN)
        )
    }

    @Test
    fun locale_translatesNames() {
        assertEquals(
            "quinta-feira 11:59",
            formatUtc(
                thursdayMarch12,
                ZoneOffset.UTC,
                Locale.forLanguageTag("pt-PT"),
                TimestampFormat.DAY_OF_WEEK_HOUR_MIN
            )
        )
    }

    private fun format(format: TimestampFormat) =
        formatUtc(thursdayMarch12, ZoneOffset.UTC, Locale.UK, format)
}
