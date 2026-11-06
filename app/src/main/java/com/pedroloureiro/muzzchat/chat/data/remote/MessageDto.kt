package com.pedroloureiro.muzzchat.chat.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class ContentDto(val type: String, val value: String)

@Serializable
data class MessageDto(
    val messageId: String,
    val senderId: String,
    val content: ContentDto,
    val timestamp: Long
)
