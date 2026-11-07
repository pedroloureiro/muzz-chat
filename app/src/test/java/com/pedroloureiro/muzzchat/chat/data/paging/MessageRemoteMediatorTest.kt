package com.pedroloureiro.muzzchat.chat.data.paging

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingConfig
import androidx.paging.PagingState
import androidx.paging.RemoteMediator.MediatorResult
import com.pedroloureiro.muzzchat.chat.data.local.db.entity.MessageWithNextRow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalPagingApi::class)
class MessageRemoteMediatorTest {

    private data class Request(val beforeId: String?, val limit: Int)

    private val requests = mutableListOf<Request>()
    private var oldestStoredId: String? = "oldest"
    private var storedCount: Result<Int> = Result.success(PAGE_SIZE)
    private val state = PagingState<Int, MessageWithNextRow>(
        pages = emptyList(),
        anchorPosition = null,
        config = PagingConfig(pageSize = PAGE_SIZE),
        leadingPlaceholderCount = 0
    )

    @Test
    fun `refresh fetches the newest page`() = runTest {
        mediator().load(LoadType.REFRESH, state)

        assertEquals(listOf(Request(beforeId = null, limit = PAGE_SIZE)), requests)
    }

    @Test
    fun `append fetches the page before the oldest stored id`() = runTest {
        mediator().load(LoadType.APPEND, state)

        assertEquals(listOf(Request(beforeId = "oldest", limit = PAGE_SIZE)), requests)
    }

    @Test
    fun `append with no confirmed message stored fetches nothing and leaves pagination open`() = runTest {
        oldestStoredId = null

        val result = mediator().load(LoadType.APPEND, state)

        assertEquals(emptyList<Request>(), requests)
        assertEndOfPagination(false, result)
    }

    @Test
    fun `prepend ends at once without fetching`() = runTest {
        val result = mediator().load(LoadType.PREPEND, state)

        assertEquals(emptyList<Request>(), requests)
        assertEndOfPagination(true, result)
    }

    @Test
    fun `a full page leaves pagination open`() = runTest {
        assertEndOfPagination(false, mediator().load(LoadType.APPEND, state))
    }

    @Test
    fun `a short page ends pagination`() = runTest {
        storedCount = Result.success(PAGE_SIZE - 1)

        assertEndOfPagination(true, mediator().load(LoadType.APPEND, state))
    }

    @Test
    fun `a failed fetch is reported as an error`() = runTest {
        val failure = IllegalStateException("unavailable")
        storedCount = Result.failure(failure)

        val result = mediator().load(LoadType.REFRESH, state)

        assertEquals(failure, (result as MediatorResult.Error).throwable)
    }

    private fun mediator() = MessageRemoteMediator(
        oldestStoredId = { oldestStoredId },
        storePage = { beforeId, limit ->
            requests += Request(beforeId, limit)
            storedCount
        }
    )

    private fun assertEndOfPagination(expected: Boolean, result: MediatorResult) {
        assertTrue(result is MediatorResult.Success)
        assertEquals(expected, (result as MediatorResult.Success).endOfPaginationReached)
    }

    private companion object {
        const val PAGE_SIZE = 3
    }
}
