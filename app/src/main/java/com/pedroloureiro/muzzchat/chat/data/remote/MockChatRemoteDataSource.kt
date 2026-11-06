package com.pedroloureiro.muzzchat.chat.data.remote

import com.pedroloureiro.muzzchat.chat.data.mock.MockData
import com.pedroloureiro.muzzchat.chat.data.remote.model.MessageDto
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

private const val ECHO_DELAY_MILLIS = 200L

class MockChatRemoteDataSource(private val mockData: MockData) : ChatRemoteDataSource {
    private val incoming = MutableSharedFlow<MessageDto>(replay = 0)

    override fun incomingMessages(): Flow<MessageDto> = incoming

    override suspend fun sendMessage(dto: MessageDto): Result<MessageDto> {
        delay(ECHO_DELAY_MILLIS)
        mockData.addMessage(dto)
        incoming.emit(dto)
        return Result.success(dto)
    }

    override suspend fun fetchMessages(): Result<List<MessageDto>> = Result.success(mockData.messages())
}
