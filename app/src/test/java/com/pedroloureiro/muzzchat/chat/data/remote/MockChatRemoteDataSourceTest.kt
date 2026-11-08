package com.pedroloureiro.muzzchat.chat.data.remote

import androidx.test.core.app.ApplicationProvider
import com.pedroloureiro.muzzchat.chat.data.mock.MOCK_SARAH_CHAT_ID
import com.pedroloureiro.muzzchat.chat.data.mock.MockData
import com.pedroloureiro.muzzchat.chat.data.remote.model.ContentDto
import com.pedroloureiro.muzzchat.chat.data.remote.model.MessageDto
import com.pedroloureiro.muzzchat.chat.data.remote.model.MessagePageDto
import com.pedroloureiro.muzzchat.chat.data.remote.model.OutgoingMessageDto
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class MockChatRemoteDataSourceTest {

    private val mockData = MockData(ApplicationProvider.getApplicationContext())
    private val remote = MockChatRemoteDataSource(mockData)
    private val outgoing = OutgoingMessageDto(
        localId = "local",
        senderId = "sender",
        content = ContentDto(type = "TEXT", value = "text"),
        timestampMillis = 1L
    )

    @Test
    fun `sendMessage echoes the stored message on incomingMessages after 200ms`() = runTest {
        val received = mutableListOf<MessageDto>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            remote.incomingMessages().toList(received)
        }

        launch { remote.sendMessage(MOCK_SARAH_CHAT_ID, outgoing) }

        advanceTimeBy(200)
        assertEquals(emptyList<MessageDto>(), received)
        runCurrent()
        assertEquals(listOf(mockData.messages().last()), received)
    }

    @Test
    fun `sendMessage returns the message stored in the chat with its localId`() = runTest {
        val result = remote.sendMessage(MOCK_SARAH_CHAT_ID, outgoing).getOrThrow()

        assertEquals(mockData.messages().last(), result)
        assertEquals("local", result.localId)
        assertEquals(MOCK_SARAH_CHAT_ID, result.chatId)
    }

    @Test
    fun `fetchMessagesBefore answers after 200ms`() = runTest {
        var page: Result<MessagePageDto>? = null

        launch { page = remote.fetchMessagesBefore(MOCK_SARAH_CHAT_ID, beforeSeq = null, limit = 1) }

        advanceTimeBy(200)
        assertNull(page)
        runCurrent()
        assertEquals(Result.success(mockData.pageBefore(MOCK_SARAH_CHAT_ID, beforeSeq = null, limit = 1)), page)
    }

    @Test
    fun `fetchMessagesBefore returns the mock's page before the cursor`() = runTest {
        assertEquals(
            Result.success(mockData.pageBefore(MOCK_SARAH_CHAT_ID, beforeSeq = 10, limit = 3)),
            remote.fetchMessagesBefore(MOCK_SARAH_CHAT_ID, beforeSeq = 10, limit = 3)
        )
    }

    @Test
    fun `fetchMessagesAfter answers after 200ms with the mock's page after the cursor`() = runTest {
        var page: Result<MessagePageDto>? = null

        launch { page = remote.fetchMessagesAfter(MOCK_SARAH_CHAT_ID, afterSeq = 10, limit = 3) }

        advanceTimeBy(200)
        assertNull(page)
        runCurrent()
        assertEquals(Result.success(mockData.pageAfter(MOCK_SARAH_CHAT_ID, afterSeq = 10, limit = 3)), page)
    }

    @Test
    fun `fetchMessagesBefore puts sent messages in the newest page`() = runTest {
        val sent = remote.sendMessage(MOCK_SARAH_CHAT_ID, outgoing).getOrThrow()

        assertEquals(sent, remote.fetchMessagesBefore(MOCK_SARAH_CHAT_ID, beforeSeq = null, limit = 1).getOrThrow().messages.single())
    }
}
