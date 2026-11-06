package com.pedroloureiro.muzzchat.chat.data

import com.pedroloureiro.muzzchat.chat.data.local.ChatLocalDataSource
import com.pedroloureiro.muzzchat.chat.data.remote.ChatRemoteDataSource
import com.pedroloureiro.muzzchat.chat.domain.model.Message
import com.pedroloureiro.muzzchat.chat.domain.model.MessageDeliveryStatus
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
        remote.incomingMessages().collect { dto ->
            local.insertMessage(dto.toEntity(status = MessageDeliveryStatus.SENT))
        }
    }

    override suspend fun sendMessage(message: Message) {
        local.insertMessage(message.toEntity())
        remote.sendMessage(message.toDto())
            .onSuccess { local.updateStatus(message.messageId, MessageDeliveryStatus.SENT) }
            // TODO: .onFailure { local.updateStatus(message.messageId, MessageDeliveryStatus.FAILED) }
    }

    override suspend fun loadMessages() {
        remote.fetchMessages()
            .onSuccess { messages -> messages.forEach { local.insertMessage(it.toEntity()) } }
            // TODO: .onFailure { surface the error to the UI }
    }

    override suspend fun clearMessages() = local.clearAll()
}
