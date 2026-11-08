package com.pedroloureiro.muzzchat.chat.ui

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.testing.asPagingSourceFactory
import androidx.paging.testing.asSnapshot
import com.pedroloureiro.muzzchat.chat.domain.model.Content
import com.pedroloureiro.muzzchat.chat.domain.model.ContentType
import com.pedroloureiro.muzzchat.chat.domain.model.Message
import com.pedroloureiro.muzzchat.chat.domain.model.MessageDeliveryStatus
import com.pedroloureiro.muzzchat.chat.domain.model.MessageWithNext
import com.pedroloureiro.muzzchat.chat.domain.model.User
import com.pedroloureiro.muzzchat.chat.domain.model.Users
import com.pedroloureiro.muzzchat.chat.ui.model.ChatItem
import com.pedroloureiro.muzzchat.chat.ui.model.MessageDirection
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Locale
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatItemMapperTest {

    private val now = millis("2026-03-12T18:00:00Z")
    private val t0 = millis("2026-03-12T11:59:00Z")
    private val alice = User("alice", "Alice")
    private val bob = User("bob", "Bob")

    @Test
    fun toMessageItem_mapsAllFields() {
        assertEquals(
            ChatItem.Message(
                key = "alice/1",
                direction = MessageDirection.INCOMING,
                content = "text 1",
                timestampMillis = t0,
                isGrouped = false,
                deliveryStatus = MessageDeliveryStatus.SENT
            ),
            toItem(message("1", alice, t0))
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
    fun deliveryStatus_pendingIsSending() {
        assertEquals(MessageDeliveryStatus.SENDING, toItem(pending("local")).deliveryStatus)
    }

    @Test
    fun deliveryStatus_confirmedIsSent() {
        assertEquals(MessageDeliveryStatus.SENT, toItem(message("1", Users.ME, t0)).deliveryStatus)
    }

    @Test
    fun key_pendingIsSenderAndClientMessageId() {
        assertEquals("alice/local", toItem(pending("local", alice)).key)
    }

    @Test
    fun key_confirmedIsSenderAndClientMessageId() {
        assertEquals("alice/1", toItem(message("1", alice, t0)).key)
    }

    @Test
    fun key_unchangedWhenPendingIsConfirmed() {
        assertEquals(toItem(pending("local", alice)).key, toItem(message("local", alice, t0)).key)
    }

    @Test
    fun key_differsBetweenSendersOfTheSameClientMessageId() {
        assertNotEquals(toItem(message("1", alice, t0)).key, toItem(message("1", bob, t0)).key)
    }

    @Test
    fun sectionHeader_firstMessage() {
        assertEquals(ChatItem.TimestampHeader("11:59", key = "header-alice/1"), headerAbove(item(alice, t0), older = null))
    }

    @Test
    fun sectionHeader_gapOver1h() {
        val header = headerAbove(item(alice, t0 + 3_600_001), older = item(alice, t0))

        assertEquals(ChatItem.TimestampHeader("12:59", key = "header-alice/1"), header)
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

    @Test
    fun sectionHeader_keyFollowsTheMessageBelow() {
        assertEquals("header-alice/local", headerAbove(toItem(pending("local", alice)), older = null)?.key)
    }

    @Test
    fun paging_headerAcrossPageBoundary() = runTest {
        val newestFirst = listOf(
            message("4", alice, t0 + 7_210_000),
            message("3", alice, t0 + 7_200_000),
            message("2", bob, t0),
            message("1", bob, t0 - 10_000)
        )

        val items = pagedChatItems(newestFirst).asSnapshot { appendScrollWhile { true } }

        assertEquals(
            listOf(
                toItem(rowsOf(newestFirst)[0]),
                toItem(rowsOf(newestFirst)[1]),
                ChatItem.TimestampHeader("13:59", key = "header-alice/3"),
                toItem(rowsOf(newestFirst)[2]),
                toItem(rowsOf(newestFirst)[3]),
                ChatItem.TimestampHeader("11:58", key = "header-bob/1")
            ),
            items
        )
    }

    @Test
    fun paging_oldestMessageHeaderOnlyOnceLoaded() = runTest {
        val newestFirst = listOf(
            message("3", alice, t0 + 2_000),
            message("2", alice, t0 + 1_000),
            message("1", alice, t0)
        )
        val pagedItems = pagedChatItems(newestFirst)

        assertTrue(pagedItems.asSnapshot().none { it is ChatItem.TimestampHeader })
        assertEquals(
            ChatItem.TimestampHeader("11:59", key = "header-alice/1"),
            pagedItems.asSnapshot { appendScrollWhile { true } }.last()
        )
    }

    @Test
    fun paging_groupingAcrossPageBoundary() = runTest {
        val newestFirst = listOf(
            message("2", Users.ME, t0 + 1_000),
            message("1", Users.ME, t0)
        )

        val items = pagedChatItems(newestFirst).asSnapshot { appendScrollWhile { true } }

        assertEquals(listOf(false, true), items.filterIsInstance<ChatItem.Message>().map { it.isGrouped })
    }

    private fun assertGrouped(expected: Boolean, message: Message, newer: Message?) {
        assertEquals(expected, ChatItemMapper.toMessageItem(row(message, newer)).isGrouped)
    }

    private fun item(sender: User, timestamp: Long) = toItem(message("1", sender, timestamp))

    private fun headerAbove(item: ChatItem.Message, older: ChatItem.Message?, now: Long = this.now, zoneId: ZoneId = ZoneOffset.UTC) =
        ChatItemMapper.headerAbove(item, older, now, zoneId, Locale.UK)

    private fun headerFor(timestamp: String, now: Long = this.now, zoneId: ZoneId = ZoneOffset.UTC): String? =
        headerAbove(item(Users.ME, millis(timestamp)), older = null, now, zoneId)?.label

    private fun toItem(message: Message) = ChatItemMapper.toMessageItem(row(message, newer = null))

    private fun toItem(row: MessageWithNext) = ChatItemMapper.toMessageItem(row)

    private fun row(message: Message, newer: Message?) =
        MessageWithNext(message, nextSenderId = newer?.senderId, nextTimestampMillis = newer?.timestampMillis)

    private fun rowsOf(newestFirst: List<Message>) =
        newestFirst.mapIndexed { index, message -> row(message, newer = newestFirst.getOrNull(index - 1)) }

    // One message per page, so every pair of neighbours straddles a page boundary.
    private fun pagedChatItems(newestFirst: List<Message>) =
        Pager(PagingConfig(pageSize = 1, prefetchDistance = 1, initialLoadSize = 1, enablePlaceholders = false)) {
            rowsOf(newestFirst).asPagingSourceFactory().invoke()
        }.flow.map { it.toChatItems(now, ZoneOffset.UTC, Locale.UK) }

    private fun message(clientMessageId: String, sender: User, timestamp: Long) = Message.Confirmed(
        clientMessageId = clientMessageId,
        serverMessageId = "server-$clientMessageId",
        seq = 1L,
        chatId = "chat",
        senderId = sender.id,
        content = Content(ContentType.TEXT, "text $clientMessageId"),
        timestampMillis = timestamp
    )

    private fun pending(clientMessageId: String, sender: User = Users.ME) = Message.Pending(
        clientMessageId = clientMessageId,
        chatId = "chat",
        senderId = sender.id,
        content = Content(ContentType.TEXT, "text"),
        timestampMillis = t0
    )

    private fun millis(instant: String) = Instant.parse(instant).toEpochMilli()
}
