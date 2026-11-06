package com.pedroloureiro.muzzchat.chat.domain.usecase

import com.pedroloureiro.muzzchat.chat.domain.model.Content
import com.pedroloureiro.muzzchat.chat.domain.model.ContentType
import com.pedroloureiro.muzzchat.chat.domain.model.Message
import com.pedroloureiro.muzzchat.chat.domain.model.MessageDeliveryStatus
import com.pedroloureiro.muzzchat.chat.domain.model.Users
import com.pedroloureiro.muzzchat.chat.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SendMessageUseCaseTest {

    private val repository = FakeChatRepository()
    private val sendMessage = SendMessageUseCase(
        repository = repository,
        currentTimeMillis = { 42L },
        generateMessageId = { "generated-id" }
    )

    @Test
    fun blankText_sendsNothing() = runTest {
        sendMessage("  \n\t ", Users.ME)

        assertEquals(emptyList<Message>(), repository.sent)
    }

    @Test
    fun textIsTrimmed() = runTest {
        sendMessage("  hello  ", Users.ME)

        assertEquals("hello", repository.sent.single().content.value)
    }

    @Test
    fun message_usesInjectedIdAndTimestamp() = runTest {
        sendMessage("hello", Users.ME)

        val message = repository.sent.single()
        assertEquals("generated-id", message.messageId)
        assertEquals(42L, message.timestamp)
    }

    @Test
    fun message_usesSenderId() = runTest {
        sendMessage("hello", Users.SARAH)

        assertEquals(Users.SARAH.id, repository.sent.single().senderId)
    }

    @Test
    fun message_isTextWithSendingStatus() = runTest {
        sendMessage("hello", Users.ME)

        val message = repository.sent.single()
        assertEquals(Content(ContentType.TEXT, "hello"), message.content)
        assertEquals(MessageDeliveryStatus.SENDING, message.deliveryStatus)
    }

    private class FakeChatRepository : ChatRepository {
        val sent = mutableListOf<Message>()

        override fun getMessages(): Flow<List<Message>> = emptyFlow()
        override suspend fun sendMessage(message: Message) {
            sent += message
        }
        override suspend fun loadSampleData() = Unit
        override suspend fun clearMessages() = Unit
    }
}
