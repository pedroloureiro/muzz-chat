package com.pedroloureiro.muzzchat.chat.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pedroloureiro.muzzchat.chat.data.local.db.MessageDatabase
import com.pedroloureiro.muzzchat.chat.data.local.db.entities.MessageEntity
import com.pedroloureiro.muzzchat.chat.domain.models.ContentType
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RoomChatLocalDataSourceTest {

    private lateinit var database: MessageDatabase
    private lateinit var local: RoomChatLocalDataSource

    @Before
    fun openDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            MessageDatabase::class.java
        ).build()
        local = RoomChatLocalDataSource(database.messageDao())
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun `insertPending invalidates the paging sources handed out before it`() = assertInvalidatedBy {
        insertPending(message("pending", seq = null))
    }

    @Test
    fun `storeIncoming invalidates the paging sources handed out before it`() = assertInvalidatedBy {
        storeIncoming(message("incoming", seq = 1))
    }

    @Test
    fun `storeNewestPage invalidates the paging sources handed out before it`() = assertInvalidatedBy {
        storeNewestPage(CHAT, listOf(message("newest", seq = 1)), nextCursor = 1, hasMore = false)
    }

    @Test
    fun `storeOlderPage invalidates the paging sources handed out before it`() = assertInvalidatedBy {
        storeOlderPage(CHAT, listOf(message("older", seq = 1)), nextCursor = 1, hasMore = false)
    }

    @Test
    fun `storeNewerPage invalidates the paging sources handed out before it`() = assertInvalidatedBy {
        storeNewerPage(CHAT, listOf(message("newer", seq = 1)), nextCursor = 1)
    }

    private fun assertInvalidatedBy(write: suspend RoomChatLocalDataSource.() -> Unit) = runBlocking {
        val sources = listOf(local.pagingSource(CHAT), local.pagingSource(OTHER_CHAT))

        local.write()

        assertTrue(sources.all { it.invalid })
    }

    private fun message(clientMessageId: String, seq: Long?) = MessageEntity(
        clientMessageId = clientMessageId,
        serverMessageId = seq?.let { "server-$clientMessageId" },
        seq = seq,
        chatId = CHAT,
        senderId = "sender",
        contentType = ContentType.TEXT,
        contentValue = "text",
        timestampMillis = 1L
    )

    private companion object {
        const val CHAT = "chat"
        const val OTHER_CHAT = "other-chat"
    }
}
