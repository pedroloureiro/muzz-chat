package com.pedroloureiro.muzzchat.chat.data.paging

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import com.pedroloureiro.muzzchat.chat.data.local.db.entity.ChatSyncEntity
import com.pedroloureiro.muzzchat.chat.data.local.db.entity.MessageWithNextRow

const val MESSAGE_PAGE_SIZE = 20

@OptIn(ExperimentalPagingApi::class)
class MessageRemoteMediator(
    private val syncState: suspend () -> ChatSyncEntity?,
    private val loadNewest: suspend () -> Result<Unit>,
    private val catchUp: suspend () -> Result<Unit>,
    private val loadOlder: suspend (beforeSeq: Long) -> Result<Unit>
) : RemoteMediator<Int, MessageWithNextRow>() {

    override suspend fun load(loadType: LoadType, state: PagingState<Int, MessageWithNextRow>): MediatorResult {
        val loaded = when (loadType) {
            LoadType.REFRESH -> if (syncState() == null) loadNewest() else catchUp()
            LoadType.PREPEND -> return MediatorResult.Success(endOfPaginationReached = true)
            LoadType.APPEND -> {
                val sync = syncState()
                val beforeSeq = sync?.oldestSeq?.takeUnless { sync.reachedStart }
                    ?: return MediatorResult.Success(endOfPaginationReached = true)
                loadOlder(beforeSeq)
            }
        }
        return loaded.fold(
            onSuccess = { MediatorResult.Success(endOfPaginationReached = syncState()?.reachedStart ?: false) },
            onFailure = { MediatorResult.Error(it) }
        )
    }
}
