package com.pedroloureiro.muzzchat.chat.data.mock

import android.content.Context
import com.pedroloureiro.muzzchat.chat.data.remote.model.MessageDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.Json

private const val SEED_MESSAGES_ASSET = "mock_messages.json"

class MockData(private val context: Context) {
    private val addedMessages = MutableStateFlow(emptyList<MessageDto>())

    fun messages(): List<MessageDto> = loadSeedMessages() + addedMessages.value

    fun addMessage(message: MessageDto) = addedMessages.update { it + message }

    fun loadSeedMessages(): List<MessageDto> =
        context.assets.open(SEED_MESSAGES_ASSET).bufferedReader().use { reader ->
            Json.decodeFromString(reader.readText())
        }
}
