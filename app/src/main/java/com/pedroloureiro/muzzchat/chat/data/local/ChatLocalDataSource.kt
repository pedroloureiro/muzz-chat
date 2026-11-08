package com.pedroloureiro.muzzchat.chat.data.local

import androidx.paging.PagingSource
import com.pedroloureiro.muzzchat.chat.data.local.db.entities.ChatSyncEntity
import com.pedroloureiro.muzzchat.chat.data.local.db.entities.MessageEntity
import com.pedroloureiro.muzzchat.chat.data.local.db.entities.MessageWithNextRow

interface ChatLocalDataSource {
    suspend fun insertPending(entity: MessageEntity)
    // Returns true when the message skips a seq.
    suspend fun storeIncoming(entity: MessageEntity): Boolean
    suspend fun storeNewestPage(chatId: String, entities: List<MessageEntity>, nextCursor: Long?, hasMore: Boolean)
    suspend fun storeOlderPage(chatId: String, entities: List<MessageEntity>, nextCursor: Long?, hasMore: Boolean)
    suspend fun storeNewerPage(chatId: String, entities: List<MessageEntity>, nextCursor: Long?)
    suspend fun syncState(chatId: String): ChatSyncEntity?
    fun pagingSource(chatId: String): PagingSource<Int, MessageWithNextRow>
}
