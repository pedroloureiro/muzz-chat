package com.pedroloureiro.muzzchat.chat.data.local.db.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.pedroloureiro.muzzchat.chat.data.local.db.entity.MessageEntity
import com.pedroloureiro.muzzchat.chat.data.local.db.entity.MessageWithNextRow

@Dao
interface MessageDao {
    @Insert
    suspend fun insertPending(entity: MessageEntity)

    @Transaction
    suspend fun confirm(entity: MessageEntity) {
        entity.localId?.let { deleteByLocalId(it) }
        insertReplacingId(entity)
    }

    @Query("DELETE FROM messages WHERE localId = :localId")
    suspend fun deleteByLocalId(localId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReplacingId(entity: MessageEntity)

    @Transaction
    suspend fun confirmAll(entities: List<MessageEntity>) {
        entities.forEach { confirm(it) }
    }

    @Query("SELECT MIN(id) FROM messages")
    suspend fun oldestId(): String?

    // Newest first. Pending rows have a null id, so `id IS NULL DESC` puts them before every confirmed row.
    // The next message in conversation order: for a confirmed row the next confirmed id, else the oldest
    // pending row; for a pending row the next pending row by device time.
    @Query(
        """
        SELECT m.*, n.senderId AS nextSenderId, n.timestampMillis AS nextTimestampMillis
        FROM messages m
        LEFT JOIN messages n ON n.rowId = CASE
            WHEN m.id IS NOT NULL THEN COALESCE(
                (SELECT rowId FROM messages WHERE id > m.id ORDER BY id LIMIT 1),
                (SELECT rowId FROM messages WHERE id IS NULL ORDER BY timestampMillis LIMIT 1)
            )
            ELSE (
                SELECT rowId FROM messages
                WHERE id IS NULL AND timestampMillis > m.timestampMillis
                ORDER BY timestampMillis LIMIT 1
            )
        END
        ORDER BY m.id IS NULL DESC, m.id DESC, m.timestampMillis DESC
        """
    )
    fun pagingSource(): PagingSource<Int, MessageWithNextRow>

    @Query("DELETE FROM messages")
    suspend fun clearAll()
}
