package com.pedroloureiro.muzzchat.chat.data.remote

import androidx.test.core.app.ApplicationProvider
import com.pedroloureiro.muzzchat.chat.data.mock.MockData
import com.pedroloureiro.muzzchat.chat.data.remote.model.ContentDto
import com.pedroloureiro.muzzchat.chat.data.remote.model.MessageDto
import com.pedroloureiro.muzzchat.chat.data.remote.model.OutgoingMessageDto
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
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

        launch { remote.sendMessage(outgoing) }

        advanceTimeBy(200)
        assertEquals(emptyList<MessageDto>(), received)
        runCurrent()
        assertEquals(listOf(mockData.messages().last()), received)
    }

    @Test
    fun `sendMessage returns the stored message with its localId`() = runTest {
        val result = remote.sendMessage(outgoing).getOrThrow()

        assertEquals(mockData.messages().last(), result)
        assertEquals("local", result.localId)
    }

    @Test
    fun `fetchMessages returns the mock's page before the cursor`() = runTest {
        val cursor = mockData.page(beforeId = null, limit = 1).single().id

        assertEquals(Result.success(mockData.page(cursor, limit = 3)), remote.fetchMessages(cursor, limit = 3))
    }

    @Test
    fun `fetchMessages puts sent messages in the newest page`() = runTest {
        val sent = remote.sendMessage(outgoing).getOrThrow()

        assertEquals(sent, remote.fetchMessages(beforeId = null, limit = 1).getOrThrow().single())
    }
}
