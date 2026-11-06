package com.pedroloureiro.muzzchat.chat.data.local

import com.pedroloureiro.muzzchat.chat.data.local.db.dao.MessageDao
import com.pedroloureiro.muzzchat.chat.data.local.db.entity.MessageEntity
import com.pedroloureiro.muzzchat.chat.domain.model.MessageDeliveryStatus
import kotlinx.coroutines.flow.Flow

class RoomChatLocalDataSource(private val dao: MessageDao) : ChatLocalDataSource {
    override suspend fun insertMessage(entity: MessageEntity) = dao.insert(entity)

    override suspend fun updateStatus(messageId: String, status: MessageDeliveryStatus) =
        dao.updateStatus(messageId, status)

    override fun getMessages(): Flow<List<MessageEntity>> = dao.getMessages()

    override suspend fun clearAll() = dao.clearAll()
}
