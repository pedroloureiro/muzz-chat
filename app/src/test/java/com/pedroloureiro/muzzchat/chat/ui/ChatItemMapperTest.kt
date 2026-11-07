package com.pedroloureiro.muzzchat.chat.ui

import com.pedroloureiro.muzzchat.chat.domain.model.Content
import com.pedroloureiro.muzzchat.chat.domain.model.ContentType
import com.pedroloureiro.muzzchat.chat.domain.model.Message
import com.pedroloureiro.muzzchat.chat.domain.model.MessageDeliveryStatus
import com.pedroloureiro.muzzchat.chat.domain.model.User
import com.pedroloureiro.muzzchat.chat.domain.model.Users
import com.pedroloureiro.muzzchat.chat.ui.model.ChatItem
import com.pedroloureiro.muzzchat.chat.ui.model.MessageDirection
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChatItemMapperTest {

    private val now = millis("2026-03-12T18:00:00Z")
    private val t0 = millis("2026-03-12T11:59:00Z")

    @Test
    fun toMessageItem_mapsAllFields() {
        assertEquals(
            ChatItem.Message(
                messageId = "1",
                direction = MessageDirection.OUTGOING,
                content = "text 1",
                timestampMillis = t0,
                isGrouped = false,
                deliveryStatus = MessageDeliveryStatus.SENT
            ),
            ChatItemMapper.toMessageItem(message("1", Users.ME, t0), newer = null)
        )
    }

    @Test
    fun grouping_sameUser_within20s() {
        assertGrouped(true, message("1", Users.ME, t0), newer = message("2", Users.ME, t0 + 19_999))
    }

    @Test
    fun grouping_sameUser_over20s() {
        assertGrouped(false, message("1", Users.ME, t0), newer = message("2", Users.ME, t0 + 20_000))
    }

    @Test
    fun grouping_differentUser() {
        assertGrouped(false, message("1", Users.ME, t0), newer = message("2", Users.SARAH, t0 + 1_000))
    }

    @Test
    fun grouping_newestMessage() {
        assertGrouped(false, message("1", Users.ME, t0), newer = null)
    }

    @Test
    fun direction_meSender() {
        assertEquals(MessageDirection.OUTGOING, item(Users.ME, t0).direction)
    }

    @Test
    fun direction_sarahSender() {
        assertEquals(MessageDirection.INCOMING, item(Users.SARAH, t0).direction)
    }

    @Test
    fun deliveryStatus_passthrough() {
        val item = ChatItemMapper.toMessageItem(message("1", Users.ME, t0, MessageDeliveryStatus.SENDING), newer = null)

        assertEquals(MessageDeliveryStatus.SENDING, item.deliveryStatus)
    }

    @Test
    fun sectionHeader_firstMessage() {
        assertEquals(ChatItem.TimestampHeader("11:59"), headerAbove(item(Users.ME, t0), older = null))
    }

    @Test
    fun sectionHeader_gapOver1h() {
        val header = headerAbove(item(Users.ME, t0 + 3_600_001), older = item(Users.ME, t0))

        assertEquals(ChatItem.TimestampHeader("12:59"), header)
    }

    @Test
    fun sectionHeader_gapUnder1h() {
        assertNull(headerAbove(item(Users.ME, t0 + 3_600_000), older = item(Users.ME, t0)))
    }

    @Test
    fun headerFormat_today_showsTime() {
        assertEquals("11:59", headerFor("2026-03-12T11:59:00Z"))
    }

    @Test
    fun headerFormat_yesterday_showsWeekday() {
        assertEquals("Wednesday 11:59", headerFor("2026-03-11T11:59:00Z"))
    }

    @Test
    fun headerFormat_sixDaysAgo_showsWeekday() {
        assertEquals("Friday 11:59", headerFor("2026-03-06T11:59:00Z"))
    }

    @Test
    fun headerFormat_sevenDaysAgo_showsFullDate() {
        assertEquals("5 Mar 2026 11:59", headerFor("2026-03-05T11:59:00Z"))
    }

    @Test
    fun headerFormat_laterDayThanNow_showsFullDate() {
        assertEquals(
            "13 Mar 2026 00:01",
            headerFor("2026-03-13T00:01:00Z", now = millis("2026-03-12T23:59:00Z"))
        )
    }

    @Test
    fun headerFormat_previousCalendarDayUnder24h_showsWeekday() {
        assertEquals(
            "Wednesday 23:00",
            headerFor("2026-03-11T23:00:00Z", now = millis("2026-03-12T00:30:00Z"))
        )
    }

    @Test
    fun headerFormat_calendarDayUsesZone() {
        assertEquals(
            "08:00",
            headerFor(
                "2026-03-11T23:00:00Z",
                now = millis("2026-03-12T00:30:00Z"),
                zoneId = ZoneId.of("Asia/Tokyo")
            )
        )
    }

    private fun assertGrouped(expected: Boolean, message: Message, newer: Message?) {
        assertEquals(expected, ChatItemMapper.toMessageItem(message, newer).isGrouped)
    }

    private fun item(sender: User, timestamp: Long) =
        ChatItemMapper.toMessageItem(message("1", sender, timestamp), newer = null)

    private fun headerAbove(item: ChatItem.Message, older: ChatItem.Message?, now: Long = this.now, zoneId: ZoneId = ZoneOffset.UTC) =
        ChatItemMapper.headerAbove(item, older, now, zoneId, Locale.UK)

    private fun headerFor(timestamp: String, now: Long = this.now, zoneId: ZoneId = ZoneOffset.UTC): String? =
        headerAbove(item(Users.ME, millis(timestamp)), older = null, now, zoneId)?.label

    private fun message(
        id: String,
        sender: User,
        timestamp: Long,
        status: MessageDeliveryStatus = MessageDeliveryStatus.SENT
    ) = Message(
        messageId = id,
        senderId = sender.id,
        content = Content(ContentType.TEXT, "text $id"),
        timestampMillis = timestamp,
        deliveryStatus = status
    )

    private fun millis(instant: String) = Instant.parse(instant).toEpochMilli()
}
