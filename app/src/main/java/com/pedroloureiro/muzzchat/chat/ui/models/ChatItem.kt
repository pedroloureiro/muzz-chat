package com.pedroloureiro.muzzchat.chat.ui.models

import com.pedroloureiro.muzzchat.chat.domain.models.MessageDeliveryStatus

sealed class ChatItem {
    abstract val key: String

    data class Message(
        override val key: String,
        val direction: MessageDirection,
        val content: String,
        val timestampMillis: Long,
        val isGrouped: Boolean,
        val deliveryStatus: MessageDeliveryStatus
    ) : ChatItem()

    data class TimestampHeader(val label: String, override val key: String) : ChatItem()

    data object StartOfChat : ChatItem() {
        override val key = "start-of-chat"
    }
}
