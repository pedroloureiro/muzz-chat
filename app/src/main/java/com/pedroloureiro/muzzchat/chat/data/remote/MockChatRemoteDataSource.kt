package com.pedroloureiro.muzzchat.chat.data.remote

import com.pedroloureiro.muzzchat.chat.data.MockChatApi
import com.pedroloureiro.muzzchat.chat.data.remote.models.ContentDto
import com.pedroloureiro.muzzchat.chat.data.remote.models.MessageDto
import com.pedroloureiro.muzzchat.chat.data.remote.models.MessagePageDto
import com.pedroloureiro.muzzchat.chat.data.remote.models.OutgoingMessageDto
import com.pedroloureiro.muzzchat.chat.domain.OtherUserDevice
import com.pedroloureiro.muzzchat.chat.domain.models.ContentType
import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

internal const val MOCK_NETWORK_DELAY_MILLIS = 250L

class MockChatRemoteDataSource(private val api: MockChatApi) : ChatRemoteDataSource, OtherUserDevice {
    private val incoming = MutableSharedFlow<MessageDto>(replay = 0)

    override fun incomingMessages(): Flow<MessageDto> = incoming

    override suspend fun sendMessage(chatId: String, outgoing: OutgoingMessageDto): Result<MessageDto> {
        delay(MOCK_NETWORK_DELAY_MILLIS)
        val stored = api.send(chatId, outgoing)
        incoming.emit(stored)
        return Result.success(stored)
    }

    // MOCK DATA, droppable: Sarah has no device in the exercise, so this one sends her messages. Delete it with
    // OtherUserDevice once other users' messages arrive from the server. Only this path names its sender.
    override suspend fun sendAsAnother(chatId: String, senderId: String, text: String) {
        delay(MOCK_NETWORK_DELAY_MILLIS)
        val outgoing = OutgoingMessageDto(
            clientMessageId = UUID.randomUUID().toString(),
            content = ContentDto(ContentType.TEXT.name, text),
            timestampMillis = System.currentTimeMillis()
        )
        incoming.emit(api.sendAs(chatId, senderId, outgoing))
    }

    override suspend fun fetchMessagesBefore(chatId: String, beforeSeq: Long?, limit: Int): Result<MessagePageDto> {
        delay(MOCK_NETWORK_DELAY_MILLIS)
        return Result.success(api.pageBefore(chatId, beforeSeq, limit))
    }

    override suspend fun fetchMessagesAfter(chatId: String, afterSeq: Long, limit: Int): Result<MessagePageDto> {
        delay(MOCK_NETWORK_DELAY_MILLIS)
        return Result.success(api.pageAfter(chatId, afterSeq, limit))
    }
}
