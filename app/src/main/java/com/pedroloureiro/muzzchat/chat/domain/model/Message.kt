package com.pedroloureiro.muzzchat.chat.domain.model

sealed class Message {
    abstract val chatId: String
    abstract val senderId: String
    abstract val content: Content
    abstract val timestampMillis: Long

    data class Pending(
        val localId: String,
        override val chatId: String,
        override val senderId: String,
        override val content: Content,
        override val timestampMillis: Long // device time
    ) : Message()

    data class Confirmed(
        val id: String,         // opaque server UUID
        val seq: Long,          // server-assigned position in the chat, from 1 with no holes
        val localId: String?,   // set when this device sent it
        override val chatId: String,
        override val senderId: String,
        override val content: Content,
        override val timestampMillis: Long // server time
    ) : Message()
}
