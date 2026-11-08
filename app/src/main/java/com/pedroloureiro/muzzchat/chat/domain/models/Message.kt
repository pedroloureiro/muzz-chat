package com.pedroloureiro.muzzchat.chat.domain.models

sealed class Message {
    abstract val clientMessageId: String // unique per sender, so senderId and clientMessageId identify the message
    abstract val chatId: String
    abstract val senderId: String
    abstract val content: Content
    abstract val timestampMillis: Long

    data class Pending(
        override val clientMessageId: String,
        override val chatId: String,
        override val senderId: String,
        override val content: Content,
        override val timestampMillis: Long // device time
    ) : Message()

    data class Confirmed(
        override val clientMessageId: String,
        val serverMessageId: String, // opaque server UUID
        val seq: Long,               // server-assigned position in the chat, from 1 with no holes
        override val chatId: String,
        override val senderId: String,
        override val content: Content,
        override val timestampMillis: Long // server time
    ) : Message()
}
