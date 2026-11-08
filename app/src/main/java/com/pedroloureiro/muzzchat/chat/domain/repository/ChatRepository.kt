package com.pedroloureiro.muzzchat.chat.domain.repository

import androidx.paging.PagingData
import com.pedroloureiro.muzzchat.chat.domain.model.Message
import com.pedroloureiro.muzzchat.chat.domain.model.MessageWithNext
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun getMessages(chatId: String): Flow<PagingData<MessageWithNext>>
    suspend fun syncIncomingMessages(chatId: String)
    suspend fun sendMessage(message: Message.Pending)
}
