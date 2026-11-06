package com.pedroloureiro.muzzchat.chat.data.local

import com.pedroloureiro.muzzchat.chat.data.db.entities.MessageEntity
import com.pedroloureiro.muzzchat.chat.data.db.models.MessageDeliveryStatus
import kotlinx.coroutines.flow.Flow

interface ChatLocalDataSource {
    suspend fun insertMessage(entity: MessageEntity)
    suspend fun updateStatus(messageId: String, status: MessageDeliveryStatus)
    fun getMessages(): Flow<List<MessageEntity>>
    suspend fun clearAll()
}
