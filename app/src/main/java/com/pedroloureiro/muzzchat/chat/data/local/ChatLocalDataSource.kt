package com.pedroloureiro.muzzchat.chat.data.local

import androidx.paging.PagingSource
import com.pedroloureiro.muzzchat.chat.data.local.db.entity.MessageEntity
import com.pedroloureiro.muzzchat.chat.data.local.db.entity.MessageWithNextRow

interface ChatLocalDataSource {
    suspend fun insertPending(entity: MessageEntity)
    suspend fun confirm(entity: MessageEntity)
    suspend fun confirmAll(entities: List<MessageEntity>)
    suspend fun oldestId(): String?
    fun pagingSource(): PagingSource<Int, MessageWithNextRow>
    suspend fun clearAll()
}
