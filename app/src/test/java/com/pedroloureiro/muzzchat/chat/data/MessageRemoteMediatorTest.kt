package com.pedroloureiro.muzzchat.chat.data

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingConfig
import androidx.paging.PagingState
import androidx.paging.RemoteMediator.MediatorResult
import com.pedroloureiro.muzzchat.chat.data.local.db.entities.ChatSyncEntity
import com.pedroloureiro.muzzchat.chat.data.local.db.entities.MessageWithNextRow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalPagingApi::class)
class MessageRemoteMediatorTest {

    private val loads = mutableListOf<String>()
    private var syncState: ChatSyncEntity? = null
    private var syncStateReads = 0
    private var reachedStart = false
    private var failure: Throwable? = null
    private val state = PagingState<Int, MessageWithNextRow>(
        pages = emptyList(),
        anchorPosition = null,
        config = PagingConfig(pageSize = 3),
        leadingPlaceholderCount = 0
    )

    @Test
    fun `refresh with no sync state loads the newest page`() = runTest {
        mediator().load(LoadType.REFRESH, state)

        assertEquals(listOf("newest"), loads)
    }

    @Test
    fun `refresh with a sync state catches up`() = runTest {
        syncState = sync(oldestSeq = 5, reachedStart = false)

        mediator().load(LoadType.REFRESH, state)

        assertEquals(listOf("catch-up"), loads)
    }

    @Test
    fun `append loads the page before the oldest seq`() = runTest {
        syncState = sync(oldestSeq = 5, reachedStart = false)

        mediator().load(LoadType.APPEND, state)

        assertEquals(listOf("older than 5"), loads)
    }

    @Test
    fun `append ends at once when the start is reached`() = runTest {
        syncState = sync(oldestSeq = 1, reachedStart = true)

        val result = mediator().load(LoadType.APPEND, state)

        assertEquals(emptyList<String>(), loads)
        assertEndOfPagination(true, result)
    }

    @Test
    fun `append with no sync state ends without loading`() = runTest {
        val result = mediator().load(LoadType.APPEND, state)

        assertEquals(emptyList<String>(), loads)
        assertEndOfPagination(true, result)
    }

    @Test
    fun `prepend ends at once without loading`() = runTest {
        val result = mediator().load(LoadType.PREPEND, state)

        assertEquals(emptyList<String>(), loads)
        assertEndOfPagination(true, result)
    }

    @Test
    fun `pagination ends when an older page reaches the start`() = runTest {
        syncState = sync(oldestSeq = 5, reachedStart = false)
        reachedStart = true

        assertEndOfPagination(true, mediator().load(LoadType.APPEND, state))
    }

    @Test
    fun `pagination ends when the newest page is the whole history`() = runTest {
        reachedStart = true

        assertEndOfPagination(true, mediator().load(LoadType.REFRESH, state))
    }

    @Test
    fun `pagination stays open after a catch-up while older history remains`() = runTest {
        syncState = sync(oldestSeq = 5, reachedStart = false)

        assertEndOfPagination(false, mediator().load(LoadType.REFRESH, state))
    }

    @Test
    fun `pagination ends after a catch-up when the start was already reached`() = runTest {
        syncState = sync(oldestSeq = 1, reachedStart = true)

        assertEndOfPagination(true, mediator().load(LoadType.REFRESH, state))
    }

    @Test
    fun `append reads the sync state only before loading`() = runTest {
        syncState = sync(oldestSeq = 5, reachedStart = false)

        mediator().load(LoadType.APPEND, state)

        assertEquals(1, syncStateReads)
    }

    @Test
    fun `a server page holds more than half of a first load`() {
        assertTrue(REMOTE_PAGE_SIZE > MESSAGE_PAGING_CONFIG.initialLoadSize / 2)
    }

    @Test
    fun `a failed load is reported as an error`() = runTest {
        val error = IllegalStateException("unavailable")
        failure = error

        val result = mediator().load(LoadType.REFRESH, state)

        assertEquals(error, (result as MediatorResult.Error).throwable)
    }

    private fun mediator() = MessageRemoteMediator(
        syncState = { syncStateReads++; syncState },
        loadNewest = { record("newest").map { reachedStart } },
        catchUp = { record("catch-up") },
        loadOlder = { beforeSeq -> record("older than $beforeSeq").map { reachedStart } }
    )

    private fun record(load: String): Result<Unit> {
        loads += load
        return failure?.let { Result.failure(it) } ?: Result.success(Unit)
    }

    private fun sync(oldestSeq: Long, reachedStart: Boolean) =
        ChatSyncEntity(chatId = "chat", oldestSeq = oldestSeq, newestSeq = 10, reachedStart = reachedStart)

    private fun assertEndOfPagination(expected: Boolean, result: MediatorResult) {
        assertTrue(result is MediatorResult.Success)
        assertEquals(expected, (result as MediatorResult.Success).endOfPaginationReached)
    }
}
