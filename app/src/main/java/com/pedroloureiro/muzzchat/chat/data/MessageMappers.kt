package com.pedroloureiro.muzzchat.chat.data

import com.pedroloureiro.muzzchat.chat.data.local.db.entities.MessageEntity
import com.pedroloureiro.muzzchat.chat.data.local.db.entities.MessageWithNextRow
import com.pedroloureiro.muzzchat.chat.data.remote.models.ContentDto
import com.pedroloureiro.muzzchat.chat.data.remote.models.MessageDto
import com.pedroloureiro.muzzchat.chat.data.remote.models.OutgoingMessageDto
import com.pedroloureiro.muzzchat.chat.domain.models.Content
import com.pedroloureiro.muzzchat.chat.domain.models.ContentType
import com.pedroloureiro.muzzchat.chat.domain.models.Message
import com.pedroloureiro.muzzchat.chat.domain.models.MessageWithNext

fun MessageDto.toEntity() = MessageEntity(
    clientMessageId = clientMessageId,
    serverMessageId = serverMessageId,
    seq = seq,
    chatId = chatId,
    senderId = senderId,
    contentType = ContentType.valueOf(content.type),
    contentValue = content.value,
    timestampMillis = timestampMillis
)

fun MessageEntity.toMessage(): Message {
    val content = Content(contentType, contentValue)
    return if (serverMessageId == null) {
        Message.Pending(clientMessageId, chatId, senderId, content, timestampMillis)
    } else {
        Message.Confirmed(clientMessageId, serverMessageId, checkNotNull(seq), chatId, senderId, content, timestampMillis)
    }
}

fun MessageWithNextRow.toMessageWithNext() = MessageWithNext(message.toMessage(), nextSenderId, nextTimestampMillis)

fun Message.Pending.toEntity() = MessageEntity(
    clientMessageId = clientMessageId,
    serverMessageId = null,
    seq = null,
    chatId = chatId,
    senderId = senderId,
    contentType = content.type,
    contentValue = content.value,
    timestampMillis = timestampMillis
)

fun Message.Pending.toDto() = OutgoingMessageDto(
    clientMessageId = clientMessageId,
    content = ContentDto(type = content.type.name, value = content.value),
    timestampMillis = timestampMillis
)
