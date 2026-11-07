package com.pedroloureiro.muzzchat.chat.data

import com.pedroloureiro.muzzchat.chat.data.local.db.entity.MessageEntity
import com.pedroloureiro.muzzchat.chat.data.remote.model.ContentDto
import com.pedroloureiro.muzzchat.chat.data.remote.model.MessageDto
import com.pedroloureiro.muzzchat.chat.data.remote.model.OutgoingMessageDto
import com.pedroloureiro.muzzchat.chat.domain.model.Content
import com.pedroloureiro.muzzchat.chat.domain.model.ContentType
import com.pedroloureiro.muzzchat.chat.domain.model.Message

fun MessageDto.toEntity() = MessageEntity(
    id = id,
    localId = localId,
    senderId = senderId,
    contentType = ContentType.valueOf(content.type),
    contentValue = content.value,
    timestampMillis = timestampMillis
)

fun MessageEntity.toMessage(): Message {
    val content = Content(contentType, contentValue)
    return if (id == null) {
        Message.Pending(checkNotNull(localId), senderId, content, timestampMillis)
    } else {
        Message.Confirmed(id, localId, senderId, content, timestampMillis)
    }
}

fun Message.Pending.toEntity() = MessageEntity(
    id = null,
    localId = localId,
    senderId = senderId,
    contentType = content.type,
    contentValue = content.value,
    timestampMillis = timestampMillis
)

fun Message.Pending.toDto() = OutgoingMessageDto(
    localId = localId,
    senderId = senderId,
    content = ContentDto(type = content.type.name, value = content.value),
    timestampMillis = timestampMillis
)
