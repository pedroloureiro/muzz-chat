package com.pedroloureiro.muzzchat.chat.data.db

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromContentType(type: ContentType): String = type.name

    @TypeConverter
    fun toContentType(name: String): ContentType = ContentType.valueOf(name)

    @TypeConverter
    fun fromDeliveryStatus(status: MessageDeliveryStatus): String = status.name

    @TypeConverter
    fun toDeliveryStatus(name: String): MessageDeliveryStatus = MessageDeliveryStatus.valueOf(name)
}
