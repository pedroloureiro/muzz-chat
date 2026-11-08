package com.pedroloureiro.muzzchat.chat.data.remote

import com.pedroloureiro.muzzchat.chat.data.MockData
import com.pedroloureiro.muzzchat.chat.data.remote.models.ContentDto
import com.pedroloureiro.muzzchat.chat.data.remote.models.MessageDto
import com.pedroloureiro.muzzchat.chat.data.remote.models.MessagePageDto
import com.pedroloureiro.muzzchat.chat.data.remote.models.OutgoingMessageDto
import com.pedroloureiro.muzzchat.chat.domain.OtherUserDevice
import com.pedroloureiro.muzzchat.chat.domain.models.ContentType
import com.pedroloureiro.muzzchat.chat.domain.models.Users
import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

private const val MOCK_NETWORK_DELAY_MILLIS = 200L

class MockChatRemoteDataSource(private val mockData: MockData) : ChatRemoteDataSource, OtherUserDevice {
    private val incoming = MutableSharedFlow<MessageDto>(replay = 0)

    override fun incomingMessages(): Flow<MessageDto> = incoming

    override suspend fun sendMessage(chatId: String, outgoing: OutgoingMessageDto): Result<MessageDto> {
        delay(MOCK_NETWORK_DELAY_MILLIS)
        val stored = mockData.store(chatId, outgoing)
        incoming.emit(stored)
        return Result.success(stored)
    }

    override suspend fun send(chatId: String, text: String) {
        delay(MOCK_NETWORK_DELAY_MILLIS)
        val outgoing = OutgoingMessageDto(
            clientMessageId = UUID.randomUUID().toString(),
            senderId = Users.SARAH.id,
            content = ContentDto(ContentType.TEXT.name, text),
            timestampMillis = System.currentTimeMillis()
        )
        incoming.emit(mockData.store(chatId, outgoing))
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
