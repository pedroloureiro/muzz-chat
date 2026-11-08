package com.pedroloureiro.muzzchat.chat.data.remote.model

import kotlinx.serialization.Serializable

// `nextCursor` is the seq to continue from in the direction requested, and `hasMore` whether messages remain there.
@Serializable
data class MessagePageDto(
    val messages: List<MessageDto>,
    val nextCursor: Long?,
    val hasMore: Boolean
)
