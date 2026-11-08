package com.pedroloureiro.muzzchat.chat.data.remote.models

import kotlinx.serialization.Serializable

@Serializable
data class MessageDto(
    val clientMessageId: String,
    val serverMessageId: String,
    val seq: Long,
    val chatId: String,
    val senderId: String,
    val content: ContentDto,
    val timestampMillis: Long
)
