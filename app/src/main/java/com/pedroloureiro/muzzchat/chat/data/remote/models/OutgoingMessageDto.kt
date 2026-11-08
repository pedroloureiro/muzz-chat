package com.pedroloureiro.muzzchat.chat.data.remote.models

import kotlinx.serialization.Serializable

@Serializable
data class OutgoingMessageDto(
    val clientMessageId: String,
    val senderId: String,
    val content: ContentDto,
    val timestampMillis: Long
)
