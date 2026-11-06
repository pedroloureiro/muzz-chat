package com.pedroloureiro.muzzchat.chat.data

import com.pedroloureiro.muzzchat.chat.data.local.db.entity.MessageEntity
import com.pedroloureiro.muzzchat.chat.data.remote.MessageDto
import com.pedroloureiro.muzzchat.chat.domain.model.ContentType
import com.pedroloureiro.muzzchat.chat.domain.model.MessageDeliveryStatus

fun MessageDto.toEntity(status: MessageDeliveryStatus = MessageDeliveryStatus.SENT) = MessageEntity(
    messageId = messageId,
    senderId = senderId,
    contentType = ContentType.valueOf(content.type),
    contentValue = content.value,
    timestamp = timestamp,
    deliveryStatus = status
)
