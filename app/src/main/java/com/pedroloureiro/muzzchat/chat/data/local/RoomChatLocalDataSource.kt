package com.pedroloureiro.muzzchat.chat.data.local

import com.pedroloureiro.muzzchat.chat.data.local.db.dao.MessageDao
import com.pedroloureiro.muzzchat.chat.data.local.db.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

class RoomChatLocalDataSource(private val dao: MessageDao) : ChatLocalDataSource {
    override suspend fun insertPending(entity: MessageEntity) = dao.insertPending(entity)

    override suspend fun confirm(entity: MessageEntity) = dao.confirm(entity)

    override fun getMessages(): Flow<List<MessageEntity>> = dao.getMessages()

    override suspend fun clearAll() = dao.clearAll()
}
