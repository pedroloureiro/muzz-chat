package com.pedroloureiro.muzzchat.chat.data.local.db.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pedroloureiro.muzzchat.chat.data.local.db.MessageDatabase
import com.pedroloureiro.muzzchat.chat.data.local.db.entity.MessageEntity
import com.pedroloureiro.muzzchat.chat.domain.model.ContentType
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
    fun `getMessages orders confirmed messages by id whatever their timestamps, then pending messages by timestamp`() = runBlocking {
        dao.insertPending(pending("late", timestamp = 2L))
        dao.insertPending(pending("early", timestamp = 1L))
        dao.confirm(confirmed("b", timestamp = 10L))
        dao.confirm(confirmed("a", timestamp = 20L))

        assertEquals(listOf("a", "b", "early", "late"), storedKeys())
    }

    @Test
    fun `getMessages returns the newest 500 messages in ascending order`() = runBlocking {
        (600 downTo 1).forEach { dao.confirm(confirmed("%03d".format(it), timestamp = 0L)) }

        assertEquals((101..600).map { "%03d".format(it) }, storedKeys())
    }

    @Test
    fun `getMessages emits again after an insert`() = runBlocking {
        val emissions = Channel<List<MessageEntity>>(Channel.UNLIMITED)
        val collector = launch(Dispatchers.Default) {
            dao.getMessages().collect { emissions.send(it) }
        }

        assertEquals(emptyList<MessageEntity>(), withTimeout(TIMEOUT_MILLIS) { emissions.receive() })
        dao.insertPending(pending("local", timestamp = 1L))

        assertEquals(listOf("local"), withTimeout(TIMEOUT_MILLIS) { emissions.receive() }.map { it.localId })
        collector.cancel()
    }

    @Test
    fun `confirm replaces the pending message with the same localId`() = runBlocking {
        dao.insertPending(pending("local", timestamp = 1L))

        dao.confirm(confirmed("server", timestamp = 2L, localId = "local"))

        assertEquals(listOf(confirmed("server", timestamp = 2L, localId = "local")), stored())
    }

    @Test
    fun `confirm is one transaction, so a failed insert keeps the pending message`() = runBlocking {
        dao.insertPending(pending("local", timestamp = 1L))
        database.openHelper.writableDatabase.execSQL(
            "CREATE TRIGGER reject_confirmed BEFORE INSERT ON messages WHEN NEW.id IS NOT NULL " +
                "BEGIN SELECT RAISE(ABORT, 'rejected'); END"
        )

        runCatching { dao.confirm(confirmed("server", timestamp = 2L, localId = "local")) }

        assertEquals(listOf(pending("local", timestamp = 1L)), stored())
    }

    @Test
    fun `an echo that arrives twice leaves one confirmed message`() = runBlocking {
        val echo = confirmed("server", timestamp = 2L, localId = "local")
        dao.insertPending(pending("local", timestamp = 1L))

        dao.confirm(echo)
        dao.confirm(echo)

        assertEquals(listOf(echo), stored())
    }

    @Test
    fun `confirm inserts a server message whose localId is not stored`() = runBlocking {
        dao.insertPending(pending("mine", timestamp = 1L))

        dao.confirm(confirmed("other-device", timestamp = 2L, localId = "theirs"))

        assertEquals(listOf("other-device", null), stored().map { it.id })
    }

    @Test
    fun `confirm replaces a confirmed message with the same id`() = runBlocking {
        dao.confirm(confirmed("server", timestamp = 1L))

        dao.confirm(confirmed("server", timestamp = 2L))

        assertEquals(listOf(confirmed("server", timestamp = 2L)), stored())
    }

    @Test
    fun `clearAll removes every message`() = runBlocking {
        dao.insertPending(pending("local", timestamp = 1L))
        dao.confirm(confirmed("server", timestamp = 2L))

        dao.clearAll()

        assertEquals(emptyList<MessageEntity>(), stored())
    }

    private suspend fun stored() = dao.getMessages().first().map { it.copy(rowId = 0) }

    private suspend fun storedKeys() = dao.getMessages().first().map { it.id ?: it.localId }

    private fun pending(localId: String, timestamp: Long) = message(id = null, localId = localId, timestamp)

    private fun confirmed(id: String, timestamp: Long, localId: String? = null) = message(id, localId, timestamp)

    private fun message(id: String?, localId: String?, timestamp: Long) = MessageEntity(
        id = id,
        localId = localId,
        senderId = "sender",
        contentType = ContentType.TEXT,
        contentValue = "text",
        timestampMillis = timestamp
    )

    private companion object {
        const val TIMEOUT_MILLIS = 5_000L
    }
}
