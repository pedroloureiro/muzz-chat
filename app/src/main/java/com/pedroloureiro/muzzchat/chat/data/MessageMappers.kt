package com.pedroloureiro.muzzchat.chat.data

import com.pedroloureiro.muzzchat.chat.data.local.db.entity.MessageEntity
import com.pedroloureiro.muzzchat.chat.data.remote.model.ContentDto
import com.pedroloureiro.muzzchat.chat.data.remote.model.MessageDto
import com.pedroloureiro.muzzchat.chat.domain.model.Content
import com.pedroloureiro.muzzchat.chat.domain.model.ContentType
import com.pedroloureiro.muzzchat.chat.domain.model.Message
import com.pedroloureiro.muzzchat.chat.domain.model.MessageDeliveryStatus

fun MessageDto.toEntity(status: MessageDeliveryStatus = MessageDeliveryStatus.SENT) = MessageEntity(
    messageId = messageId,
    senderId = senderId,
    contentType = ContentType.valueOf(content.type),
    contentValue = content.value,
    timestampMillis = timestampMillis,
    deliveryStatus = status
)

fun MessageEntity.toMessage() = Message(
    messageId = messageId,
    senderId = senderId,
    content = Content(contentType, contentValue),
    timestampMillis = timestampMillis,
    deliveryStatus = deliveryStatus
)

fun Message.toEntity() = MessageEntity(
    messageId = messageId,
    senderId = senderId,
    contentType = content.type,
    contentValue = content.value,
    timestampMillis = timestampMillis,
    deliveryStatus = deliveryStatus
)

fun Message.toDto() = MessageDto(
    messageId = messageId,
    senderId = senderId,
    content = ContentDto(type = content.type.name, value = content.value),
    timestampMillis = timestampMillis
)
