package com.pedroloureiro.muzzchat.chat.data.local.db.entity

import androidx.room.Entity
import androidx.room.Index
import com.pedroloureiro.muzzchat.chat.domain.model.ContentType

@Entity(
    tableName = "messages",
    primaryKeys = ["senderId", "clientMessageId"],
    indices = [
        Index(value = ["serverMessageId"], unique = true),
        Index(value = ["chatId", "seq"], unique = true)
    ]
)
data class MessageEntity(
    val clientMessageId: String,
    val serverMessageId: String?, // null while pending
    val seq: Long?, // null while pending
    val chatId: String,
    val senderId: String,
    val contentType: ContentType,
    val contentValue: String,
    val timestampMillis: Long
)
