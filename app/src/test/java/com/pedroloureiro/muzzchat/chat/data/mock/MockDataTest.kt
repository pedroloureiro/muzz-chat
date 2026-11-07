package com.pedroloureiro.muzzchat.chat.data.mock

import androidx.test.core.app.ApplicationProvider
import com.pedroloureiro.muzzchat.chat.data.remote.model.ContentDto
import com.pedroloureiro.muzzchat.chat.data.remote.model.MessageDto
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
    fun `seed has unique ids so reloading replaces rather than duplicates`() {
        assertEquals(seed.size, seed.map { it.id }.toSet().size)
    }

    @Test
    fun `seed ids are lowercase UUIDv7s matching their timestamps`() {
        seed.forEach {
            assertUuidV7(it.id)
            assertEquals(it.timestampMillis, uuidMillis(it.id))
        }
    }

    @Test
    fun `seed ids sort in timestamp order`() {
        assertEquals(seed.sortedBy { it.timestampMillis }, seed.sortedBy { it.id })
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
    fun `every seed message maps to an entity`() {
        seed.forEach { it.toEntity() }
    }

    @Test
    fun `store gives the message a UUIDv7 id and the server time, and keeps its localId`() {
        val before = System.currentTimeMillis()

        val stored = mockData.store(outgoing("local", timestampMillis = 1L))

        assertUuidV7(stored.id)
        assertTrue(stored.timestampMillis in before..System.currentTimeMillis())
        assertEquals("local", stored.localId)
        assertEquals(ContentDto(type = "TEXT", value = "text"), stored.content)
    }

    @Test
    fun `store generates increasing ids, including within the same millisecond`() {
        val ids = (1..1_000).map { mockData.store(outgoing("local-$it")).id }

        assertTrue(ids.groupBy(::uuidMillis).values.any { it.size > 1 })
        assertEquals(ids.sorted(), ids)
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `store recognises a resend by its localId and returns the stored message`() {
        val first = mockData.store(outgoing("local"))

        val resent = mockData.store(outgoing("local"))

        assertEquals(first, resent)
        assertEquals(seed + first, mockData.messages())
    }

    @Test
    fun `messages has the seed followed by stored messages`() {
        val stored = mockData.store(outgoing("local"))

        assertEquals(seed + stored, mockData.messages())
    }

    @Test
    fun `seed fills several pages`() {
        assertTrue(seed.size >= 200)
    }

    @Test
    fun `page without a cursor returns the newest messages, newest first`() {
        assertEquals(seed.sortedByDescending { it.id }.take(3), mockData.page(beforeId = null, limit = 3))
    }

    @Test
    fun `page returns the messages before the cursor, newest first`() {
        val newestFirst = seed.sortedByDescending { it.id }

        assertEquals(newestFirst.subList(5, 8), mockData.page(beforeId = newestFirst[4].id, limit = 3))
    }

    @Test
    fun `paging by id returns every message once, with no repeats and no gaps`() {
        val stored = mockData.store(outgoing("local"))
        val pages = generateSequence(mockData.page(beforeId = null, limit = 20)) { previous ->
            if (previous.size < 20) null else mockData.page(beforeId = previous.last().id, limit = 20)
        }.toList()

        assertEquals((seed + stored).sortedByDescending { it.id }, pages.flatten())
    }

    private fun outgoing(localId: String, timestampMillis: Long = 1L) = OutgoingMessageDto(
        localId = localId,
        senderId = Users.ME.id,
        content = ContentDto(type = "TEXT", value = "text"),
        timestampMillis = timestampMillis
    )

    private fun assertUuidV7(id: String) {
        assertEquals(id.lowercase(), id)
        assertEquals(7, UUID.fromString(id).version())
    }

    private fun uuidMillis(id: String) = UUID.fromString(id).mostSignificantBits ushr 16
}
