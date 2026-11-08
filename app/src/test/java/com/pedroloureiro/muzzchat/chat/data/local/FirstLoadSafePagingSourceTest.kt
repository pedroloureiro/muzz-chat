package com.pedroloureiro.muzzchat.chat.data.local

import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingSource.LoadParams
import androidx.paging.PagingSource.LoadResult
import androidx.paging.PagingState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FirstLoadSafePagingSourceTest {

    private val room = GatedPagingSource()
    private val tableChanges = MutableSharedFlow<Set<String>>()
    private val source = FirstLoadSafePagingSource(room, tableChanges)

    @Test
    fun `a change during the first load invalidates the source once the load returns`() = runTest {
        val load = async { source.load(REFRESH) }
        runCurrent()

        tableChanges.emit(setOf(TABLE))
        assertFalse(source.invalid)
        room.gate.complete(Unit)
        load.await()

        assertTrue(source.invalid)
    }

    @Test
    fun `no change during the first load leaves the source valid`() = runTest {
        room.gate.complete(Unit)

        source.load(REFRESH)

        assertFalse(source.invalid)
    }

    @Test
    fun `a change after the first load is left to Room`() = runTest {
        room.gate.complete(Unit)
        source.load(REFRESH)

        tableChanges.emit(setOf(TABLE))

        assertFalse(source.invalid)
    }

    @Test
    fun `invalidating Room's source invalidates the wrapper`() {
        room.invalidate()

        assertTrue(source.invalid)
    }

    @Test
    fun `invalidating the wrapper invalidates Room's source`() {
        source.invalidate()

        assertTrue(room.invalid)
    }

    @Test
    fun `loads and refresh keys come from Room's source`() = runTest {
        room.gate.complete(Unit)

        assertEquals(PAGE, source.load(REFRESH))
        assertEquals(REFRESH_KEY, source.getRefreshKey(PagingState(emptyList(), null, PagingConfig(1), 0)))
    }

    private class GatedPagingSource : PagingSource<Int, String>() {
        val gate = CompletableDeferred<Unit>()

        override fun getRefreshKey(state: PagingState<Int, String>) = REFRESH_KEY

        override suspend fun load(params: LoadParams<Int>): LoadResult<Int, String> {
            gate.await()
            return PAGE
        }
    }

    private companion object {
        const val TABLE = "messages"
        const val REFRESH_KEY = 7
        val REFRESH = LoadParams.Refresh<Int>(key = null, loadSize = 1, placeholdersEnabled = false)
        val PAGE = LoadResult.Page<Int, String>(listOf("row"), prevKey = null, nextKey = null)
    }
}
