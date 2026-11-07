package com.pedroloureiro.muzzchat.chat.ui

import androidx.paging.PagingData
import androidx.paging.insertSeparators
import androidx.paging.map
import com.pedroloureiro.muzzchat.chat.domain.model.Message
import com.pedroloureiro.muzzchat.chat.domain.model.MessageDeliveryStatus
import com.pedroloureiro.muzzchat.chat.domain.model.MessageWithNext
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

// Newest first, so insertSeparators sees each message before the older one it follows.
fun PagingData<MessageWithNext>.toChatItems(nowMillis: Long, zoneId: ZoneId, locale: Locale): PagingData<ChatItem> =
    map { ChatItemMapper.toMessageItem(it) }
        .insertSeparators { newer: ChatItem.Message?, older: ChatItem.Message? ->
            newer?.let { ChatItemMapper.headerAbove(it, older, nowMillis, zoneId, locale) }
        }

object ChatItemMapper {

    fun toMessageItem(row: MessageWithNext): ChatItem.Message {
        val message = row.message
        return ChatItem.Message(
            messageId = when (message) {
                is Message.Pending -> message.localId
                is Message.Confirmed -> message.localId ?: message.id
            },
            direction = if (message.senderId == Users.ME.id) MessageDirection.OUTGOING else MessageDirection.INCOMING,
            content = message.content.value,
            timestampMillis = message.timestampMillis,
            isGrouped = isGrouped(message, row.nextSenderId, row.nextTimestampMillis),
            deliveryStatus = when (message) {
                is Message.Pending -> MessageDeliveryStatus.SENDING
                is Message.Confirmed -> MessageDeliveryStatus.SENT
            }
            )
    }

    fun headerAbove(
        message: ChatItem.Message,
        older: ChatItem.Message?,
        nowMillis: Long,
        zoneId: ZoneId,
        locale: Locale
    ): ChatItem.TimestampHeader? {
        if (older != null && message.timestampMillis - older.timestampMillis <= SECTION_GAP_MILLIS) return null
        val format = headerFormat(message.timestampMillis, nowMillis, zoneId)
        return ChatItem.TimestampHeader(
            label = formatUtc(message.timestampMillis, zoneId, locale, format),
            key = "header-${message.messageId}"
        )
    }

    private fun isGrouped(message: Message, nextSenderId: String?, nextTimestampMillis: Long?) =
        nextSenderId == message.senderId &&
            nextTimestampMillis != null &&
            nextTimestampMillis - message.timestampMillis < GROUPING_WINDOW_MILLIS

    private fun headerFormat(timestampMillis: Long, nowMillis: Long, zoneId: ZoneId): DateTimeFormat {
        val daysAgo = calendarDaysBetween(timestampMillis, nowMillis, zoneId)
        return when {
            daysAgo == TODAY -> DateTimeFormat.HOUR_MIN
            daysAgo in WITHIN_A_WEEK -> DateTimeFormat.DAY_OF_WEEK_HOUR_MIN
            else -> DateTimeFormat.DAY_ABR_MONTH_YEAR_HOUR_MIN
        }
    }
}
