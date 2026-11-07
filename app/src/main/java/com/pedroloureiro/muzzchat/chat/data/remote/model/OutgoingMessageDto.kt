package com.pedroloureiro.muzzchat.chat.data.remote.model

import kotlinx.serialization.Serializable

@Serializable
data class OutgoingMessageDto(
    val localId: String,
    val senderId: String,
    val content: ContentDto,
    val timestampMillis: Long
)
