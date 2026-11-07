package com.pedroloureiro.muzzchat.chat.domain.repository

import com.pedroloureiro.muzzchat.chat.domain.model.Message
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun getMessages(): Flow<List<Message>>
    suspend fun syncIncomingMessages()
    suspend fun sendMessage(message: Message.Pending)
    suspend fun loadMessages()
    suspend fun clearMessages()
}
