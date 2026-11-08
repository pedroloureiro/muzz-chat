package com.pedroloureiro.muzzchat.chat.domain.models

data class MessageWithNext(
    val message: Message,
    val nextSenderId: String?,
    val nextTimestampMillis: Long?
)
