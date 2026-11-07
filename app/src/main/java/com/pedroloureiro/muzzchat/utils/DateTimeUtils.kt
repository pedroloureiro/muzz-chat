package com.pedroloureiro.muzzchat.utils

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

enum class DateTimeFormat(val pattern: String) {
    HOUR_MIN("HH:mm"),
    DAY_OF_WEEK_HOUR_MIN("EEEE HH:mm"),
    DAY_ABR_MONTH_YEAR_HOUR_MIN("d MMM yyyy HH:mm")
}

fun formatUtc(utcMillis: Long, zoneId: ZoneId, locale: Locale, format: DateTimeFormat): String =
    DateTimeFormatter.ofPattern(format.pattern, locale)
        .format(Instant.ofEpochMilli(utcMillis).atZone(zoneId))

fun calendarDaysBetween(fromUtcMillis: Long, toUtcMillis: Long, zoneId: ZoneId): Long {
    val fromDate = localDate(fromUtcMillis, zoneId)
    val toDate = localDate(toUtcMillis, zoneId)
    return ChronoUnit.DAYS.between(fromDate, toDate)
}

private fun localDate(utcMillis: Long, zoneId: ZoneId): LocalDate =
    Instant.ofEpochMilli(utcMillis).atZone(zoneId).toLocalDate()
