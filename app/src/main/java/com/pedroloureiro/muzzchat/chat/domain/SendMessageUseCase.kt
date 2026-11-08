package com.pedroloureiro.muzzchat.chat.domain

import com.pedroloureiro.muzzchat.chat.domain.models.Content
import com.pedroloureiro.muzzchat.chat.domain.models.ContentType
import com.pedroloureiro.muzzchat.chat.domain.models.Message
import com.pedroloureiro.muzzchat.chat.domain.models.Users
import java.util.UUID

class SendMessageUseCase(private val repository: ChatRepository) {
    suspend operator fun invoke(chatId: String, text: String) {
        val trimmedText = text.trim()
        if (trimmedText.isEmpty()) return
        repository.sendMessage(
            Message.Pending(
                clientMessageId = UUID.randomUUID().toString(),
                chatId = chatId,
                senderId = Users.ME.id,
                content = Content(ContentType.TEXT, trimmedText),
                timestampMillis = System.currentTimeMillis()
            )
        )
    }
}
