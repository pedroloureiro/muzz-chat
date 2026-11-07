package com.pedroloureiro.muzzchat.chat.ui.model

import com.pedroloureiro.muzzchat.chat.domain.model.MessageDeliveryStatus

sealed class ChatItem {
    data class Message(
        val messageId: String,
        val direction: MessageDirection,
        val content: String,
        val timestamp: Long,
        val isGrouped: Boolean,
        val deliveryStatus: MessageDeliveryStatus
    ) : ChatItem()

    data class TimestampHeader(val label: String) : ChatItem()
}
