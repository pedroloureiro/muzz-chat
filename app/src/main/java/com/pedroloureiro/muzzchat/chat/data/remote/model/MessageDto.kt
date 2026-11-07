package com.pedroloureiro.muzzchat.chat.data.remote.model

import kotlinx.serialization.Serializable

@Serializable
data class MessageDto(
    val id: String,
    val localId: String? = null,
    val senderId: String,
    val content: ContentDto,
    val timestampMillis: Long
)
