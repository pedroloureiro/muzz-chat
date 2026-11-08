package com.pedroloureiro.muzzchat.chat.data.remote

import com.pedroloureiro.muzzchat.chat.data.remote.models.MessageDto
import com.pedroloureiro.muzzchat.chat.data.remote.models.MessagePageDto
import com.pedroloureiro.muzzchat.chat.data.remote.models.OutgoingMessageDto
import kotlinx.coroutines.flow.Flow

interface ChatRemoteDataSource {
    suspend fun sendMessage(chatId: String, outgoing: OutgoingMessageDto): Result<MessageDto>
    suspend fun fetchMessagesBefore(chatId: String, beforeSeq: Long?, limit: Int): Result<MessagePageDto>
    suspend fun fetchMessagesAfter(chatId: String, afterSeq: Long, limit: Int): Result<MessagePageDto>
    fun incomingMessages(): Flow<MessageDto>
}
