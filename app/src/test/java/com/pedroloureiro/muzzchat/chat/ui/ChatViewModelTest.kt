package com.pedroloureiro.muzzchat.chat.ui

import androidx.paging.PagingData
import androidx.paging.testing.asSnapshot
import com.pedroloureiro.muzzchat.chat.domain.ChatRepository
import com.pedroloureiro.muzzchat.chat.domain.OtherUserDevice
import com.pedroloureiro.muzzchat.chat.domain.SendMessageUseCase
import com.pedroloureiro.muzzchat.chat.domain.models.Content
import com.pedroloureiro.muzzchat.chat.domain.models.ContentType
import com.pedroloureiro.muzzchat.chat.domain.models.Message
import com.pedroloureiro.muzzchat.chat.domain.models.MessageWithNext
import com.pedroloureiro.muzzchat.chat.ui.models.ChatItem
import com.pedroloureiro.muzzchat.chat.ui.models.MessageDirection
import com.pedroloureiro.muzzchat.user.domain.User
import com.pedroloureiro.muzzchat.user.domain.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ChatViewModelTest {

    private val repository = FakeChatRepository()
    private val userRepository = FakeUserRepository()
    private val otherUser = FakeOtherUserDevice()
    private lateinit var viewModel: ChatViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        viewModel = ChatViewModel(CHAT_ID, SARAH.id, repository, userRepository, SendMessageUseCase(repository, userRepository), otherUser)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun init_syncsIncomingMessagesOfTheChat() {
        assertEquals(listOf(CHAT_ID), repository.syncedChatIds)
    }

    @Test
    fun messages_areTheChatsRowsAsChatItems() = runTest {
        val items = flowOf(viewModel.messages.first()).asSnapshot()

        assertEquals(listOf(CHAT_ID), repository.requestedChatIds)
        assertEquals(listOf("${SARAH.id}/1", "header-${SARAH.id}/1"), items.map(ChatItem::key))
    }

    @Test
    fun messages_fromTheCurrentUserAreOutgoing() = runTest {
        userRepository.currentUser = SARAH

        val items = flowOf(viewModel.messages.first()).asSnapshot()

        assertEquals(MessageDirection.OUTGOING, items.filterIsInstance<ChatItem.Message>().single().direction)
    }

    @Test
    fun messages_fromAnyoneElseAreIncoming() = runTest {
        val items = flowOf(viewModel.messages.first()).asSnapshot()

        assertEquals(MessageDirection.INCOMING, items.filterIsInstance<ChatItem.Message>().single().direction)
    }

    @Test
    fun onInputChanged_updatesInputText() {
        viewModel.onInputChanged("hello")

        assertEquals("hello", viewModel.uiState.value.inputText)
    }

    @Test
    fun send_sendsInputAsTheCurrentUser() {
        viewModel.onInputChanged("hello")

        viewModel.send()

        val sent = repository.sent.single()
        assertEquals(ME.id, sent.senderId)
        assertEquals(CHAT_ID, sent.chatId)
        assertEquals("hello", sent.content.value)
    }

    @Test
    fun sendAsOther_sendsTrimmedInputAsTheOtherUserThroughTheirDevice() {
        viewModel.onInputChanged("  hello  ")

        viewModel.sendAsOther()

        assertEquals(listOf(Triple(CHAT_ID, SARAH.id, "hello")), otherUser.sent)
        assertEquals(emptyList<Message.Pending>(), repository.sent)
    }

    @Test
    fun sendAsOther_clearsInput() {
        viewModel.onInputChanged("hello")

        viewModel.sendAsOther()

        assertEquals("", viewModel.uiState.value.inputText)
    }

    @Test
    fun send_clearsInputBeforeTheSendFinishes() {
        viewModel.onInputChanged("hello")

        viewModel.send()

        assertEquals(1, repository.sent.size)
        assertEquals("", viewModel.uiState.value.inputText)
    }

    @Test
    fun send_blankInput_isNoOp() {
        viewModel.onInputChanged("  \n ")

        viewModel.send()

        assertEquals(emptyList<Message.Pending>(), repository.sent)
        assertEquals("  \n ", viewModel.uiState.value.inputText)
    }

    @Test
    fun sendAsOther_blankInput_isNoOp() {
        viewModel.onInputChanged("  \n ")

        viewModel.sendAsOther()

        assertEquals(emptyList<Triple<String, String, String>>(), otherUser.sent)
        assertEquals("  \n ", viewModel.uiState.value.inputText)
    }

    private class FakeChatRepository : ChatRepository {
        val requestedChatIds = mutableListOf<String>()
        val syncedChatIds = mutableListOf<String>()
        val sent = mutableListOf<Message.Pending>()

        override fun getMessages(chatId: String): Flow<PagingData<MessageWithNext>> {
            requestedChatIds += chatId
            return flowOf(PagingData.from(listOf(MessageWithNext(sarahMessage, null, null))))
        }

        override suspend fun syncIncomingMessages(chatId: String) {
            syncedChatIds += chatId
        }

        // Never finishes, as if the echo had not returned yet.
        override suspend fun sendMessage(message: Message.Pending) {
            sent += message
            awaitCancellation()
        }
    }

    private class FakeUserRepository : UserRepository {
        var currentUser = ME

        override suspend fun currentUser() = currentUser
    }

    private class FakeOtherUserDevice : OtherUserDevice {
        val sent = mutableListOf<Triple<String, String, String>>()

        override suspend fun sendAsAnother(chatId: String, senderId: String, text: String) {
            sent += Triple(chatId, senderId, text)
        }
    }

    private companion object {
        const val CHAT_ID = "chat"
        val ME = User("me", "Me")
        val SARAH = User("sarah", "Sarah")

        val sarahMessage = Message.Confirmed(
            clientMessageId = "1",
            serverMessageId = "server-1",
            seq = 1L,
            chatId = CHAT_ID,
            senderId = SARAH.id,
            content = Content(ContentType.TEXT, "hi"),
            timestampMillis = 0L
        )
    }
}
