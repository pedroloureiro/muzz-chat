package com.pedroloureiro.muzzchat.chat.data.remote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ContentDto(val type: String, val value: String)

@Serializable
data class MessageDto(
    val messageId: String,
    val senderId: String,
    val content: ContentDto,
    @SerialName("timestamp") val timestampMillis: Long
)
