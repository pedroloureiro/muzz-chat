package com.pedroloureiro.muzzchat.chat.data.remote

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

private const val ECHO_DELAY_MILLIS = 200L

class MockChatRemoteDataSource : ChatRemoteDataSource {
    private val incoming = MutableSharedFlow<MessageDto>(replay = 0)

    override fun incomingMessages(): Flow<MessageDto> = incoming

    override suspend fun sendMessage(dto: MessageDto): Result<MessageDto> {
        delay(ECHO_DELAY_MILLIS)
        incoming.emit(dto)
        return Result.success(dto)
    }
}
