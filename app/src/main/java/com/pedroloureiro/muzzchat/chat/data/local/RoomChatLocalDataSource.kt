package com.pedroloureiro.muzzchat.chat.data.local

import androidx.paging.PagingSource
import com.pedroloureiro.muzzchat.chat.data.local.db.MessageDao
import com.pedroloureiro.muzzchat.chat.data.local.db.entities.ChatSyncEntity
import com.pedroloureiro.muzzchat.chat.data.local.db.entities.MessageEntity
import com.pedroloureiro.muzzchat.chat.data.local.db.entities.MessageWithNextRow
import java.util.concurrent.CopyOnWriteArraySet

class RoomChatLocalDataSource(private val dao: MessageDao) : ChatLocalDataSource {
    // Room can miss a write that commits during a paging source's first load and leave its page stale,
    // so every write here also invalidates the sources still live.
    private val liveSources = CopyOnWriteArraySet<PagingSource<*, *>>()

    override suspend fun insertPending(entity: MessageEntity) = invalidatingSources { dao.insertPending(entity) }

    override suspend fun storeIncoming(entity: MessageEntity): Boolean = invalidatingSources { dao.storeIncoming(entity) }

    override suspend fun storeNewestPage(chatId: String, entities: List<MessageEntity>, nextCursor: Long?, hasMore: Boolean) =
        invalidatingSources { dao.storeNewestPage(chatId, entities, nextCursor, hasMore) }

    override suspend fun storeOlderPage(chatId: String, entities: List<MessageEntity>, nextCursor: Long?, hasMore: Boolean) =
        invalidatingSources { dao.storeOlderPage(chatId, entities, nextCursor, hasMore) }

    override suspend fun storeNewerPage(chatId: String, entities: List<MessageEntity>, nextCursor: Long?) =
        invalidatingSources { dao.storeNewerPage(chatId, entities, nextCursor) }

    override suspend fun syncState(chatId: String): ChatSyncEntity? = dao.syncState(chatId)

    override fun pagingSource(chatId: String): PagingSource<Int, MessageWithNextRow> =
        dao.pagingSource(chatId).also { source ->
            liveSources += source
            source.registerInvalidatedCallback { liveSources -= source }
        }

    private inline fun <T> invalidatingSources(write: () -> T): T =
        write().also { liveSources.forEach(PagingSource<*, *>::invalidate) }
}
