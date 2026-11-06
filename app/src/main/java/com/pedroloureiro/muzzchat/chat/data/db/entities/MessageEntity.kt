package com.pedroloureiro.muzzchat.chat.data.db.entities

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.pedroloureiro.muzzchat.chat.data.db.models.Content
import com.pedroloureiro.muzzchat.chat.data.db.models.MessageDeliveryStatus

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val messageId: String,
    val senderId: String,
    @Embedded val content: Content,
    val timestamp: Long,
    val deliveryStatus: MessageDeliveryStatus
)
