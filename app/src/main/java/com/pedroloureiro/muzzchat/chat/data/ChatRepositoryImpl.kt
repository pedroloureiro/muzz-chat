package com.pedroloureiro.muzzchat.chat.data

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.pedroloureiro.muzzchat.chat.data.local.ChatLocalDataSource
import com.pedroloureiro.muzzchat.chat.data.paging.MESSAGE_PAGE_SIZE
import com.pedroloureiro.muzzchat.chat.data.paging.MessageRemoteMediator
import com.pedroloureiro.muzzchat.chat.data.remote.ChatRemoteDataSource
import com.pedroloureiro.muzzchat.chat.domain.model.Message
import com.pedroloureiro.muzzchat.chat.domain.model.MessageWithNext
import com.pedroloureiro.muzzchat.chat.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ChatRepositoryImpl(
    private val local: ChatLocalDataSource,
    private val remote: ChatRemoteDataSource
) : ChatRepository {

    @OptIn(ExperimentalPagingApi::class)
    override fun getMessages(): Flow<PagingData<MessageWithNext>> =
        Pager(
            config = PagingConfig(pageSize = MESSAGE_PAGE_SIZE),
            remoteMediator = MessageRemoteMediator(oldestStoredId = local::oldestId, storePage = ::storePage),
            pagingSourceFactory = local::pagingSource
        ).flow.map { rows -> rows.map { it.toMessageWithNext() } }

    override suspend fun syncIncomingMessages() {
        remote.incomingMessages().collect { local.confirm(it.toEntity()) }
    }

    override suspend fun sendMessage(message: Message.Pending) {
        local.insertPending(message.toEntity())
        remote.sendMessage(message.toDto())
            .onSuccess { local.confirm(it.toEntity()) }
            // TODO: .onFailure { mark the pending message FAILED }
    }

    override suspend fun loadMessages() {
        storePage(beforeId = null, limit = MESSAGE_PAGE_SIZE)
            // TODO: .onFailure { surface the error to the UI }
    }

    override suspend fun clearMessages() = local.clearAll()

    private suspend fun storePage(beforeId: String?, limit: Int): Result<Int> =
        remote.fetchMessages(beforeId, limit)
            .onSuccess { page -> local.confirmAll(page.map { it.toEntity() }) }
            .map { it.size }
}
