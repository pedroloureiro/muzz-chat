package com.pedroloureiro.muzzchat.chat.data.local.db.entity

import androidx.room.Embedded

data class MessageWithNextRow(
    @Embedded val message: MessageEntity,
    val nextSenderId: String?,
    val nextTimestampMillis: Long?
)
