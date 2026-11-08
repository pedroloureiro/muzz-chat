package com.pedroloureiro.muzzchat.chat.data.remote.model

import kotlinx.serialization.Serializable

@Serializable
data class MessagePageDto(
    val messages: List<MessageDto>,
    val nextCursor: Long?,
    val hasMore: Boolean
)
