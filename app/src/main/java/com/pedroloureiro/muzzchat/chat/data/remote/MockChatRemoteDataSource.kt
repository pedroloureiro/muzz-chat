package com.pedroloureiro.muzzchat.chat.data.remote

import com.pedroloureiro.muzzchat.chat.data.mock.MockData
import com.pedroloureiro.muzzchat.chat.data.remote.model.MessageDto
import com.pedroloureiro.muzzchat.chat.data.remote.model.OutgoingMessageDto
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

private const val ECHO_DELAY_MILLIS = 200L

class MockChatRemoteDataSource(private val mockData: MockData) : ChatRemoteDataSource {
    private val incoming = MutableSharedFlow<MessageDto>(replay = 0)

    override fun incomingMessages(): Flow<MessageDto> = incoming

    override suspend fun sendMessage(outgoing: OutgoingMessageDto): Result<MessageDto> {
        delay(ECHO_DELAY_MILLIS)
        val stored = mockData.store(outgoing)
        incoming.emit(stored)
        return Result.success(stored)
    }

    override suspend fun fetchMessages(beforeId: String?, limit: Int): Result<List<MessageDto>> =
        Result.success(mockData.page(beforeId, limit))
}
