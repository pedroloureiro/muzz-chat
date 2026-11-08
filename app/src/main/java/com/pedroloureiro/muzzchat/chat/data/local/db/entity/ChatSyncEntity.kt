package com.pedroloureiro.muzzchat.chat.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_sync")
data class ChatSyncEntity(
    @PrimaryKey val chatId: String,
    val oldestSeq: Long?, // where older history continues, from the server's cursor
    val newestSeq: Long, // stored history has no gaps up to here; 0 when the chat has no messages
    val reachedStart: Boolean
)
