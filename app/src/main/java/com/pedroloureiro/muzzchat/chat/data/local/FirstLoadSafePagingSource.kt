package com.pedroloureiro.muzzchat.chat.data.local

import androidx.paging.PagingSource
import androidx.paging.PagingState
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

// Room's LimitOffsetPagingSource acts on a table change only after its first load (room-paging 2.8.5,
// CommonLimitOffsetImpl), so a write that commits during that load leaves the page stale. This watches
// the table during the first load and invalidates once the load returns. Later changes are Room's.
class FirstLoadSafePagingSource<Value : Any>(
    private val room: PagingSource<Int, Value>,
    private val tableChanges: Flow<Set<String>>,
) : PagingSource<Int, Value>() {
    private val firstLoadStarted = AtomicBoolean(false)

    init {
        room.registerInvalidatedCallback(::invalidate)
        registerInvalidatedCallback(room::invalidate)
    }

    override val jumpingSupported: Boolean
        get() = room.jumpingSupported

    override fun getRefreshKey(state: PagingState<Int, Value>): Int? = room.getRefreshKey(state)

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Value> {
        if (firstLoadStarted.getAndSet(true)) return room.load(params)
        return coroutineScope {
            val changedDuringLoad = AtomicBoolean(false)
            val watcher = launch(start = CoroutineStart.UNDISPATCHED) {
                tableChanges.collect { changedDuringLoad.set(true) }
            }
            room.load(params).also {
                watcher.cancel()
                if (changedDuringLoad.get()) invalidate()
            }
        }
    }
}
