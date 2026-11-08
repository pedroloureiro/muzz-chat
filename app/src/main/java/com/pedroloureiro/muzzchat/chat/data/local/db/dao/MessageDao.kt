package com.pedroloureiro.muzzchat.chat.data.local.db.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.pedroloureiro.muzzchat.chat.data.local.db.entity.ChatSyncEntity
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
    suspend fun storeNewestPage(chatId: String, entities: List<MessageEntity>, nextCursor: Long?, hasMore: Boolean) {
        entities.forEach { confirm(it) }
        val newestSeq = entities.mapNotNull { it.seq }.maxOrNull() ?: 0
        upsertSyncState(ChatSyncEntity(chatId, oldestSeq = nextCursor, newestSeq = newestSeq, reachedStart = !hasMore))
    }

    @Transaction
    suspend fun storeOlderPage(chatId: String, entities: List<MessageEntity>, nextCursor: Long?, hasMore: Boolean) {
        entities.forEach { confirm(it) }
        updateOlderHistory(chatId, nextCursor, reachedStart = !hasMore)
    }

    // A page after newestSeq continues the history with no gap, so its cursor is the new newestSeq.
    @Transaction
    suspend fun storeNewerPage(chatId: String, entities: List<MessageEntity>, nextCursor: Long?) {
        entities.forEach { confirm(it) }
        nextCursor?.let { raiseNewestSeq(chatId, it) }
    }

    // Returns true when the message skips a seq, so the messages before it are missing.
    @Transaction
    suspend fun storeIncoming(entity: MessageEntity): Boolean {
        confirm(entity)
        val seq = entity.seq ?: return false
        val newestSeq = syncState(entity.chatId)?.newestSeq ?: return false
        if (seq == newestSeq + 1) raiseNewestSeq(entity.chatId, seq)
        return seq > newestSeq + 1
    }

    @Query("SELECT * FROM chat_sync WHERE chatId = :chatId")
    suspend fun syncState(chatId: String): ChatSyncEntity?

    @Upsert
    suspend fun upsertSyncState(state: ChatSyncEntity)

    @Query(
        "UPDATE chat_sync SET oldestSeq = COALESCE(:oldestSeq, oldestSeq), reachedStart = :reachedStart " +
            "WHERE chatId = :chatId"
    )
    suspend fun updateOlderHistory(chatId: String, oldestSeq: Long?, reachedStart: Boolean)

    @Query("UPDATE chat_sync SET newestSeq = MAX(newestSeq, :newestSeq) WHERE chatId = :chatId")
    suspend fun raiseNewestSeq(chatId: String, newestSeq: Long)

    // Newest first. Pending rows have a null seq, so `seq IS NULL DESC` puts them before every confirmed row.
    // The next message in chat order: for a confirmed row the next stored seq, else the oldest pending row;
    // for a pending row the next pending row by device time.
    @Query(
        """
        SELECT m.*, n.senderId AS nextSenderId, n.timestampMillis AS nextTimestampMillis
        FROM messages m
        LEFT JOIN messages n ON n.rowId = CASE
            WHEN m.seq IS NOT NULL THEN COALESCE(
                (SELECT rowId FROM messages WHERE chatId = m.chatId AND seq > m.seq ORDER BY seq LIMIT 1),
                (SELECT rowId FROM messages WHERE chatId = m.chatId AND seq IS NULL ORDER BY timestampMillis LIMIT 1)
            )
            ELSE (
                SELECT rowId FROM messages
                WHERE chatId = m.chatId AND seq IS NULL AND timestampMillis > m.timestampMillis
                ORDER BY timestampMillis LIMIT 1
            )
        END
        WHERE m.chatId = :chatId
        ORDER BY m.seq IS NULL DESC, m.seq DESC, m.timestampMillis DESC
        """
    )
    fun pagingSource(chatId: String): PagingSource<Int, MessageWithNextRow>
}
