package com.pedroloureiro.muzzchat.utils

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class TimestampFormat(val pattern: String) {
    HOUR_MIN("HH:mm"),
    DAY_OF_WEEK_HOUR_MIN("EEEE HH:mm"),
    DAY_ABR_MONTH_YEAR_HOUR_MIN("d MMM yyyy HH:mm")
}

fun formatUtc(utcMillis: Long, zoneId: ZoneId, locale: Locale, format: TimestampFormat): String =
    DateTimeFormatter.ofPattern(format.pattern, locale)
        .format(Instant.ofEpochMilli(utcMillis).atZone(zoneId))
