package com.pedroloureiro.muzzchat.chat.data.remote

import com.pedroloureiro.muzzchat.chat.data.remote.model.MessageDto
import com.pedroloureiro.muzzchat.chat.data.remote.model.OutgoingMessageDto
import kotlinx.coroutines.flow.Flow

interface ChatRemoteDataSource {
    suspend fun sendMessage(outgoing: OutgoingMessageDto): Result<MessageDto>
    suspend fun fetchMessages(): Result<List<MessageDto>>
    fun incomingMessages(): Flow<MessageDto>
}
