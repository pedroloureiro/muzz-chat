package com.pedroloureiro.muzzchat.chat.data.mock

import android.content.Context
import com.pedroloureiro.muzzchat.chat.data.remote.model.MessageDto
import com.pedroloureiro.muzzchat.chat.data.remote.model.OutgoingMessageDto
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.serialization.json.Json

private const val SEED_MESSAGES_ASSET = "mock_messages.json"

class MockData(private val context: Context) {
    private val storedMessages = MutableStateFlow(emptyList<MessageDto>())

    fun messages(): List<MessageDto> = loadSeedMessages() + storedMessages.value

    fun page(beforeId: String?, limit: Int): List<MessageDto> =
        messages()
            .filter { beforeId == null || it.id < beforeId }
            .sortedByDescending { it.id }
            .take(limit)

    @OptIn(ExperimentalUuidApi::class)
    fun store(outgoing: OutgoingMessageDto): MessageDto =
        storedMessages.updateAndGet { stored ->
            if (stored.any { it.localId == outgoing.localId }) return@updateAndGet stored
            stored + MessageDto(
                id = Uuid.generateV7().toString(),
                localId = outgoing.localId,
                senderId = outgoing.senderId,
                content = outgoing.content,
                timestampMillis = System.currentTimeMillis()
            )
        }.single { it.localId == outgoing.localId }

    fun loadSeedMessages(): List<MessageDto> =
        context.assets.open(SEED_MESSAGES_ASSET).bufferedReader().use { reader ->
            Json.decodeFromString(reader.readText())
        }
}
