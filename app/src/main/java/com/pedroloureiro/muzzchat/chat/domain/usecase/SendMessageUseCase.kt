package com.pedroloureiro.muzzchat.chat.domain.usecase

import com.pedroloureiro.muzzchat.chat.domain.model.Content
import com.pedroloureiro.muzzchat.chat.domain.model.ContentType
import com.pedroloureiro.muzzchat.chat.domain.model.Message
import com.pedroloureiro.muzzchat.chat.domain.model.User
import com.pedroloureiro.muzzchat.chat.domain.repository.ChatRepository
import java.util.UUID

class SendMessageUseCase(private val repository: ChatRepository) {
    suspend operator fun invoke(text: String, sender: User) {
        val trimmedText = text.trim()
        if (trimmedText.isEmpty()) return
        repository.sendMessage(
            Message.Pending(
                localId = UUID.randomUUID().toString(),
                senderId = sender.id,
                content = Content(ContentType.TEXT, trimmedText),
                timestampMillis = System.currentTimeMillis()
            )
        )
    }
}
