package com.pedroloureiro.muzzchat.chat.data.mock

import android.content.Context
import com.pedroloureiro.muzzchat.chat.data.remote.model.MessageDto
import kotlinx.serialization.json.Json

private const val SEED_MESSAGES_ASSET = "mock_messages.json"

class MockData(private val context: Context) {
    fun loadSeedMessages(): List<MessageDto> =
        context.assets.open(SEED_MESSAGES_ASSET).bufferedReader().use { reader ->
            Json.decodeFromString(reader.readText())
        }
}
