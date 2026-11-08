package com.pedroloureiro.muzzchat.chat.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.pedroloureiro.muzzchat.chat.data.local.db.entities.ChatSyncEntity
import com.pedroloureiro.muzzchat.chat.data.local.db.entities.MessageEntity

private const val DATABASE_NAME = "messages.db"
private const val DATABASE_VERSION = 1

@Database(entities = [MessageEntity::class, ChatSyncEntity::class], version = DATABASE_VERSION, exportSchema = false)
@TypeConverters(Converters::class)
abstract class MessageDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao

    companion object {
        fun create(context: Context): MessageDatabase =
            Room.databaseBuilder(context, MessageDatabase::class.java, DATABASE_NAME).build()
    }
}
