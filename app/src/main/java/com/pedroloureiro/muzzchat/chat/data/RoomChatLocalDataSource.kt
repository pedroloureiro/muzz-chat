package com.pedroloureiro.muzzchat.chat.data

import com.pedroloureiro.muzzchat.chat.data.db.MessageDao
import com.pedroloureiro.muzzchat.chat.data.db.MessageDeliveryStatus
import com.pedroloureiro.muzzchat.chat.data.db.MessageEntity
import kotlinx.coroutines.flow.Flow

class RoomChatLocalDataSource(private val dao: MessageDao) : ChatLocalDataSource {
    override suspend fun insertMessage(entity: MessageEntity) = dao.insert(entity)

    override suspend fun updateStatus(messageId: String, status: MessageDeliveryStatus) =
        dao.updateStatus(messageId, status)

    override fun getMessages(): Flow<List<MessageEntity>> = dao.getMessages()

    override suspend fun clearAll() = dao.clearAll()
}
