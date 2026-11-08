package com.pedroloureiro.muzzchat.chat.data.local

import androidx.paging.PagingSource
import com.pedroloureiro.muzzchat.chat.data.local.db.MessageDatabase
import com.pedroloureiro.muzzchat.chat.data.local.db.entities.ChatSyncEntity
import com.pedroloureiro.muzzchat.chat.data.local.db.entities.MessageEntity
import com.pedroloureiro.muzzchat.chat.data.local.db.entities.MessageWithNextRow

private const val MESSAGES_TABLE = "messages"

class RoomChatLocalDataSource(private val database: MessageDatabase) : ChatLocalDataSource {
    private val dao = database.messageDao()

    override suspend fun insertPending(entity: MessageEntity) = dao.insertPending(entity)

    override suspend fun storeIncoming(entity: MessageEntity): Boolean = dao.storeIncoming(entity)

    override suspend fun storeNewestPage(chatId: String, entities: List<MessageEntity>, nextCursor: Long?, hasMore: Boolean) =
        dao.storeNewestPage(chatId, entities, nextCursor, hasMore)

    override suspend fun storeOlderPage(chatId: String, entities: List<MessageEntity>, nextCursor: Long?, hasMore: Boolean) =
        dao.storeOlderPage(chatId, entities, nextCursor, hasMore)

    override suspend fun storeNewerPage(chatId: String, entities: List<MessageEntity>, nextCursor: Long?) =
        dao.storeNewerPage(chatId, entities, nextCursor)

    override suspend fun syncState(chatId: String): ChatSyncEntity? = dao.syncState(chatId)

    override fun pagingSource(chatId: String): PagingSource<Int, MessageWithNextRow> = FirstLoadSafePagingSource(
        room = dao.pagingSource(chatId),
        tableChanges = database.invalidationTracker.createFlow(MESSAGES_TABLE, emitInitialState = false),
    )
}
