package com.pedroloureiro.muzzchat.utils

import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class DateTimeUtilsTest {

    private val thursdayMarch12 = millis("2026-03-12T11:59:00Z")

    @Test
    fun hourMin_showsTimeOnly() {
        assertEquals("11:59", format(DateTimeFormat.HOUR_MIN))
    }

    @Test
    fun dayOfWeekHourMin_showsWeekdayAndTime() {
        assertEquals("Thursday 11:59", format(DateTimeFormat.DAY_OF_WEEK_HOUR_MIN))
    }

    @Test
    fun dayAbrMonthYearHourMin_showsFullDateAndTime() {
        assertEquals("12 Mar 2026 11:59", format(DateTimeFormat.DAY_ABR_MONTH_YEAR_HOUR_MIN))
    }

    @Test
    fun zoneId_convertsFromUtc() {
        assertEquals(
            "20:59",
            formatUtc(thursdayMarch12, ZoneId.of("Asia/Tokyo"), Locale.UK, DateTimeFormat.HOUR_MIN)
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
                DateTimeFormat.DAY_OF_WEEK_HOUR_MIN
            )
        )
    }

    @Test
    fun calendarDaysBetween_sameDay_isZero() {
        assertEquals(
            0L,
            calendarDaysBetween(millis("2026-03-12T00:00:00Z"), millis("2026-03-12T23:59:00Z"), ZoneOffset.UTC)
        )
    }

    @Test
    fun calendarDaysBetween_countsCalendarDaysNotElapsedTime() {
        assertEquals(
            1L,
            calendarDaysBetween(millis("2026-03-11T23:00:00Z"), millis("2026-03-12T00:30:00Z"), ZoneOffset.UTC)
        )
    }

    @Test
    fun calendarDaysBetween_usesZone() {
        assertEquals(
            0L,
            calendarDaysBetween(
                millis("2026-03-11T23:00:00Z"),
                millis("2026-03-12T00:30:00Z"),
                ZoneId.of("Asia/Tokyo")
            )
        )
    }

    @Test
    fun calendarDaysBetween_laterFrom_isNegative() {
        assertEquals(
            -1L,
            calendarDaysBetween(millis("2026-03-13T00:01:00Z"), millis("2026-03-12T23:59:00Z"), ZoneOffset.UTC)
        )
    }

    private fun format(format: DateTimeFormat) =
        formatUtc(thursdayMarch12, ZoneOffset.UTC, Locale.UK, format)

    private fun millis(instant: String) = Instant.parse(instant).toEpochMilli()
}
