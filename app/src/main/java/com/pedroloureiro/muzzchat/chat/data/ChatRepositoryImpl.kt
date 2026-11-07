package com.pedroloureiro.muzzchat.chat.data

import com.pedroloureiro.muzzchat.chat.data.local.ChatLocalDataSource
import com.pedroloureiro.muzzchat.chat.data.remote.ChatRemoteDataSource
import com.pedroloureiro.muzzchat.chat.domain.model.Message
import com.pedroloureiro.muzzchat.chat.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ChatRepositoryImpl(
    private val local: ChatLocalDataSource,
    private val remote: ChatRemoteDataSource
) : ChatRepository {

    override fun getMessages(): Flow<List<Message>> =
        local.getMessages().map { entities -> entities.map { it.toMessage() } }

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
        remote.fetchMessages()
            .onSuccess { messages -> messages.forEach { local.confirm(it.toEntity()) } }
            // TODO: .onFailure { surface the error to the UI }
    }

    override suspend fun clearMessages() = local.clearAll()
}
