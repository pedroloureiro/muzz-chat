package com.pedroloureiro.muzzchat.chat.domain.repository

import com.pedroloureiro.muzzchat.chat.domain.model.Message
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun getMessages(): Flow<List<Message>>
    suspend fun sendMessage(message: Message)
    suspend fun loadSampleData()
    suspend fun clearMessages()
}
