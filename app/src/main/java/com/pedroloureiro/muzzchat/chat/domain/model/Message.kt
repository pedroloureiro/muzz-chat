package com.pedroloureiro.muzzchat.chat.domain.model

data class Message(
    val messageId: String,
    val senderId: String,
    val content: Content,
    val timestamp: Long,
    val deliveryStatus: MessageDeliveryStatus
)
