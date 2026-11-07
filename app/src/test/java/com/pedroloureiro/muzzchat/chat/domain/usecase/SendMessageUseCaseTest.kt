package com.pedroloureiro.muzzchat.chat.domain.usecase

import androidx.paging.PagingData
import com.pedroloureiro.muzzchat.chat.domain.model.Content
import com.pedroloureiro.muzzchat.chat.domain.model.ContentType
import com.pedroloureiro.muzzchat.chat.domain.model.Message
import com.pedroloureiro.muzzchat.chat.domain.model.MessageWithNext
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

        assertEquals(emptyList<Message.Pending>(), repository.sent)
    }

    @Test
    fun textIsTrimmed() = runTest {
        sendMessage("  hello  ", Users.ME)

        assertEquals("hello", repository.sent.single().content.value)
    }

    @Test
    fun message_localIdIsAUniqueUuid() = runTest {
        sendMessage("hello", Users.ME)
        sendMessage("hello", Users.ME)

        val (first, second) = repository.sent.map { it.localId }
        assertEquals(first, UUID.fromString(first).toString())
        assertNotEquals(first, second)
    }

    @Test
    fun message_timestampIsTheDeviceTime() = runTest {
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
    fun message_isText() = runTest {
        sendMessage("hello", Users.ME)

        assertEquals(Content(ContentType.TEXT, "hello"), repository.sent.single().content)
    }

    private class FakeChatRepository : ChatRepository {
        val sent = mutableListOf<Message.Pending>()

        override fun getMessages(): Flow<PagingData<MessageWithNext>> = emptyFlow()
        override suspend fun sendMessage(message: Message.Pending) {
            sent += message
        }
        override suspend fun syncIncomingMessages() = Unit
        override suspend fun loadMessages() = Unit
        override suspend fun clearMessages() = Unit
    }
}
