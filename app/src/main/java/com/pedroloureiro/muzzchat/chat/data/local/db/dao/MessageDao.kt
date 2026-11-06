package com.pedroloureiro.muzzchat.chat.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pedroloureiro.muzzchat.chat.data.local.db.entity.MessageEntity
import com.pedroloureiro.muzzchat.chat.domain.model.MessageDeliveryStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: MessageEntity)

    @Query("UPDATE messages SET deliveryStatus = :status WHERE messageId = :id")
    suspend fun updateStatus(id: String, status: MessageDeliveryStatus)

    // The inner DESC makes LIMIT keep the newest rows; ASC alone would hide new messages past 500.
    @Query("SELECT * FROM (SELECT * FROM messages ORDER BY timestamp DESC LIMIT 500) ORDER BY timestamp ASC")
    fun getMessages(): Flow<List<MessageEntity>>

    @Query("DELETE FROM messages")
    suspend fun clearAll()
}
