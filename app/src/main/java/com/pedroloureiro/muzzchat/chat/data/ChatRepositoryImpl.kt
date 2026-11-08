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
import com.pedroloureiro.muzzchat.chat.data.remote.model.MessageDto
import com.pedroloureiro.muzzchat.chat.domain.model.Message
import com.pedroloureiro.muzzchat.chat.domain.model.MessageWithNext
import com.pedroloureiro.muzzchat.chat.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map

class ChatRepositoryImpl(
    private val local: ChatLocalDataSource,
    private val remote: ChatRemoteDataSource
) : ChatRepository {

    @OptIn(ExperimentalPagingApi::class)
    override fun getMessages(chatId: String): Flow<PagingData<MessageWithNext>> =
        Pager(
            config = PagingConfig(pageSize = MESSAGE_PAGE_SIZE),
            remoteMediator = MessageRemoteMediator(
                syncState = { local.syncState(chatId) },
                loadNewest = { loadNewest(chatId) },
                catchUp = { catchUp(chatId) },
                loadOlder = { beforeSeq -> loadOlder(chatId, beforeSeq) }
            ),
            pagingSourceFactory = { local.pagingSource(chatId) }
        ).flow.map { rows -> rows.map { it.toMessageWithNext() } }

    override suspend fun syncIncomingMessages(chatId: String) {
        remote.incomingMessages().filter { it.chatId == chatId }.collect { store(it) }
    }

    override suspend fun sendMessage(message: Message.Pending) {
        local.insertPending(message.toEntity())
        remote.sendMessage(message.chatId, message.toDto())
            .onSuccess { store(it) }
            // TODO: .onFailure { mark the pending message FAILED }
    }

    private suspend fun store(message: MessageDto) {
        if (local.storeIncoming(message.toEntity())) {
            catchUp(message.chatId)
                // TODO: .onFailure { retry the catch-up }
        }
    }

    private suspend fun loadNewest(chatId: String): Result<Unit> =
        remote.fetchMessagesBefore(chatId, beforeSeq = null, MESSAGE_PAGE_SIZE).map { page ->
            local.storeNewestPage(chatId, page.messages.map { it.toEntity() }, page.nextCursor, page.hasMore)
        }

    private suspend fun loadOlder(chatId: String, beforeSeq: Long): Result<Unit> =
        remote.fetchMessagesBefore(chatId, beforeSeq, MESSAGE_PAGE_SIZE).map { page ->
            local.storeOlderPage(chatId, page.messages.map { it.toEntity() }, page.nextCursor, page.hasMore)
        }

    private suspend fun catchUp(chatId: String): Result<Unit> {
        var afterSeq = local.syncState(chatId)?.newestSeq ?: return Result.success(Unit)
        while (true) {
            val page = remote.fetchMessagesAfter(chatId, afterSeq, MESSAGE_PAGE_SIZE).getOrElse { return Result.failure(it) }
            local.storeNewerPage(chatId, page.messages.map { it.toEntity() }, page.nextCursor)
            afterSeq = page.nextCursor ?: return Result.success(Unit)
            if (!page.hasMore) return Result.success(Unit)
        }
    }
}
