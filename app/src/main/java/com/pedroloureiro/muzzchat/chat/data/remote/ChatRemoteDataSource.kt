package com.pedroloureiro.muzzchat.chat.data.remote

import com.pedroloureiro.muzzchat.chat.data.remote.model.MessageDto
import com.pedroloureiro.muzzchat.chat.data.remote.model.OutgoingMessageDto
import kotlinx.coroutines.flow.Flow

interface ChatRemoteDataSource {
    suspend fun sendMessage(outgoing: OutgoingMessageDto): Result<MessageDto>
    // Up to `limit` messages older than `beforeId`, newest first. A null `beforeId` means the newest messages.
    suspend fun fetchMessages(beforeId: String?, limit: Int): Result<List<MessageDto>>
    fun incomingMessages(): Flow<MessageDto>
}
