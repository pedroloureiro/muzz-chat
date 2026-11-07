package com.pedroloureiro.muzzchat.chat.data.local.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.pedroloureiro.muzzchat.chat.domain.model.ContentType

@Entity(
    tableName = "messages",
    indices = [Index(value = ["id"], unique = true), Index(value = ["localId"], unique = true)]
)
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val rowId: Long = 0,
    val id: String?, // null while pending
    val localId: String?, // null for messages this device did not send
    val senderId: String,
    val contentType: ContentType,
    val contentValue: String,
    val timestampMillis: Long
)
