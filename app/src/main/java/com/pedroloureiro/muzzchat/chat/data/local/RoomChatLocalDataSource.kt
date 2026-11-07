package com.pedroloureiro.muzzchat.chat.data.local

import androidx.paging.PagingSource
import com.pedroloureiro.muzzchat.chat.data.local.db.dao.MessageDao
import com.pedroloureiro.muzzchat.chat.data.local.db.entity.MessageEntity
import com.pedroloureiro.muzzchat.chat.data.local.db.entity.MessageWithNextRow

class RoomChatLocalDataSource(private val dao: MessageDao) : ChatLocalDataSource {
    override suspend fun insertPending(entity: MessageEntity) = dao.insertPending(entity)

    override suspend fun confirm(entity: MessageEntity) = dao.confirm(entity)

    override suspend fun confirmAll(entities: List<MessageEntity>) = dao.confirmAll(entities)

    override suspend fun oldestId(): String? = dao.oldestId()

    override fun pagingSource(): PagingSource<Int, MessageWithNextRow> = dao.pagingSource()

    override suspend fun clearAll() = dao.clearAll()
}
