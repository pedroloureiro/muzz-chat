package com.pedroloureiro.muzzchat.chat.data.paging

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import com.pedroloureiro.muzzchat.chat.data.local.db.entity.MessageWithNextRow

const val MESSAGE_PAGE_SIZE = 20

@OptIn(ExperimentalPagingApi::class)
class MessageRemoteMediator(
    private val oldestStoredId: suspend () -> String?,
    private val storePage: suspend (beforeId: String?, limit: Int) -> Result<Int>
) : RemoteMediator<Int, MessageWithNextRow>() {

    override suspend fun load(loadType: LoadType, state: PagingState<Int, MessageWithNextRow>): MediatorResult {
        val beforeId = when (loadType) {
            LoadType.REFRESH -> null
            LoadType.PREPEND -> return MediatorResult.Success(endOfPaginationReached = true)
            // Paging only lifts an ended APPEND on REFRESH, so with nothing stored (after a clear),
            // ending here would stop paging after the next loadMessages().
            LoadType.APPEND -> oldestStoredId() ?: return MediatorResult.Success(endOfPaginationReached = false)
        }
        val limit = state.config.pageSize
        return storePage(beforeId, limit).fold(
            onSuccess = { storedCount -> MediatorResult.Success(endOfPaginationReached = storedCount < limit) },
            onFailure = { MediatorResult.Error(it) }
        )
    }
}
