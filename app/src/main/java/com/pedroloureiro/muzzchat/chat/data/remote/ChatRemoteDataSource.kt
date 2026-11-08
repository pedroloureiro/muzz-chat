package com.pedroloureiro.muzzchat.chat.data.remote

import com.pedroloureiro.muzzchat.chat.data.remote.model.MessageDto
import com.pedroloureiro.muzzchat.chat.data.remote.model.MessagePageDto
import com.pedroloureiro.muzzchat.chat.data.remote.model.OutgoingMessageDto
import kotlinx.coroutines.flow.Flow

interface ChatRemoteDataSource {
    suspend fun sendMessage(chatId: String, outgoing: OutgoingMessageDto): Result<MessageDto>
    // Up to `limit` messages older than `beforeSeq`, newest first. A null `beforeSeq` means the newest messages.
    suspend fun fetchMessagesBefore(chatId: String, beforeSeq: Long?, limit: Int): Result<MessagePageDto>
    // Up to `limit` messages newer than `afterSeq`, oldest first.
    suspend fun fetchMessagesAfter(chatId: String, afterSeq: Long, limit: Int): Result<MessagePageDto>
    // Every chat's messages, on one stream.
    fun incomingMessages(): Flow<MessageDto>
}
