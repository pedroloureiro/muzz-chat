package com.pedroloureiro.muzzchat.chat.data

import androidx.test.core.app.ApplicationProvider
import com.pedroloureiro.muzzchat.chat.data.local.ChatLocalDataSource
import com.pedroloureiro.muzzchat.chat.data.local.db.entity.MessageEntity
import com.pedroloureiro.muzzchat.chat.data.mock.MockData
import com.pedroloureiro.muzzchat.chat.data.remote.ChatRemoteDataSource
import com.pedroloureiro.muzzchat.chat.data.remote.MockChatRemoteDataSource
import com.pedroloureiro.muzzchat.chat.data.remote.model.ContentDto
import com.pedroloureiro.muzzchat.chat.data.remote.model.MessageDto
import com.pedroloureiro.muzzchat.chat.data.remote.model.OutgoingMessageDto
import com.pedroloureiro.muzzchat.chat.domain.model.Content
import com.pedroloureiro.muzzchat.chat.domain.model.ContentType
import com.pedroloureiro.muzzchat.chat.domain.model.Message
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
    private val message = Message.Pending(
        localId = "local",
        senderId = "sender",
        content = Content(ContentType.TEXT, "text"),
        timestampMillis = 1L
    )
    private val echo = MessageDto(
        id = "server",
        localId = "local",
        senderId = "sender",
        content = ContentDto(type = "TEXT", value = "text"),
        timestampMillis = 2L
    )
    private val otherDeviceMessage = echo.copy(id = "other", localId = null)

    @Test
    fun `getMessages maps stored entities to domain messages`() = runTest {
        local.insertPending(message.toEntity())
        local.confirm(otherDeviceMessage.toEntity())

        assertEquals(
            listOf(message, otherDeviceMessage.toEntity().toMessage()),
            repository().getMessages().first()
        )
    }

    @Test
    fun `sendMessage stores the message as pending before calling the remote`() = runTest {
        var storedDuringSend: List<MessageEntity>? = null
        remote.onSend = {
            storedDuringSend = local.stored.value
            Result.success(echo)
        }

        repository().sendMessage(message)

        assertEquals(listOf(message.toEntity()), storedDuringSend)
    }

    @Test
    fun `sendMessage sends the message to the remote as an outgoing dto`() = runTest {
        repository().sendMessage(message)

        assertEquals(listOf(message.toDto()), remote.sent)
    }

    @Test
    fun `sendMessage replaces the pending message with the confirmed one when the remote succeeds`() = runTest {
        repository().sendMessage(message)

        assertEquals(listOf(echo.toEntity()), local.stored.value)
    }

    @Test
    fun `sendMessage leaves the message pending when the remote fails`() = runTest {
        remote.onSend = { Result.failure(IllegalStateException("rejected")) }

        repository().sendMessage(message)

        assertEquals(listOf(message.toEntity()), local.stored.value)
    }

    @Test
    fun `sent message stays pending until the mock echo delay, then is confirmed once`() = runTest {
        val repository = repository(remote = MockChatRemoteDataSource(mockData()))
        startSync(repository)

        launch { repository.sendMessage(message) }

        advanceTimeBy(200)
        assertEquals(listOf(null), local.stored.value.map { it.id })
        runCurrent()
        val confirmed = local.stored.value.single()
        assertTrue(confirmed.id != null)
        assertEquals(message.localId, confirmed.localId)
    }

    @Test
    fun `syncIncomingMessages stores a message from another device as confirmed`() = runTest {
        startSync(repository())

        remote.incoming.emit(otherDeviceMessage)

        assertEquals(listOf(otherDeviceMessage.toEntity()), local.stored.value)
    }

    @Test
    fun `syncIncomingMessages replaces the pending message its echo carries the localId of`() = runTest {
        local.insertPending(message.toEntity())
        startSync(repository())

        remote.incoming.emit(echo)

        assertEquals(listOf(echo.toEntity()), local.stored.value)
    }

    @Test
    fun `syncIncomingMessages stops storing when its caller cancels it`() = runTest {
        startSync(repository()).cancel()

        remote.incoming.emit(echo)

        assertEquals(emptyList<MessageEntity>(), local.stored.value)
    }

    @Test
    fun `loadMessages stores fetched messages as confirmed`() = runTest {
        local.insertPending(message.toEntity())
        remote.fetched = Result.success(listOf(echo, otherDeviceMessage))

        repository().loadMessages()

        assertEquals(listOf(echo.toEntity(), otherDeviceMessage.toEntity()), local.stored.value)
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

        assertTrue(local.stored.value.any { it.localId == message.localId && it.id != null })
    }

    @Test
    fun `clearMessages removes every stored message`() = runTest {
        local.insertPending(message.toEntity())

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

        override suspend fun insertPending(entity: MessageEntity) {
            stored.value += entity
        }

        override suspend fun confirm(entity: MessageEntity) {
            stored.value = stored.value.filterNot {
                (entity.localId != null && it.localId == entity.localId) || it.id == entity.id
            } + entity
        }

        override fun getMessages(): Flow<List<MessageEntity>> = stored

        override suspend fun clearAll() {
            stored.value = emptyList()
        }
    }

    private inner class FakeChatRemoteDataSource : ChatRemoteDataSource {
        val incoming = MutableSharedFlow<MessageDto>()
        val sent = mutableListOf<OutgoingMessageDto>()
        var onSend: suspend (OutgoingMessageDto) -> Result<MessageDto> = { Result.success(echo) }
        var fetched: Result<List<MessageDto>> = Result.success(emptyList())

        override suspend fun fetchMessages() = fetched

        override suspend fun sendMessage(outgoing: OutgoingMessageDto): Result<MessageDto> {
            sent += outgoing
            return onSend(outgoing)
        }

        override fun incomingMessages(): Flow<MessageDto> = incoming
    }
}
