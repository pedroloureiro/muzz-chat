package com.pedroloureiro.muzzchat.chat.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.pedroloureiro.muzzchat.chat.data.local.db.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

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

    // The inner DESC makes LIMIT keep the newest rows; ASC alone would hide new messages past 500.
    // Pending rows have a null id, so `id IS NULL` puts them after every confirmed row.
    @Query(
        "SELECT * FROM (SELECT * FROM messages ORDER BY id IS NULL DESC, id DESC, timestampMillis DESC LIMIT 500) " +
            "ORDER BY id IS NULL, id, timestampMillis"
    )
    fun getMessages(): Flow<List<MessageEntity>>

    @Query("DELETE FROM messages")
    suspend fun clearAll()
}
