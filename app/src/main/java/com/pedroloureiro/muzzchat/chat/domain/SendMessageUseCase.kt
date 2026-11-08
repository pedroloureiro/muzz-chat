package com.pedroloureiro.muzzchat.chat.domain

import com.pedroloureiro.muzzchat.chat.domain.models.Content
import com.pedroloureiro.muzzchat.chat.domain.models.ContentType
import com.pedroloureiro.muzzchat.chat.domain.models.Message
import com.pedroloureiro.muzzchat.chat.domain.models.User
import java.util.UUID

class SendMessageUseCase(private val repository: ChatRepository) {
    suspend operator fun invoke(chatId: String, text: String, sender: User) {
        val trimmedText = text.trim()
        if (trimmedText.isEmpty()) return
        repository.sendMessage(
            Message.Pending(
                clientMessageId = UUID.randomUUID().toString(),
                chatId = chatId,
                senderId = sender.id,
                content = Content(ContentType.TEXT, trimmedText),
                timestampMillis = System.currentTimeMillis()
            )
        )
    }
}
