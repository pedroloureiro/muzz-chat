package com.pedroloureiro.muzzchat.chat.data.db

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val messageId: String,
    val senderId: String,
    @Embedded val content: Content,
    val timestamp: Long,
    val deliveryStatus: MessageDeliveryStatus
)
