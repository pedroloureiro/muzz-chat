package com.pedroloureiro.muzzchat.chat.domain.usecase

import com.pedroloureiro.muzzchat.chat.domain.model.Content
import com.pedroloureiro.muzzchat.chat.domain.model.ContentType
import com.pedroloureiro.muzzchat.chat.domain.model.Message
import com.pedroloureiro.muzzchat.chat.domain.model.MessageDeliveryStatus
import com.pedroloureiro.muzzchat.chat.domain.model.Users
import com.pedroloureiro.muzzchat.chat.domain.repository.ChatRepository
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SendMessageUseCaseTest {

    private val repository = FakeChatRepository()
    private val sendMessage = SendMessageUseCase(repository)

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
    fun message_idIsAUniqueUuid() = runTest {
        sendMessage("hello", Users.ME)
        sendMessage("hello", Users.ME)

        val (first, second) = repository.sent.map { it.messageId }
        assertEquals(first, UUID.fromString(first).toString())
        assertNotEquals(first, second)
    }

    @Test
    fun message_timestampIsTheCurrentTime() = runTest {
        val before = System.currentTimeMillis()

        sendMessage("hello", Users.ME)

        assertTrue(repository.sent.single().timestampMillis in before..System.currentTimeMillis())
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
        override suspend fun syncIncomingMessages() = Unit
        override suspend fun loadMessages() = Unit
        override suspend fun clearMessages() = Unit
    }
}
