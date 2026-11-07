package com.pedroloureiro.muzzchat.chat.ui

import com.pedroloureiro.muzzchat.chat.domain.model.Message
import com.pedroloureiro.muzzchat.chat.domain.model.Users
import com.pedroloureiro.muzzchat.chat.ui.model.ChatItem
import com.pedroloureiro.muzzchat.chat.ui.model.MessageDirection
import com.pedroloureiro.muzzchat.utils.DateTimeFormat
import com.pedroloureiro.muzzchat.utils.calendarDaysBetween
import com.pedroloureiro.muzzchat.utils.formatUtc
import java.time.ZoneId
import java.util.Locale
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

private val SECTION_GAP_MILLIS = 1.hours.inWholeMilliseconds
private val GROUPING_WINDOW_MILLIS = 20.seconds.inWholeMilliseconds

private const val TODAY = 0L
private val WITHIN_A_WEEK = 1L..6L

object ChatItemMapper {

    // Expects messages in ascending timestamp order, as ChatRepository emits them.
    // Now, zone and locale are read on every call so a change on the device applies at the next mapping.
    fun map(
        messages: List<Message>,
        nowMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault(),
        locale: Locale = Locale.getDefault()
    ): List<ChatItem> = messages.flatMapIndexed { index, message ->
        val previous = messages.getOrNull(index - 1)
        val next = messages.getOrNull(index + 1)
        buildList {
            if (previous == null || message.timestampMillis - previous.timestampMillis > SECTION_GAP_MILLIS) {
                val format = headerFormat(message.timestampMillis, nowMillis, zoneId)
                add(ChatItem.TimestampHeader(formatUtc(message.timestampMillis, zoneId, locale, format)))
            }
            add(message.toChatItem(isGrouped = next != null && isGrouped(message, next)))
        }
    }

    private fun isGrouped(message: Message, next: Message) =
        next.senderId == message.senderId &&
            next.timestampMillis - message.timestampMillis < GROUPING_WINDOW_MILLIS

    private fun headerFormat(timestampMillis: Long, nowMillis: Long, zoneId: ZoneId): DateTimeFormat {
        val daysAgo = calendarDaysBetween(timestampMillis, nowMillis, zoneId)
        return when {
            daysAgo < TODAY -> DateTimeFormat.DAY_ABR_MONTH_YEAR_HOUR_MIN // dated after today, e.g. a clock ahead of the device
            daysAgo == TODAY -> DateTimeFormat.HOUR_MIN
            daysAgo in WITHIN_A_WEEK -> DateTimeFormat.DAY_OF_WEEK_HOUR_MIN
            else -> DateTimeFormat.DAY_ABR_MONTH_YEAR_HOUR_MIN
        }
    }

    private fun Message.toChatItem(isGrouped: Boolean) = ChatItem.Message(
        messageId = messageId,
        direction = if (senderId == Users.ME.id) MessageDirection.OUTGOING else MessageDirection.INCOMING,
        content = content.value,
        timestampMillis = timestampMillis,
        isGrouped = isGrouped,
        deliveryStatus = deliveryStatus
    )
}
