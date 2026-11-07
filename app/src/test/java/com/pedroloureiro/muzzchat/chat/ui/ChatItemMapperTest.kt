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
import org.junit.Test

class ChatItemMapperTest {

    private val now = millis("2026-03-12T18:00:00Z")
    private val t0 = millis("2026-03-12T11:59:00Z")

    @Test
    fun grouping_sameUser_within20s() {
        val items = map(message("1", Users.ME, t0), message("2", Users.ME, t0 + 19_999))

        assertEquals(listOf(true, false), items.messages().map { it.isGrouped })
    }

    @Test
    fun grouping_sameUser_over20s() {
        val items = map(message("1", Users.ME, t0), message("2", Users.ME, t0 + 20_000))

        assertEquals(listOf(false, false), items.messages().map { it.isGrouped })
    }

    @Test
    fun grouping_differentUser() {
        val items = map(message("1", Users.ME, t0), message("2", Users.SARAH, t0 + 1_000))

        assertEquals(listOf(false, false), items.messages().map { it.isGrouped })
    }

    @Test
    fun sectionHeader_firstMessage() {
        val items = map(message("1", Users.ME, t0))

        assertEquals(
            listOf(
                ChatItem.TimestampHeader("11:59"),
                ChatItem.Message(
                    messageId = "1",
                    direction = MessageDirection.OUTGOING,
                    content = "text 1",
                    timestampMillis = t0,
                    isGrouped = false,
                    deliveryStatus = MessageDeliveryStatus.SENT
                )
            ),
            items
        )
    }

    @Test
    fun sectionHeader_gapOver1h() {
        val items = map(message("1", Users.ME, t0), message("2", Users.ME, t0 + 3_600_001))

        assertEquals(listOf("header", "1", "header", "2"), items.shape())
    }

    @Test
    fun sectionHeader_gapUnder1h() {
        val items = map(message("1", Users.ME, t0), message("2", Users.ME, t0 + 3_600_000))

        assertEquals(listOf("header", "1", "2"), items.shape())
    }

    @Test
    fun direction_meSender() {
        assertEquals(MessageDirection.OUTGOING, map(message("1", Users.ME, t0)).messages().single().direction)
    }

    @Test
    fun direction_sarahSender() {
        assertEquals(MessageDirection.INCOMING, map(message("1", Users.SARAH, t0)).messages().single().direction)
    }

    @Test
    fun deliveryStatus_passthrough() {
        val items = map(message("1", Users.ME, t0, MessageDeliveryStatus.SENDING))

        assertEquals(MessageDeliveryStatus.SENDING, items.messages().single().deliveryStatus)
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

    private fun map(vararg messages: Message) =
        ChatItemMapper.map(messages.toList(), now, ZoneOffset.UTC, Locale.UK)

    private fun headerFor(timestamp: String, now: Long = this.now, zoneId: ZoneId = ZoneOffset.UTC): String {
        val items = ChatItemMapper.map(listOf(message("1", Users.ME, millis(timestamp))), now, zoneId, Locale.UK)
        return (items.first() as ChatItem.TimestampHeader).label
    }

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

    private fun List<ChatItem>.messages() = filterIsInstance<ChatItem.Message>()

    private fun List<ChatItem>.shape() = map {
        when (it) {
            is ChatItem.TimestampHeader -> "header"
            is ChatItem.Message -> it.messageId
        }
    }
}
