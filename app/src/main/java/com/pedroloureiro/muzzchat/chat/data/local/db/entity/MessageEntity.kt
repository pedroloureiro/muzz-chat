package com.pedroloureiro.muzzchat.chat.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.pedroloureiro.muzzchat.chat.domain.model.ContentType
import com.pedroloureiro.muzzchat.chat.domain.model.MessageDeliveryStatus

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val messageId: String,
    val senderId: String,
    val contentType: ContentType,
    val contentValue: String,
    val timestampMillis: Long,
    val deliveryStatus: MessageDeliveryStatus
)
