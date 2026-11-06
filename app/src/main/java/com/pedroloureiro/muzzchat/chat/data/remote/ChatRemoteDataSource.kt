package com.pedroloureiro.muzzchat.chat.data.remote

import com.pedroloureiro.muzzchat.chat.data.remote.model.MessageDto
import kotlinx.coroutines.flow.Flow

interface ChatRemoteDataSource {
    suspend fun sendMessage(dto: MessageDto): Result<MessageDto>
    suspend fun fetchMessages(): Result<List<MessageDto>>
    fun incomingMessages(): Flow<MessageDto>
}
