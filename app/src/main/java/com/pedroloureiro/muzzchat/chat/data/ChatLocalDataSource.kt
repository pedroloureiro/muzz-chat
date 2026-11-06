package com.pedroloureiro.muzzchat.chat.data

import com.pedroloureiro.muzzchat.chat.data.db.MessageDeliveryStatus
import com.pedroloureiro.muzzchat.chat.data.db.MessageEntity
import kotlinx.coroutines.flow.Flow

interface ChatLocalDataSource {
    suspend fun insertMessage(entity: MessageEntity)
    suspend fun updateStatus(messageId: String, status: MessageDeliveryStatus)
    fun getMessages(): Flow<List<MessageEntity>>
    suspend fun clearAll()
}
