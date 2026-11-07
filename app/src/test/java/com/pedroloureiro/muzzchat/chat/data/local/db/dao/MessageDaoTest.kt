package com.pedroloureiro.muzzchat.chat.data.local.db.dao

import android.content.Context
import androidx.paging.PagingSource
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pedroloureiro.muzzchat.chat.data.local.db.MessageDatabase
import com.pedroloureiro.muzzchat.chat.data.local.db.entity.MessageEntity
import com.pedroloureiro.muzzchat.chat.data.local.db.entity.MessageWithNextRow
import com.pedroloureiro.muzzchat.chat.domain.model.ContentType
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
    fun `pagingSource returns pending messages newest first, then confirmed messages by id descending whatever their timestamps`() = runBlocking {
        dao.insertPending(pending("late", timestamp = 2L))
        dao.insertPending(pending("early", timestamp = 1L))
        dao.confirm(confirmed("b", timestamp = 10L))
        dao.confirm(confirmed("a", timestamp = 20L))

        assertEquals(listOf("late", "early", "b", "a"), storedKeys())
    }

    @Test
    fun `pagingSource gives a confirmed message the confirmed message with the next id`() = runBlocking {
        dao.confirm(confirmed("c", timestamp = 30L, sender = "third"))
        dao.confirm(confirmed("a", timestamp = 10L, sender = "first"))
        dao.confirm(confirmed("b", timestamp = 20L, sender = "second"))
        dao.insertPending(pending("local", timestamp = 1L))

        assertEquals(Next("second", 20L), nextOf("a"))
        assertEquals(Next("third", 30L), nextOf("b"))
    }

    @Test
    fun `pagingSource gives the newest confirmed message the oldest pending message`() = runBlocking {
        dao.confirm(confirmed("a", timestamp = 10L))
        dao.insertPending(pending("late", timestamp = 3L, sender = "late-sender"))
        dao.insertPending(pending("early", timestamp = 2L, sender = "early-sender"))

        assertEquals(Next("early-sender", 2L), nextOf("a"))
    }

    @Test
    fun `pagingSource gives the newest confirmed message no next message when none is pending`() = runBlocking {
        dao.confirm(confirmed("a", timestamp = 10L))
        dao.confirm(confirmed("b", timestamp = 20L))

        assertEquals(Next(null, null), nextOf("b"))
    }

    @Test
    fun `pagingSource gives a pending message the next pending message by timestamp`() = runBlocking {
        dao.insertPending(pending("third", timestamp = 3L, sender = "third-sender"))
        dao.insertPending(pending("first", timestamp = 1L, sender = "first-sender"))
        dao.insertPending(pending("second", timestamp = 2L, sender = "second-sender"))

        assertEquals(Next("second-sender", 2L), nextOf("first"))
        assertEquals(Next("third-sender", 3L), nextOf("second"))
        assertEquals(Next(null, null), nextOf("third"))
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
        rejectInsertsOf(id = "server")

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

        assertEquals(listOf(null, "other-device"), stored().map { it.id })
    }

    @Test
    fun `confirm replaces a confirmed message with the same id`() = runBlocking {
        dao.confirm(confirmed("server", timestamp = 1L))

        dao.confirm(confirmed("server", timestamp = 2L))

        assertEquals(listOf(confirmed("server", timestamp = 2L)), stored())
    }

    @Test
    fun `confirmAll replaces the pending message each echo carries the localId of`() = runBlocking {
        dao.insertPending(pending("local", timestamp = 1L))

        dao.confirmAll(listOf(confirmed("b", timestamp = 2L, localId = "local"), confirmed("a", timestamp = 1L)))

        assertEquals(listOf(confirmed("b", timestamp = 2L, localId = "local"), confirmed("a", timestamp = 1L)), stored())
    }

    @Test
    fun `confirmAll is one transaction, so a failed insert stores none of the page`() = runBlocking {
        rejectInsertsOf(id = "b")

        runCatching { dao.confirmAll(listOf(confirmed("a", timestamp = 1L), confirmed("b", timestamp = 2L))) }

        assertEquals(emptyList<MessageEntity>(), stored())
    }

    @Test
    fun `oldestId returns the smallest confirmed id, ignoring pending messages`() = runBlocking {
        dao.insertPending(pending("local", timestamp = 0L))
        dao.confirm(confirmed("b", timestamp = 1L))
        dao.confirm(confirmed("a", timestamp = 2L))

        assertEquals("a", dao.oldestId())
    }

    @Test
    fun `oldestId is null when no confirmed message is stored`() = runBlocking {
        dao.insertPending(pending("local", timestamp = 0L))

        assertNull(dao.oldestId())
    }

    @Test
    fun `clearAll removes every message`() = runBlocking {
        dao.insertPending(pending("local", timestamp = 1L))
        dao.confirm(confirmed("server", timestamp = 2L))

        dao.clearAll()

        assertEquals(emptyList<MessageEntity>(), stored())
    }

    private data class Next(val senderId: String?, val timestampMillis: Long?)

    private suspend fun rows(): List<MessageWithNextRow> {
        val params = PagingSource.LoadParams.Refresh<Int>(key = null, loadSize = 100, placeholdersEnabled = false)
        return (dao.pagingSource().load(params) as PagingSource.LoadResult.Page).data
    }

    private suspend fun stored() = rows().map { it.message.copy(rowId = 0) }

    private suspend fun storedKeys() = rows().map { it.message.id ?: it.message.localId }

    private suspend fun nextOf(key: String) = rows()
        .single { (it.message.id ?: it.message.localId) == key }
        .let { Next(it.nextSenderId, it.nextTimestampMillis) }

    private fun rejectInsertsOf(id: String) {
        database.openHelper.writableDatabase.execSQL(
            "CREATE TRIGGER reject_$id BEFORE INSERT ON messages WHEN NEW.id = '$id' " +
                "BEGIN SELECT RAISE(ABORT, 'rejected'); END"
        )
    }

    private fun pending(localId: String, timestamp: Long, sender: String = "sender") =
        message(id = null, localId = localId, timestamp, sender)

    private fun confirmed(id: String, timestamp: Long, localId: String? = null, sender: String = "sender") =
        message(id, localId, timestamp, sender)

    private fun message(id: String?, localId: String?, timestamp: Long, sender: String) = MessageEntity(
        id = id,
        localId = localId,
        senderId = sender,
        contentType = ContentType.TEXT,
        contentValue = "text",
        timestampMillis = timestamp
    )
}
