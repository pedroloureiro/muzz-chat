package com.pedroloureiro.muzzchat.chat.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.pedroloureiro.muzzchat.chat.data.db.dao.MessageDao
import com.pedroloureiro.muzzchat.chat.data.db.entities.MessageEntity

@Database(entities = [MessageEntity::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class MessageDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
}
