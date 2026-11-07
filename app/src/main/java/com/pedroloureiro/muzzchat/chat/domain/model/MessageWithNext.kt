package com.pedroloureiro.muzzchat.chat.domain.model

data class MessageWithNext(
    val message: Message,
    val nextSenderId: String?,
    val nextTimestampMillis: Long?
)
