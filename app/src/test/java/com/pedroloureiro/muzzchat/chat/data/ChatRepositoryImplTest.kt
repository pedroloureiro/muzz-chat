package com.pedroloureiro.muzzchat.chat.data

import android.content.Context
import androidx.paging.PagingSource
import androidx.paging.testing.asSnapshot
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pedroloureiro.muzzchat.chat.data.local.RoomChatLocalDataSource
import com.pedroloureiro.muzzchat.chat.data.local.db.MessageDatabase
import com.pedroloureiro.muzzchat.chat.data.local.db.entities.MessageEntity
import com.pedroloureiro.muzzchat.chat.data.remote.ChatRemoteDataSource
import com.pedroloureiro.muzzchat.chat.data.remote.MockChatRemoteDataSource
import com.pedroloureiro.muzzchat.chat.data.remote.models.ContentDto
import com.pedroloureiro.muzzchat.chat.data.remote.models.MessageDto
import com.pedroloureiro.muzzchat.chat.data.remote.models.MessagePageDto
import com.pedroloureiro.muzzchat.chat.data.remote.models.OutgoingMessageDto
import com.pedroloureiro.muzzchat.chat.domain.models.Content
import com.pedroloureiro.muzzchat.chat.domain.models.ContentType
import com.pedroloureiro.muzzchat.chat.domain.models.Message
import com.pedroloureiro.muzzchat.chat.domain.models.MessageWithNext
import com.pedroloureiro.muzzchat.user.domain.User
import com.pedroloureiro.muzzchat.user.domain.UserRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ChatRepositoryImplTest {

    // A direct executor keeps Room on the test thread, so virtual time and Room writes stay in step.
    private val database = Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext<Context>(),
        MessageDatabase::class.java
    ).allowMainThreadQueries().setQueryExecutor { it.run() }.setTransactionExecutor { it.run() }.build()
    private val local = RoomChatLocalDataSource(database)
    private val remote = FakeChatRemoteDataSource()
    private val api = MockChatApi(ApplicationProvider.getApplicationContext(), FakeUserRepository())
    private val mockRemote = MockChatRemoteDataSource(api)
    private val chat = api.loadSeedMessages().first().chatId
    private val message = Message.Pending(
        clientMessageId = "client",
        chatId = chat,
        senderId = "sender",
        content = Content(ContentType.TEXT, "text"),
        timestampMillis = 1L
    )
    private val echo = MessageDto(
        clientMessageId = "client",
        serverMessageId = "server",
        seq = 1,
        chatId = chat,
        senderId = "sender",
        content = ContentDto(type = "TEXT", value = "text"),
        timestampMillis = 2L
    )
    private val otherDeviceMessage = echo.copy(clientMessageId = "other-client", serverMessageId = "other", seq = 2)

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun `getMessages pages the chat's stored rows as domain rows`() = runTest {
        local.insertPending(message.toEntity())
        local.storeIncoming(otherDeviceMessage.toEntity())

        assertEquals(
            listOf(
                MessageWithNext(message, null, null),
                MessageWithNext(otherDeviceMessage.toEntity().toMessage(), message.senderId, message.timestampMillis)
            ),
            repository().getMessages(chat).asSnapshot()
        )
    }

    @Test
    fun `opening a chat with nothing stored shows the newest messages with no gaps`() = runTest {
        val snapshot = repository(mockRemote).getMessages(chat).asSnapshot()

        assertEquals(SEED_SIZE, seqs(snapshot).first())
        assertContiguous(seqs(snapshot))
        assertEquals(SEED_SIZE, local.syncState(chat)?.newestSeq)
    }

    @Test
    fun `scrolling to the end pages through the whole history once and reaches the start`() = runTest {
        repository(mockRemote).getMessages(chat).asSnapshot { appendScrollWhile { true } }

        assertEquals((SEED_SIZE downTo 1L).toList(), storedSeqs())
        assertTrue(local.syncState(chat)!!.reachedStart)
    }

    @Test
    fun `opening a chat again catches up on more than a page of missed messages, keeping stored history`() = runTest {
        val repository = repository(mockRemote)
        val firstOpen = seqs(repository.getMessages(chat).asSnapshot())
        repeat(30) { api.sendAs(chat, OTHER_SENDER, outgoing("missed-$it")) }

        val snapshot = repository.getMessages(chat).asSnapshot()

        assertEquals(SEED_SIZE + 30, seqs(snapshot).first())
        assertEquals(SEED_SIZE + 30, local.syncState(chat)?.newestSeq)
        assertContiguous(storedSeqs())
        assertTrue(storedSeqs().containsAll(firstOpen))
    }

    @Test
    fun `opening a chat stores nothing when the newest page fails`() = runTest {
        remote.pages = Result.failure(IllegalStateException("unavailable"))

        runCatching { repository().getMessages(chat).asSnapshot() }

        assertEquals(emptyList<MessageEntity>(), stored())
        assertNull(local.syncState(chat))
    }

    @Test
    fun `a sent message whose seq follows the stored history moves the chat forward`() = runTest {
        val repository = repository(mockRemote)
        repository.getMessages(chat).asSnapshot()

        repository.sendMessage(message)

        assertEquals(SEED_SIZE + 1, local.syncState(chat)?.newestSeq)
    }

    @Test
    fun `a message that skips a seq is stored and the missing messages are caught up`() = runTest {
        val repository = repository(mockRemote)
        repository.getMessages(chat).asSnapshot()
        repeat(2) { api.sendAs(chat, OTHER_SENDER, outgoing("missed-$it")) }

        repository.sendMessage(message)

        assertEquals(SEED_SIZE + 3, local.syncState(chat)?.newestSeq)
        assertEquals((SEED_SIZE + 3 downTo SEED_SIZE + 1).toList(), storedSeqs().take(3))
    }

    @Test
    fun `a resend with the same sender and clientMessageId leaves one message and moves seq once`() = runTest {
        val repository = repository(mockRemote)
        repository.getMessages(chat).asSnapshot()
        startSync(repository)
        repository.sendMessage(message)

        val resent = mockRemote.sendMessage(chat, message.toDto()).getOrThrow()

        assertEquals(SEED_SIZE + 1, resent.seq)
        assertEquals(listOf(SEED_SIZE + 1), stored().filter { it.clientMessageId == message.clientMessageId }.map { it.seq })
        assertEquals(SEED_SIZE + 1, local.syncState(chat)?.newestSeq)
    }

    @Test
    fun `the same clientMessageId from another sender is stored as a second message`() = runTest {
        val repository = repository(mockRemote)
        repository.getMessages(chat).asSnapshot()
        startSync(repository)
        repository.sendMessage(message)

        api.sendAs(chat, OTHER_SENDER, outgoing(message.clientMessageId))
        repository.getMessages(chat).asSnapshot()

        assertEquals(
            listOf(OTHER_SENDER to SEED_SIZE + 2, message.senderId to SEED_SIZE + 1),
            stored().filter { it.clientMessageId == message.clientMessageId }.map { it.senderId to it.seq }
        )
    }

    @Test
    fun `syncIncomingMessages stores the chat's incoming messages and moves it forward`() = runTest {
        val repository = repository(mockRemote)
        repository.getMessages(chat).asSnapshot()
        startSync(repository)

        mockRemote.sendAsAnother(chat, OTHER_SENDER, "from another device")

        assertEquals(SEED_SIZE + 1, local.syncState(chat)?.newestSeq)
    }

    @Test
    fun `syncIncomingMessages ignores another chat's messages`() = runTest {
        startSync(repository(mockRemote))

        mockRemote.sendAsAnother(OTHER_CHAT, OTHER_SENDER, "elsewhere")

        assertEquals(emptyList<MessageEntity>(), stored(OTHER_CHAT))
    }

    @Test
    fun `sendMessage stores the message as pending before calling the remote`() = runTest {
        var storedDuringSend: List<MessageEntity>? = null
        remote.onSend = {
            storedDuringSend = stored()
            Result.success(echo)
        }

        repository().sendMessage(message)

        assertEquals(listOf(message.toEntity()), storedDuringSend)
    }

    @Test
    fun `sendMessage sends the message to its chat as an outgoing dto`() = runTest {
        repository().sendMessage(message)

        assertEquals(listOf(chat to message.toDto()), remote.sent)
    }

    @Test
    fun `sendMessage confirms the pending message from the send response when no echo arrives`() = runTest {
        repository().sendMessage(message)

        assertEquals(listOf(echo.toEntity()), stored())
    }

    @Test
    fun `an echo that arrives after the send response changes nothing`() = runTest {
        val repository = repository()
        local.storeNewestPage(chat, emptyList(), nextCursor = null, hasMore = false)
        startSync(repository)
        repository.sendMessage(message)
        val afterResponse = stored() to local.syncState(chat)

        remote.incoming.emit(echo)

        assertEquals(afterResponse, stored() to local.syncState(chat))
        assertEquals(1L, local.syncState(chat)?.newestSeq)
    }

    @Test
    fun `sendMessage leaves the message pending when the remote fails`() = runTest {
        remote.onSend = { Result.failure(IllegalStateException("rejected")) }

        repository().sendMessage(message)

        assertEquals(listOf(message.toEntity()), stored())
    }

    @Test
    fun `sent message stays pending until the mock delay, then is confirmed once`() = runTest {
        val repository = repository(mockRemote)
        startSync(repository)

        launch { repository.sendMessage(message) }

        advanceTimeBy(200)
        assertEquals(listOf(null), stored().map { it.serverMessageId })
        runCurrent()
        val confirmed = stored().single()
        assertTrue(confirmed.serverMessageId != null)
        assertEquals(message.clientMessageId, confirmed.clientMessageId)
    }

    @Test
    fun `syncIncomingMessages stores a message from another device as confirmed`() = runTest {
        startSync(repository())

        remote.incoming.emit(otherDeviceMessage)

        assertEquals(listOf(otherDeviceMessage.toEntity()), stored())
    }

    @Test
    fun `syncIncomingMessages confirms the pending message its echo carries the key of`() = runTest {
        local.insertPending(message.toEntity())
        startSync(repository())

        remote.incoming.emit(echo)

        assertEquals(listOf(echo.toEntity()), stored())
    }

    @Test
    fun `syncIncomingMessages stops storing when its caller cancels it`() = runTest {
        startSync(repository()).cancel()

        remote.incoming.emit(echo)

        assertEquals(emptyList<MessageEntity>(), stored())
    }

    private fun repository(remote: ChatRemoteDataSource = this.remote) = ChatRepositoryImpl(local = local, remote = remote)

    private fun TestScope.startSync(repository: ChatRepositoryImpl) =
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { repository.syncIncomingMessages(chat) }

    private suspend fun stored(chatId: String = chat): List<MessageEntity> {
        val params = PagingSource.LoadParams.Refresh<Int>(key = null, loadSize = 1_000, placeholdersEnabled = false)
        val page = local.pagingSource(chatId).load(params) as PagingSource.LoadResult.Page
        return page.data.map { it.message }
    }

    private suspend fun storedSeqs() = stored().mapNotNull { it.seq }

    private fun seqs(snapshot: List<MessageWithNext>) = snapshot.map { (it.message as Message.Confirmed).seq }

    private fun assertContiguous(newestFirst: List<Long>) {
        assertEquals((newestFirst.first() downTo newestFirst.last()).toList(), newestFirst)
    }

    private fun outgoing(clientMessageId: String) = OutgoingMessageDto(
        clientMessageId = clientMessageId,
        content = ContentDto(type = "TEXT", value = "text"),
        timestampMillis = 1L
    )

    private inner class FakeChatRemoteDataSource : ChatRemoteDataSource {
        val incoming = MutableSharedFlow<MessageDto>()
        val sent = mutableListOf<Pair<String, OutgoingMessageDto>>()
        var onSend: suspend (OutgoingMessageDto) -> Result<MessageDto> = { Result.success(echo) }
        var pages: Result<MessagePageDto> = Result.success(MessagePageDto(emptyList(), nextCursor = null, hasMore = false))

        override suspend fun fetchMessagesBefore(chatId: String, beforeSeq: Long?, limit: Int) = pages

        override suspend fun fetchMessagesAfter(chatId: String, afterSeq: Long, limit: Int) = pages

        override suspend fun sendMessage(chatId: String, outgoing: OutgoingMessageDto): Result<MessageDto> {
            sent += chatId to outgoing
            return onSend(outgoing)
        }

        override fun incomingMessages(): Flow<MessageDto> = incoming
    }

    // The signed-in user the mock API sends as, matching the pending message's sender.
    private inner class FakeUserRepository : UserRepository {
        override suspend fun currentUser() = User(message.senderId, "Me")
    }

    private companion object {
        const val OTHER_CHAT = "other-chat"
        const val OTHER_SENDER = "other-sender"
        const val SEED_SIZE = 200L
    }
}
