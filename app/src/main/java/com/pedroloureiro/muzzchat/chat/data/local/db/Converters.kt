package com.pedroloureiro.muzzchat.chat.data.local.db

import androidx.room.TypeConverter
import com.pedroloureiro.muzzchat.chat.domain.model.ContentType

class Converters {
    @TypeConverter
    fun fromContentType(type: ContentType): String = type.name

    @TypeConverter
    fun toContentType(name: String): ContentType = ContentType.valueOf(name)
}
