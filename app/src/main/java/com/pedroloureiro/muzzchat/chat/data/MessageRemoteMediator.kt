package com.pedroloureiro.muzzchat.chat.data

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingConfig
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import com.pedroloureiro.muzzchat.chat.data.local.db.entities.ChatSyncEntity
import com.pedroloureiro.muzzchat.chat.data.local.db.entities.MessageWithNextRow

val MESSAGE_PAGING_CONFIG = PagingConfig(pageSize = 20, prefetchDistance = 20, initialLoadSize = 40)

// More than half of a first load, so the generation a stored page starts stops short of the end of stored
// history and requests the next APPEND only later, once this one has returned. Paging drops it otherwise.
const val REMOTE_PAGE_SIZE = 60

@OptIn(ExperimentalPagingApi::class)
class MessageRemoteMediator(
    private val syncState: suspend () -> ChatSyncEntity?,
    private val loadNewest: suspend () -> Result<Boolean>,
    private val catchUp: suspend () -> Result<Unit>,
    private val loadOlder: suspend (beforeSeq: Long) -> Result<Boolean>
) : RemoteMediator<Int, MessageWithNextRow>() {

    override suspend fun load(loadType: LoadType, state: PagingState<Int, MessageWithNextRow>): MediatorResult =
        when (loadType) {
            LoadType.REFRESH -> refresh()
            LoadType.PREPEND -> MediatorResult.Success(endOfPaginationReached = true)
            LoadType.APPEND -> append()
        }

    // Each load returns as soon as its page is stored, with whether it reached the start, and reads
    // nothing after. Storing a page starts a new generation that may request the next APPEND, and
    // Paging drops that request if it arrives before the running APPEND returns.
    private suspend fun refresh(): MediatorResult {
        val sync = syncState() ?: return loadNewest().toMediatorResult()
        return catchUp().map { sync.reachedStart }.toMediatorResult()
    }

    private suspend fun append(): MediatorResult {
        val sync = syncState()
        val beforeSeq = sync?.oldestSeq?.takeUnless { sync.reachedStart }
            ?: return MediatorResult.Success(endOfPaginationReached = true)
        return loadOlder(beforeSeq).toMediatorResult()
    }

    private fun Result<Boolean>.toMediatorResult(): MediatorResult = fold(
        onSuccess = { reachedStart -> MediatorResult.Success(endOfPaginationReached = reachedStart) },
        onFailure = { MediatorResult.Error(it) }
    )
}
