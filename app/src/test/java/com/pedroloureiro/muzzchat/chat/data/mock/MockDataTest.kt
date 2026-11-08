package com.pedroloureiro.muzzchat.chat.data.mock

import androidx.test.core.app.ApplicationProvider
import com.pedroloureiro.muzzchat.chat.data.remote.model.ContentDto
import com.pedroloureiro.muzzchat.chat.data.remote.model.MessageDto
import com.pedroloureiro.muzzchat.chat.data.remote.model.MessagePageDto
import com.pedroloureiro.muzzchat.chat.data.remote.model.OutgoingMessageDto
import com.pedroloureiro.muzzchat.chat.data.toEntity
import com.pedroloureiro.muzzchat.chat.domain.model.Users
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MockDataTest {

    private val mockData = MockData(ApplicationProvider.getApplicationContext())
    private lateinit var seed: List<MessageDto>

    @Before
    fun loadSeed() {
        seed = mockData.loadSeedMessages()
    }

    @Test
    fun `seed has messages from exactly both users`() {
        assertEquals(setOf(Users.ME.id, Users.SARAH.id), seed.map { it.senderId }.toSet())
    }

    @Test
    fun `seed is all in the mock chat`() {
        assertEquals(setOf(MOCK_SARAH_CHAT_ID), seed.map { it.chatId }.toSet())
    }

    @Test
    fun `seed has unique UUID ids`() {
        seed.forEach { UUID.fromString(it.id) }
        assertEquals(seed.size, seed.map { it.id }.toSet().size)
    }

    @Test
    fun `seed seqs run from 1 with no holes, in timestamp order`() {
        assertEquals((1L..seed.size).toList(), seed.sortedBy { it.timestampMillis }.map { it.seq })
    }

    @Test
    fun `seed has consecutive messages from the same sender within 20 seconds`() {
        assertTrue(seed.zipWithNext().any { (previous, next) ->
            previous.senderId == next.senderId && next.timestampMillis - previous.timestampMillis < 20_000
        })
    }

    @Test
    fun `seed has a gap of more than one hour`() {
        assertTrue(seed.zipWithNext().any { (previous, next) ->
            next.timestampMillis - previous.timestampMillis > 3_600_000
        })
    }

    @Test
    fun `seed fills several pages`() {
        assertTrue(seed.size >= 200)
    }

    @Test
    fun `every seed message maps to an entity`() {
        seed.forEach { it.toEntity() }
    }

    @Test
    fun `store gives the message a UUID id, the chat's next seq and the server time, and keeps its localId`() {
        val before = System.currentTimeMillis()

        val stored = mockData.store(MOCK_SARAH_CHAT_ID, outgoing("local"))

        UUID.fromString(stored.id)
        assertEquals(seed.size + 1L, stored.seq)
        assertEquals(MOCK_SARAH_CHAT_ID, stored.chatId)
        assertTrue(stored.timestampMillis in before..System.currentTimeMillis())
        assertEquals("local", stored.localId)
        assertEquals(ContentDto(type = "TEXT", value = "text"), stored.content)
    }

    @Test
    fun `store numbers each chat on its own`() {
        val first = mockData.store(OTHER_CHAT_ID, outgoing("a"))
        val second = mockData.store(OTHER_CHAT_ID, outgoing("b"))

        assertEquals(listOf(1L, 2L), listOf(first.seq, second.seq))
    }

    @Test
    fun `store recognises a resend by its localId and returns the stored message`() {
        val first = mockData.store(MOCK_SARAH_CHAT_ID, outgoing("local"))

        val resent = mockData.store(MOCK_SARAH_CHAT_ID, outgoing("local"))

        assertEquals(first, resent)
        assertEquals(seed + first, mockData.messages())
    }

    @Test
    fun `pageBefore without a cursor returns the newest messages, newest first, continuing from the oldest`() {
        val newestFirst = seed.sortedByDescending { it.seq }.take(3)

        assertEquals(
            MessagePageDto(newestFirst, nextCursor = newestFirst.last().seq, hasMore = true),
            mockData.pageBefore(MOCK_SARAH_CHAT_ID, beforeSeq = null, limit = 3)
        )
    }

    @Test
    fun `pageBefore returns the messages before the cursor, newest first`() {
        val page = mockData.pageBefore(MOCK_SARAH_CHAT_ID, beforeSeq = 10, limit = 3)

        assertEquals(listOf(9L, 8L, 7L), page.messages.map { it.seq })
        assertEquals(7L, page.nextCursor)
        assertTrue(page.hasMore)
    }

    @Test
    fun `pageBefore reports no more once it reaches seq 1`() {
        val page = mockData.pageBefore(MOCK_SARAH_CHAT_ID, beforeSeq = 4, limit = 3)

        assertEquals(MessagePageDto(page.messages, nextCursor = 1, hasMore = false), page)
    }

    @Test
    fun `pageAfter returns the messages after the cursor, oldest first, continuing from the newest`() {
        val page = mockData.pageAfter(MOCK_SARAH_CHAT_ID, afterSeq = 10, limit = 3)

        assertEquals(listOf(11L, 12L, 13L), page.messages.map { it.seq })
        assertEquals(13L, page.nextCursor)
        assertTrue(page.hasMore)
    }

    @Test
    fun `pageAfter reports no more once it reaches the newest message`() {
        val page = mockData.pageAfter(MOCK_SARAH_CHAT_ID, afterSeq = seed.size - 2L, limit = 3)

        assertEquals(listOf(seed.size - 1L, seed.size.toLong()), page.messages.map { it.seq })
        assertEquals(MessagePageDto(page.messages, nextCursor = seed.size.toLong(), hasMore = false), page)
    }

    @Test
    fun `pages of another chat do not include the mock chat's messages`() {
        assertEquals(
            MessagePageDto(emptyList(), nextCursor = null, hasMore = false),
            mockData.pageBefore(OTHER_CHAT_ID, beforeSeq = null, limit = 3)
        )
    }

    @Test
    fun `paging before by seq returns every message once, with no repeats and no gaps`() {
        val stored = mockData.store(MOCK_SARAH_CHAT_ID, outgoing("local"))
        val pages = generateSequence(mockData.pageBefore(MOCK_SARAH_CHAT_ID, beforeSeq = null, limit = 20)) { previous ->
            if (!previous.hasMore) null else mockData.pageBefore(MOCK_SARAH_CHAT_ID, previous.nextCursor, limit = 20)
        }.toList()

        assertEquals((seed + stored).sortedByDescending { it.seq }, pages.flatMap { it.messages })
    }

    private fun outgoing(localId: String) = OutgoingMessageDto(
        localId = localId,
        senderId = Users.ME.id,
        content = ContentDto(type = "TEXT", value = "text"),
        timestampMillis = 1L
    )

    private companion object {
        const val OTHER_CHAT_ID = "other-chat"
    }
}
