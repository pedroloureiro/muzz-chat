package com.pedroloureiro.muzzchat.chat.data.local.db.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pedroloureiro.muzzchat.chat.data.local.db.MessageDatabase
import com.pedroloureiro.muzzchat.chat.data.local.db.entity.MessageEntity
import com.pedroloureiro.muzzchat.chat.domain.model.ContentType
import com.pedroloureiro.muzzchat.chat.domain.model.MessageDeliveryStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MessageDaoTest {

    private lateinit var database: MessageDatabase
    private lateinit var dao: MessageDao

    @Before
    fun openDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            MessageDatabase::class.java
        ).build()
        dao = database.messageDao()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun `getMessages returns the newest 500 messages in ascending order`() = runBlocking {
        (600L downTo 1L).forEach { timestamp -> dao.insert(message("$timestamp", timestamp)) }

        val timestamps = dao.getMessages().first().map { it.timestampMillis }

        assertEquals((101L..600L).toList(), timestamps)
    }

    @Test
    fun `getMessages emits again after an insert`() = runBlocking {
        val emissions = Channel<List<MessageEntity>>(Channel.UNLIMITED)
        val collector = launch(Dispatchers.Default) {
            dao.getMessages().collect { emissions.send(it) }
        }
        val inserted = message("1", 1L)

        assertEquals(emptyList<MessageEntity>(), withTimeout(TIMEOUT_MILLIS) { emissions.receive() })
        dao.insert(inserted)

        assertEquals(listOf(inserted), withTimeout(TIMEOUT_MILLIS) { emissions.receive() })
        collector.cancel()
    }

    @Test
    fun `updateStatus changes only the targeted message`() = runBlocking {
        val target = message("target", 1L)
        val other = message("other", 2L)
        dao.insert(target)
        dao.insert(other)

        dao.updateStatus("target", MessageDeliveryStatus.SENT)

        assertEquals(
            listOf(target.copy(deliveryStatus = MessageDeliveryStatus.SENT), other),
            dao.getMessages().first()
        )
    }

    @Test
    fun `inserting an existing id replaces the message`() = runBlocking {
        val original = message("1", 1L)
        val replacement = original.copy(deliveryStatus = MessageDeliveryStatus.SENT)
        dao.insert(original)

        dao.insert(replacement)

        assertEquals(listOf(replacement), dao.getMessages().first())
    }

    @Test
    fun `clearAll removes every message`() = runBlocking {
        dao.insert(message("1", 1L))
        dao.insert(message("2", 2L))

        dao.clearAll()

        assertEquals(emptyList<MessageEntity>(), dao.getMessages().first())
    }

    private fun message(id: String, timestamp: Long) = MessageEntity(
        messageId = id,
        senderId = "sender",
        contentType = ContentType.TEXT,
        contentValue = "text",
        timestampMillis = timestamp,
        deliveryStatus = MessageDeliveryStatus.SENDING
    )

    private companion object {
        const val TIMEOUT_MILLIS = 5_000L
    }
}
