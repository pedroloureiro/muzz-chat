package com.pedroloureiro.muzzchat.chat.domain.model

sealed class Message {
    abstract val senderId: String
    abstract val content: Content
    abstract val timestampMillis: Long

    data class Pending(
        val localId: String,
        override val senderId: String,
        override val content: Content,
        override val timestampMillis: Long // device time
    ) : Message()

    data class Confirmed(
        val id: String,         // server-generated UUIDv7, lowercase
        val localId: String?,   // set when this device sent it
        override val senderId: String,
        override val content: Content,
        override val timestampMillis: Long // server time
    ) : Message()
}
