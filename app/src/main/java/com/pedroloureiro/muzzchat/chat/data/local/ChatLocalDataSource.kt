package com.pedroloureiro.muzzchat.chat.data.local

import com.pedroloureiro.muzzchat.chat.data.local.db.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

interface ChatLocalDataSource {
    suspend fun insertPending(entity: MessageEntity)
    suspend fun confirm(entity: MessageEntity)
    fun getMessages(): Flow<List<MessageEntity>>
    suspend fun clearAll()
}
