package com.pedroloureiro.muzzchat.chat.data.remote

import kotlinx.coroutines.flow.Flow

interface ChatRemoteDataSource {
    suspend fun sendMessage(dto: MessageDto): Result<MessageDto>
    fun incomingMessages(): Flow<MessageDto>
}
