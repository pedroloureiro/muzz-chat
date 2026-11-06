package com.pedroloureiro.muzzchat.chat.data.remote

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MockChatRemoteDataSourceTest {

    private val remote = MockChatRemoteDataSource()
    private val dto = MessageDto(
        messageId = "id",
        senderId = "sender",
        content = ContentDto(type = "TEXT", value = "text"),
        timestamp = 1L
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
}
