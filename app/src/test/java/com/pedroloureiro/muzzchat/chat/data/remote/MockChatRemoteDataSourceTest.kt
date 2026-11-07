package com.pedroloureiro.muzzchat.chat.data.remote

import androidx.test.core.app.ApplicationProvider
import com.pedroloureiro.muzzchat.chat.data.mock.MockData
import com.pedroloureiro.muzzchat.chat.data.remote.model.ContentDto
import com.pedroloureiro.muzzchat.chat.data.remote.model.MessageDto
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
    private val dto = MessageDto(
        messageId = "id",
        senderId = "sender",
        content = ContentDto(type = "TEXT", value = "text"),
        timestampMillis = 1L
    )

    @Test
    fun `sendMessage echoes the dto on incomingMessages after 200ms`() = runTest {
        val received = mutableListOf<MessageDto>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            remote.incomingMessages().toList(received)
        }

        launch { remote.sendMessage(dto) }

        advanceTimeBy(200)
        assertEquals(emptyList<MessageDto>(), received)
        runCurrent()
        assertEquals(listOf(dto), received)
    }

    @Test
    fun `sendMessage returns the dto`() = runTest {
        assertEquals(Result.success(dto), remote.sendMessage(dto))
    }

    @Test
    fun `fetchMessages returns the seed messages`() = runTest {
        assertEquals(Result.success(mockData.loadSeedMessages()), remote.fetchMessages())
    }

    @Test
    fun `fetchMessages includes sent messages`() = runTest {
        remote.sendMessage(dto)

        assertEquals(Result.success(mockData.loadSeedMessages() + dto), remote.fetchMessages())
    }
}
