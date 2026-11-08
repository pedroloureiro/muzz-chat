package com.pedroloureiro.muzzchat.chat.data.local.db.dao

import android.content.Context
import androidx.paging.PagingSource
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pedroloureiro.muzzchat.chat.data.local.db.MessageDatabase
import com.pedroloureiro.muzzchat.chat.data.local.db.entity.ChatSyncEntity
import com.pedroloureiro.muzzchat.chat.data.local.db.entity.MessageEntity
import com.pedroloureiro.muzzchat.chat.data.local.db.entity.MessageWithNextRow
import com.pedroloureiro.muzzchat.chat.domain.model.ContentType
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
    fun `pagingSource returns pending messages newest first, then confirmed messages by seq descending whatever their timestamps`() = runBlocking {
        dao.insertPending(pending("late", timestamp = 2L))
        dao.insertPending(pending("early", timestamp = 1L))
        dao.confirm(confirmed("a", seq = 2, timestamp = 10L))
        dao.confirm(confirmed("b", seq = 1, timestamp = 20L))

        assertEquals(listOf("late", "early", "a", "b"), storedKeys())
    }

    @Test
    fun `pagingSource returns only the given chat's messages`() = runBlocking {
        dao.confirm(confirmed("mine", seq = 1, timestamp = 1L))
        dao.confirm(confirmed("theirs", seq = 1, timestamp = 1L, chat = OTHER_CHAT))
        dao.insertPending(pending("their-pending", timestamp = 2L, chat = OTHER_CHAT))

        assertEquals(listOf("mine"), storedKeys())
    }

    @Test
    fun `pagingSource gives a confirmed message the stored confirmed message with the next seq`() = runBlocking {
        dao.confirm(confirmed("c", seq = 5, timestamp = 30L, sender = "third"))
        dao.confirm(confirmed("a", seq = 1, timestamp = 10L, sender = "first"))
        dao.confirm(confirmed("b", seq = 2, timestamp = 20L, sender = "second"))
        dao.insertPending(pending("local", timestamp = 1L))

        assertEquals(Next("second", 20L), nextOf("a"))
        assertEquals(Next("third", 30L), nextOf("b"))
    }

    @Test
    fun `pagingSource gives the newest confirmed message the oldest pending message`() = runBlocking {
        dao.confirm(confirmed("a", seq = 1, timestamp = 10L))
        dao.insertPending(pending("late", timestamp = 3L, sender = "late-sender"))
        dao.insertPending(pending("early", timestamp = 2L, sender = "early-sender"))

        assertEquals(Next("early-sender", 2L), nextOf("a"))
    }

    @Test
    fun `pagingSource gives the newest confirmed message no next message when none is pending`() = runBlocking {
        dao.confirm(confirmed("a", seq = 1, timestamp = 10L))
        dao.confirm(confirmed("b", seq = 2, timestamp = 20L))

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
    fun `pagingSource takes next messages only from the same chat`() = runBlocking {
        dao.confirm(confirmed("a", seq = 1, timestamp = 10L))
        dao.confirm(confirmed("theirs", seq = 2, timestamp = 20L, chat = OTHER_CHAT))
        dao.insertPending(pending("their-pending", timestamp = 30L, chat = OTHER_CHAT))

        assertEquals(Next(null, null), nextOf("a"))
    }

    @Test
    fun `confirm updates the pending message with the same sender and clientMessageId in place, with no delete`() = runBlocking {
        dao.insertPending(pending("local", timestamp = 1L))
        rejectDeletes()

        dao.confirm(confirmed("local", seq = 1, timestamp = 2L))

        assertEquals(listOf(confirmed("local", seq = 1, timestamp = 2L)), stored())
    }

    @Test
    fun `an echo that arrives twice leaves one confirmed message`() = runBlocking {
        val echo = confirmed("local", seq = 1, timestamp = 2L)
        dao.insertPending(pending("local", timestamp = 1L))

        dao.confirm(echo)
        dao.confirm(echo)

        assertEquals(listOf(echo), stored())
    }

    @Test
    fun `confirm inserts a server message no pending message has the key of`() = runBlocking {
        dao.insertPending(pending("mine", timestamp = 1L))

        dao.confirm(confirmed("theirs", seq = 1, timestamp = 2L))

        assertEquals(listOf("mine", "theirs"), storedKeys())
    }

    @Test
    fun `confirm overwrites a confirmed message with the same key`() = runBlocking {
        dao.confirm(confirmed("a", seq = 1, timestamp = 1L))

        dao.confirm(confirmed("a", seq = 1, timestamp = 2L))

        assertEquals(listOf(confirmed("a", seq = 1, timestamp = 2L)), stored())
    }

    @Test
    fun `the same clientMessageId from two senders is stored as two messages`() = runBlocking {
        dao.confirm(confirmed("same", seq = 1, timestamp = 1L, sender = "first"))
        dao.confirm(confirmed("same", seq = 2, timestamp = 2L, sender = "second"))

        assertEquals(listOf("second", "first"), stored().map { it.senderId })
    }

    @Test
    fun `a message stored from a page and then received as incoming leaves one row`() = runBlocking {
        val message = confirmed("a", seq = 1, timestamp = 1L)
        dao.storeNewestPage(CHAT, listOf(message), nextCursor = 1, hasMore = false)

        dao.storeIncoming(message)

        assertEquals(listOf(message), stored())
    }

    @Test
    fun `storeNewestPage writes the page and starts the chat's sync state`() = runBlocking {
        dao.insertPending(pending("local", timestamp = 1L))

        dao.storeNewestPage(
            CHAT,
            listOf(confirmed("local", seq = 9, timestamp = 2L), confirmed("a", seq = 8, timestamp = 1L)),
            nextCursor = 8,
            hasMore = true
        )

        assertEquals(listOf("local", "a"), storedKeys())
        assertEquals(ChatSyncEntity(CHAT, oldestSeq = 8, newestSeq = 9, reachedStart = false), dao.syncState(CHAT))
    }

    @Test
    fun `storeNewestPage of an empty chat starts at seq 0 with the start reached`() = runBlocking {
        dao.storeNewestPage(CHAT, emptyList(), nextCursor = null, hasMore = false)

        assertEquals(ChatSyncEntity(CHAT, oldestSeq = null, newestSeq = 0, reachedStart = true), dao.syncState(CHAT))
    }

    @Test
    fun `storeNewestPage is one transaction, so a failed insert stores neither the page nor the sync state`() = runBlocking {
        rejectInsertsOf(clientMessageId = "b")

        runCatching {
            dao.storeNewestPage(CHAT, listOf(confirmed("a", seq = 2, timestamp = 2L), confirmed("b", seq = 1, timestamp = 1L)), 1, false)
        }

        assertEquals(emptyList<MessageEntity>(), stored())
        assertNull(dao.syncState(CHAT))
    }

    @Test
    fun `storeOlderPage writes the page and moves oldestSeq and reachedStart, keeping newestSeq`() = runBlocking {
        dao.storeNewestPage(CHAT, listOf(confirmed("c", seq = 3, timestamp = 3L)), nextCursor = 3, hasMore = true)

        dao.storeOlderPage(CHAT, listOf(confirmed("b", seq = 2, timestamp = 2L), confirmed("a", seq = 1, timestamp = 1L)), nextCursor = 1, hasMore = false)

        assertEquals(listOf("c", "b", "a"), storedKeys())
        assertEquals(ChatSyncEntity(CHAT, oldestSeq = 1, newestSeq = 3, reachedStart = true), dao.syncState(CHAT))
    }

    @Test
    fun `storeOlderPage with no cursor keeps oldestSeq`() = runBlocking {
        dao.storeNewestPage(CHAT, listOf(confirmed("c", seq = 3, timestamp = 3L)), nextCursor = 3, hasMore = true)

        dao.storeOlderPage(CHAT, emptyList(), nextCursor = null, hasMore = false)

        assertEquals(ChatSyncEntity(CHAT, oldestSeq = 3, newestSeq = 3, reachedStart = true), dao.syncState(CHAT))
    }

    @Test
    fun `storeNewerPage writes the page and moves newestSeq to its cursor, keeping the older history`() = runBlocking {
        dao.storeNewestPage(CHAT, listOf(confirmed("a", seq = 1, timestamp = 1L)), nextCursor = 1, hasMore = false)

        dao.storeNewerPage(CHAT, listOf(confirmed("b", seq = 2, timestamp = 2L), confirmed("c", seq = 3, timestamp = 3L)), nextCursor = 3)

        assertEquals(listOf("c", "b", "a"), storedKeys())
        assertEquals(ChatSyncEntity(CHAT, oldestSeq = 1, newestSeq = 3, reachedStart = true), dao.syncState(CHAT))
    }

    @Test
    fun `storeNewerPage never moves newestSeq back`() = runBlocking {
        dao.storeNewestPage(CHAT, listOf(confirmed("c", seq = 3, timestamp = 3L)), nextCursor = 3, hasMore = true)

        dao.storeNewerPage(CHAT, listOf(confirmed("b", seq = 2, timestamp = 2L)), nextCursor = 2)

        assertEquals(3L, dao.syncState(CHAT)?.newestSeq)
    }

    @Test
    fun `storeIncoming with the next seq moves newestSeq and finds no gap`() = runBlocking {
        dao.storeNewestPage(CHAT, listOf(confirmed("a", seq = 1, timestamp = 1L)), nextCursor = 1, hasMore = false)

        val skipsSeq = dao.storeIncoming(confirmed("b", seq = 2, timestamp = 2L))

        assertFalse(skipsSeq)
        assertEquals(listOf("b", "a"), storedKeys())
        assertEquals(2L, dao.syncState(CHAT)?.newestSeq)
    }

    @Test
    fun `storeIncoming that skips a seq stores the message, keeps newestSeq and reports the gap`() = runBlocking {
        dao.storeNewestPage(CHAT, listOf(confirmed("a", seq = 1, timestamp = 1L)), nextCursor = 1, hasMore = false)

        val skipsSeq = dao.storeIncoming(confirmed("c", seq = 3, timestamp = 3L))

        assertTrue(skipsSeq)
        assertEquals(listOf("c", "a"), storedKeys())
        assertEquals(1L, dao.syncState(CHAT)?.newestSeq)
    }

    @Test
    fun `storeIncoming of a seq already reached changes nothing in the sync state`() = runBlocking {
        val echo = confirmed("b", seq = 2, timestamp = 2L)
        dao.storeNewestPage(CHAT, listOf(echo, confirmed("a", seq = 1, timestamp = 1L)), nextCursor = 1, hasMore = false)

        val skipsSeq = dao.storeIncoming(echo)

        assertFalse(skipsSeq)
        assertEquals(2L, dao.syncState(CHAT)?.newestSeq)
    }

    @Test
    fun `storeIncoming for a chat with no sync state stores the message and finds no gap`() = runBlocking {
        val skipsSeq = dao.storeIncoming(confirmed("c", seq = 3, timestamp = 3L))

        assertFalse(skipsSeq)
        assertEquals(listOf("c"), storedKeys())
        assertNull(dao.syncState(CHAT))
    }

    @Test
    fun `sync state is kept per chat`() = runBlocking {
        dao.storeNewestPage(CHAT, emptyList(), nextCursor = null, hasMore = false)

        assertNull(dao.syncState(OTHER_CHAT))
    }

    private data class Next(val senderId: String?, val timestampMillis: Long?)

    private suspend fun rows(): List<MessageWithNextRow> {
        val params = PagingSource.LoadParams.Refresh<Int>(key = null, loadSize = 100, placeholdersEnabled = false)
        return (dao.pagingSource(CHAT).load(params) as PagingSource.LoadResult.Page).data
    }

    private suspend fun stored() = rows().map { it.message }

    private suspend fun storedKeys() = rows().map { it.message.clientMessageId }

    private suspend fun nextOf(clientMessageId: String) = rows()
        .single { it.message.clientMessageId == clientMessageId }
        .let { Next(it.nextSenderId, it.nextTimestampMillis) }

    private fun rejectInsertsOf(clientMessageId: String) {
        database.openHelper.writableDatabase.execSQL(
            "CREATE TRIGGER reject_$clientMessageId BEFORE INSERT ON messages WHEN NEW.clientMessageId = '$clientMessageId' " +
                "BEGIN SELECT RAISE(ABORT, 'rejected'); END"
        )
    }

    private fun rejectDeletes() {
        database.openHelper.writableDatabase.execSQL(
            "CREATE TRIGGER reject_deletes BEFORE DELETE ON messages BEGIN SELECT RAISE(ABORT, 'rejected'); END"
        )
    }

    private fun pending(clientMessageId: String, timestamp: Long, sender: String = "sender", chat: String = CHAT) =
        message(clientMessageId, serverMessageId = null, seq = null, timestamp, sender, chat)

    private fun confirmed(clientMessageId: String, seq: Long, timestamp: Long, sender: String = "sender", chat: String = CHAT) =
        message(clientMessageId, serverMessageId = "server-$sender-$clientMessageId", seq, timestamp, sender, chat)

    private fun message(clientMessageId: String, serverMessageId: String?, seq: Long?, timestamp: Long, sender: String, chat: String) =
        MessageEntity(
            clientMessageId = clientMessageId,
            serverMessageId = serverMessageId,
            seq = seq,
            chatId = chat,
            senderId = sender,
            contentType = ContentType.TEXT,
            contentValue = "text",
            timestampMillis = timestamp
        )

    private companion object {
        const val CHAT = "chat"
        const val OTHER_CHAT = "other-chat"
    }
}
