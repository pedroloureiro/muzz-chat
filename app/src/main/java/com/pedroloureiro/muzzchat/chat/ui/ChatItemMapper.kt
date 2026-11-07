package com.pedroloureiro.muzzchat.chat.ui

import com.pedroloureiro.muzzchat.chat.domain.model.Message
import com.pedroloureiro.muzzchat.chat.domain.model.Users
import com.pedroloureiro.muzzchat.chat.ui.model.ChatItem
import com.pedroloureiro.muzzchat.chat.ui.model.MessageDirection
import com.pedroloureiro.muzzchat.utils.TimestampFormat
import com.pedroloureiro.muzzchat.utils.formatUtc
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Locale

private const val SECTION_GAP_MS = 3_600_000L
private const val GROUPING_WINDOW_MS = 20_000L

class ChatItemMapper {

    // Zone and locale are read on every call so a change on the device applies at the next mapping.
    fun map(
        messages: List<Message>,
        now: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault(),
        locale: Locale = Locale.getDefault()
    ): List<ChatItem> {
        val sorted = messages.sortedBy { it.timestamp }
        return sorted.flatMapIndexed { index, message ->
            val previous = sorted.getOrNull(index - 1)
            val next = sorted.getOrNull(index + 1)
            buildList {
                if (previous == null || message.timestamp - previous.timestamp > SECTION_GAP_MS) {
                    val format = headerFormat(message.timestamp, now, zoneId)
                    add(ChatItem.TimestampHeader(formatUtc(message.timestamp, zoneId, locale, format)))
                }
                add(message.toChatItem(isGrouped = next != null && isGrouped(message, next)))
            }
        }
    }

    private fun isGrouped(message: Message, next: Message) =
        next.senderId == message.senderId && next.timestamp - message.timestamp < GROUPING_WINDOW_MS

    private fun headerFormat(timestamp: Long, now: Long, zoneId: ZoneId): TimestampFormat {
        val daysAgo = ChronoUnit.DAYS.between(localDate(timestamp, zoneId), localDate(now, zoneId))
        return when (daysAgo) {
            0L -> TimestampFormat.HOUR_MIN
            in 1L..6L -> TimestampFormat.DAY_OF_WEEK_HOUR_MIN
            else -> TimestampFormat.DAY_ABR_MONTH_YEAR_HOUR_MIN
        }
    }

    private fun localDate(utcMillis: Long, zoneId: ZoneId) =
        Instant.ofEpochMilli(utcMillis).atZone(zoneId).toLocalDate()

    private fun Message.toChatItem(isGrouped: Boolean) = ChatItem.Message(
        messageId = messageId,
        direction = if (senderId == Users.ME.id) MessageDirection.OUTGOING else MessageDirection.INCOMING,
        content = content.value,
        timestamp = timestamp,
        isGrouped = isGrouped,
        deliveryStatus = deliveryStatus
    )
}
