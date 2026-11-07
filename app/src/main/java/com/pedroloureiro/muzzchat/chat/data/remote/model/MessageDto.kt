package com.pedroloureiro.muzzchat.chat.data.remote.model

import kotlinx.serialization.Serializable

@Serializable
data class ContentDto(val type: String, val value: String)

@Serializable
data class MessageDto(
    val id: String,
    val localId: String? = null,
    val senderId: String,
    val content: ContentDto,
    val timestampMillis: Long
)

@Serializable
data class OutgoingMessageDto(
    val localId: String,
    val senderId: String,
    val content: ContentDto,
    val timestampMillis: Long
)
