package com.pedroloureiro.muzzchat.chat.data.remote

import com.pedroloureiro.muzzchat.chat.data.mock.MockData
import com.pedroloureiro.muzzchat.chat.data.remote.model.MessageDto
import com.pedroloureiro.muzzchat.chat.data.remote.model.MessagePageDto
import com.pedroloureiro.muzzchat.chat.data.remote.model.OutgoingMessageDto
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

private const val MOCK_NETWORK_DELAY_MILLIS = 200L

class MockChatRemoteDataSource(private val mockData: MockData) : ChatRemoteDataSource {
    private val incoming = MutableSharedFlow<MessageDto>(replay = 0)

    override fun incomingMessages(): Flow<MessageDto> = incoming

    override suspend fun sendMessage(chatId: String, outgoing: OutgoingMessageDto): Result<MessageDto> {
        delay(MOCK_NETWORK_DELAY_MILLIS)
        val stored = mockData.store(chatId, outgoing)
        incoming.emit(stored)
        return Result.success(stored)
    }

    override suspend fun fetchMessagesBefore(chatId: String, beforeSeq: Long?, limit: Int): Result<MessagePageDto> {
        delay(MOCK_NETWORK_DELAY_MILLIS)
        return Result.success(mockData.pageBefore(chatId, beforeSeq, limit))
    }

    override suspend fun fetchMessagesAfter(chatId: String, afterSeq: Long, limit: Int): Result<MessagePageDto> {
        delay(MOCK_NETWORK_DELAY_MILLIS)
        return Result.success(mockData.pageAfter(chatId, afterSeq, limit))
    }
}
