package com.pedroloureiro.muzzchat.chat.domain.repository

import com.pedroloureiro.muzzchat.chat.domain.model.Message
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun getMessages(): Flow<List<Message>>

    /** Stores incoming messages until the caller cancels it, so it never returns normally. */
    suspend fun syncIncomingMessages()

    suspend fun sendMessage(message: Message)
    suspend fun loadSampleData()
    suspend fun clearMessages()
}
