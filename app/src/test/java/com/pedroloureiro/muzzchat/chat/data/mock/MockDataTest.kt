package com.pedroloureiro.muzzchat.chat.data.mock

import androidx.test.core.app.ApplicationProvider
import com.pedroloureiro.muzzchat.chat.data.remote.model.MessageDto
import com.pedroloureiro.muzzchat.chat.data.toEntity
import com.pedroloureiro.muzzchat.chat.domain.model.Users
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MockDataTest {

    private lateinit var seed: List<MessageDto>

    @Before
    fun loadSeed() {
        seed = MockData(ApplicationProvider.getApplicationContext()).loadSeedMessages()
    }

    @Test
    fun `seed has messages from exactly both users`() {
        assertEquals(setOf(Users.ME.id, Users.SARAH.id), seed.map { it.senderId }.toSet())
    }

    @Test
    fun `seed has unique ids so reloading replaces rather than duplicates`() {
        assertEquals(seed.size, seed.map { it.messageId }.toSet().size)
    }

    @Test
    fun `seed is in ascending timestamp order`() {
        assertEquals(seed.sortedBy { it.timestamp }, seed)
    }

    @Test
    fun `seed has consecutive messages from the same sender within 20 seconds`() {
        assertTrue(seed.zipWithNext().any { (previous, next) ->
            previous.senderId == next.senderId && next.timestamp - previous.timestamp < 20_000
        })
    }

    @Test
    fun `seed has a gap of more than one hour`() {
        assertTrue(seed.zipWithNext().any { (previous, next) ->
            next.timestamp - previous.timestamp > 3_600_000
        })
    }

    @Test
    fun `every seed message maps to an entity`() {
        seed.forEach { it.toEntity() }
    }
}
