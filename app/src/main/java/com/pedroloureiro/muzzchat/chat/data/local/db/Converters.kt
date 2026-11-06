package com.pedroloureiro.muzzchat.chat.data.local.db

import androidx.room.TypeConverter
import com.pedroloureiro.muzzchat.chat.domain.model.ContentType
import com.pedroloureiro.muzzchat.chat.domain.model.MessageDeliveryStatus

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
