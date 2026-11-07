package com.pedroloureiro.muzzchat.chat.data

import androidx.test.core.app.ApplicationProvider
import com.pedroloureiro.muzzchat.chat.data.local.ChatLocalDataSource
import com.pedroloureiro.muzzchat.chat.data.local.db.entity.MessageEntity
import com.pedroloureiro.muzzchat.chat.data.mock.MockData
import com.pedroloureiro.muzzchat.chat.data.remote.ChatRemoteDataSource
import com.pedroloureiro.muzzchat.chat.data.remote.MockChatRemoteDataSource
import com.pedroloureiro.muzzchat.chat.data.remote.model.ContentDto
import com.pedroloureiro.muzzchat.chat.data.remote.model.MessageDto
import com.pedroloureiro.muzzchat.chat.domain.model.Content
import com.pedroloureiro.muzzchat.chat.domain.model.ContentType
import com.pedroloureiro.muzzchat.chat.domain.model.Message
import com.pedroloureiro.muzzchat.chat.domain.model.MessageDeliveryStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ChatRepositoryImplTest {

    private val local = FakeChatLocalDataSource()
    private val remote = FakeChatRemoteDataSource()
    private val message = Message(
        messageId = "id",
        senderId = "sender",
        content = Content(ContentType.TEXT, "text"),
        timestampMillis = 1L,
        deliveryStatus = MessageDeliveryStatus.SENDING
    )
    private val dto = MessageDto(
        messageId = "id",
        senderId = "sender",
        content = ContentDto(type = "TEXT", value = "text"),
        timestampMillis = 1L
    )

    @Test
    fun `getMessages maps stored entities to domain messages`() = runTest {
        local.insertMessage(message.toEntity())

        assertEquals(listOf(message), repository().getMessages().first())
    }

    @Test
    fun `sendMessage stores the message as SENDING before calling the remote`() = runTest {
        var statusDuringSend: MessageDeliveryStatus? = null
        remote.onSend = {
            statusDuringSend = local.statusOf(message.messageId)
            Result.success(it)
        }

        repository().sendMessage(message)

        assertEquals(MessageDeliveryStatus.SENDING, statusDuringSend)
    }

    @Test
    fun `sendMessage sends the message to the remote as a dto`() = runTest {
        repository().sendMessage(message)

        assertEquals(listOf(dto), remote.sent)
    }

    @Test
    fun `sendMessage marks the message SENT when the remote succeeds`() = runTest {
        repository().sendMessage(message)

        assertEquals(MessageDeliveryStatus.SENT, local.statusOf(message.messageId))
    }

    @Test
    fun `sendMessage leaves the message SENDING when the remote fails`() = runTest {
        remote.onSend = { Result.failure(IllegalStateException("rejected")) }

        repository().sendMessage(message)

        assertEquals(MessageDeliveryStatus.SENDING, local.statusOf(message.messageId))
    }

    @Test
    fun `sent message moves from SENDING to SENT after the mock echo delay`() = runTest {
        val repository = repository(remote = MockChatRemoteDataSource(mockData()))
        startSync(repository)

        launch { repository.sendMessage(message) }

        advanceTimeBy(200)
        assertEquals(MessageDeliveryStatus.SENDING, local.statusOf(message.messageId))
        runCurrent()
        assertEquals(MessageDeliveryStatus.SENT, local.statusOf(message.messageId))
    }

    @Test
    fun `syncIncomingMessages stores incoming messages as SENT`() = runTest {
        startSync(repository())

        remote.incoming.emit(dto)

        assertEquals(listOf(dto.toEntity(MessageDeliveryStatus.SENT)), local.stored.value)
    }

    @Test
    fun `syncIncomingMessages stops storing when its caller cancels it`() = runTest {
        startSync(repository()).cancel()

        remote.incoming.emit(dto)

        assertEquals(emptyList<MessageEntity>(), local.stored.value)
    }

    @Test
    fun `loadMessages stores fetched messages as SENT`() = runTest {
        remote.fetched = Result.success(listOf(dto))

        repository().loadMessages()

        assertEquals(listOf(dto.toEntity(MessageDeliveryStatus.SENT)), local.stored.value)
    }

    @Test
    fun `loadMessages stores nothing when the remote fails`() = runTest {
        remote.fetched = Result.failure(IllegalStateException("unavailable"))

        repository().loadMessages()

        assertEquals(emptyList<MessageEntity>(), local.stored.value)
    }

    @Test
    fun `loadMessages restores a sent message after it was cleared`() = runTest {
        val repository = repository(remote = MockChatRemoteDataSource(mockData()))
        launch { repository.sendMessage(message) }
        advanceUntilIdle()
        repository.clearMessages()

        repository.loadMessages()

        assertTrue(local.stored.value.any { it.messageId == message.messageId })
    }

    @Test
    fun `clearMessages removes every stored message`() = runTest {
        local.insertMessage(message.toEntity())

        repository().clearMessages()

        assertEquals(emptyList<MessageEntity>(), local.stored.value)
    }

    private fun mockData() = MockData(ApplicationProvider.getApplicationContext())

    private fun repository(remote: ChatRemoteDataSource = this.remote) =
        ChatRepositoryImpl(local = local, remote = remote)

    private fun TestScope.startSync(repository: ChatRepositoryImpl) =
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { repository.syncIncomingMessages() }

    private class FakeChatLocalDataSource : ChatLocalDataSource {
        val stored = MutableStateFlow(emptyList<MessageEntity>())

        fun statusOf(messageId: String) = stored.value.single { it.messageId == messageId }.deliveryStatus

        override suspend fun insertMessage(entity: MessageEntity) {
            stored.value = stored.value.filterNot { it.messageId == entity.messageId } + entity
        }

        override suspend fun updateStatus(messageId: String, status: MessageDeliveryStatus) {
            stored.value = stored.value.map {
                if (it.messageId == messageId) it.copy(deliveryStatus = status) else it
            }
        }

        override fun getMessages(): Flow<List<MessageEntity>> = stored

        override suspend fun clearAll() {
            stored.value = emptyList()
        }
    }

    private class FakeChatRemoteDataSource : ChatRemoteDataSource {
        val incoming = MutableSharedFlow<MessageDto>()
        val sent = mutableListOf<MessageDto>()
        var onSend: suspend (MessageDto) -> Result<MessageDto> = { Result.success(it) }
        var fetched: Result<List<MessageDto>> = Result.success(emptyList())

        override suspend fun fetchMessages() = fetched

        override suspend fun sendMessage(dto: MessageDto): Result<MessageDto> {
            sent += dto
            return onSend(dto)
        }

        override fun incomingMessages(): Flow<MessageDto> = incoming
    }
}
