package com.pedroloureiro.muzzchat.chat.domain

import androidx.paging.PagingData
import com.pedroloureiro.muzzchat.chat.domain.models.Content
import com.pedroloureiro.muzzchat.chat.domain.models.ContentType
import com.pedroloureiro.muzzchat.chat.domain.models.Message
import com.pedroloureiro.muzzchat.chat.domain.models.MessageWithNext
import com.pedroloureiro.muzzchat.chat.domain.models.Users
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
        sendMessage(CHAT_ID, "  \n\t ")

        assertEquals(emptyList<Message.Pending>(), repository.sent)
    }

    @Test
    fun textIsTrimmed() = runTest {
        sendMessage(CHAT_ID, "  hello  ")

        assertEquals("hello", repository.sent.single().content.value)
    }

    @Test
    fun message_clientMessageIdIsAUniqueUuid() = runTest {
        sendMessage(CHAT_ID, "hello")
        sendMessage(CHAT_ID, "hello")

        val (first, second) = repository.sent.map { it.clientMessageId }
        assertEquals(first, UUID.fromString(first).toString())
        assertNotEquals(first, second)
    }

    @Test
    fun message_timestampIsTheDeviceTime() = runTest {
        val before = System.currentTimeMillis()

        sendMessage(CHAT_ID, "hello")

        assertTrue(repository.sent.single().timestampMillis in before..System.currentTimeMillis())
    }

    @Test
    fun message_isFromMe() = runTest {
        sendMessage(CHAT_ID, "hello")

        assertEquals(Users.ME.id, repository.sent.single().senderId)
    }

    @Test
    fun message_isInTheChat() = runTest {
        sendMessage(CHAT_ID, "hello")

        assertEquals(CHAT_ID, repository.sent.single().chatId)
    }

    @Test
    fun message_isText() = runTest {
        sendMessage(CHAT_ID, "hello")

        assertEquals(Content(ContentType.TEXT, "hello"), repository.sent.single().content)
    }

    private class FakeChatRepository : ChatRepository {
        val sent = mutableListOf<Message.Pending>()

        override fun getMessages(chatId: String): Flow<PagingData<MessageWithNext>> = emptyFlow()
        override suspend fun sendMessage(message: Message.Pending) {
            sent += message
        }
        override suspend fun syncIncomingMessages(chatId: String) = Unit
    }

    private companion object {
        const val CHAT_ID = "chat"
    }
}
