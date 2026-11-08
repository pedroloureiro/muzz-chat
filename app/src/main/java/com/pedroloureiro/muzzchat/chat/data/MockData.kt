package com.pedroloureiro.muzzchat.chat.data

import android.content.Context
import com.pedroloureiro.muzzchat.chat.data.remote.models.MessageDto
import com.pedroloureiro.muzzchat.chat.data.remote.models.MessagePageDto
import com.pedroloureiro.muzzchat.chat.data.remote.models.OutgoingMessageDto
import java.io.File
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.serialization.json.Json

private const val SEED_MESSAGES_ASSET = "mock_messages.json"
private const val SENT_MESSAGES_FILE = "mock_sent_messages.json"

class MockData(private val context: Context) {
    private val sentMessagesFile = File(context.filesDir, SENT_MESSAGES_FILE)
    private val storedMessages = MutableStateFlow(loadSentMessages())

    fun messages(): List<MessageDto> = loadSeedMessages() + storedMessages.value

    fun pageBefore(chatId: String, beforeSeq: Long?, limit: Int): MessagePageDto {
        val older = messagesIn(chatId).filter { beforeSeq == null || it.seq < beforeSeq }.sortedByDescending { it.seq }
        return page(older, limit)
    }

    fun pageAfter(chatId: String, afterSeq: Long, limit: Int): MessagePageDto {
        val newer = messagesIn(chatId).filter { it.seq > afterSeq }.sortedBy { it.seq }
        return page(newer, limit)
    }

    @OptIn(ExperimentalUuidApi::class)
    fun store(chatId: String, outgoing: OutgoingMessageDto): MessageDto =
        storedMessages.updateAndGet { stored ->
            if (stored.any { it.isResendOf(outgoing) }) return@updateAndGet stored
            val newestSeq = (loadSeedMessages() + stored).filter { it.chatId == chatId }.maxOfOrNull { it.seq } ?: 0
            stored + MessageDto(
                clientMessageId = outgoing.clientMessageId,
                serverMessageId = Uuid.random().toString(),
                seq = newestSeq + 1,
                chatId = chatId,
                senderId = outgoing.senderId,
                content = outgoing.content,
                timestampMillis = System.currentTimeMillis()
            )
        }.also { sentMessagesFile.writeText(Json.encodeToString(it)) }
            .single { it.isResendOf(outgoing) }

    fun loadSeedMessages(): List<MessageDto> =
        context.assets.open(SEED_MESSAGES_ASSET).bufferedReader().use { reader ->
            Json.decodeFromString(reader.readText())
        }

    private fun loadSentMessages(): List<MessageDto> =
        if (sentMessagesFile.exists()) Json.decodeFromString(sentMessagesFile.readText()) else emptyList()

    private fun MessageDto.isResendOf(outgoing: OutgoingMessageDto) =
        senderId == outgoing.senderId && clientMessageId == outgoing.clientMessageId

    private fun messagesIn(chatId: String) = messages().filter { it.chatId == chatId }

    private fun page(inDirection: List<MessageDto>, limit: Int): MessagePageDto {
        val messages = inDirection.take(limit)
        return MessagePageDto(messages, nextCursor = messages.lastOrNull()?.seq, hasMore = inDirection.size > limit)
    }
}
