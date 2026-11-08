package com.pedroloureiro.muzzchat.chat.data

import com.pedroloureiro.muzzchat.chat.data.local.db.entity.MessageEntity
import com.pedroloureiro.muzzchat.chat.data.local.db.entity.MessageWithNextRow
import com.pedroloureiro.muzzchat.chat.data.remote.model.ContentDto
import com.pedroloureiro.muzzchat.chat.data.remote.model.MessageDto
import com.pedroloureiro.muzzchat.chat.data.remote.model.OutgoingMessageDto
import com.pedroloureiro.muzzchat.chat.domain.model.Content
import com.pedroloureiro.muzzchat.chat.domain.model.ContentType
import com.pedroloureiro.muzzchat.chat.domain.model.Message
import com.pedroloureiro.muzzchat.chat.domain.model.MessageWithNext

fun MessageDto.toEntity() = MessageEntity(
    id = id,
    seq = seq,
    localId = localId,
    chatId = chatId,
    senderId = senderId,
    contentType = ContentType.valueOf(content.type),
    contentValue = content.value,
    timestampMillis = timestampMillis
)

fun MessageEntity.toMessage(): Message {
    val content = Content(contentType, contentValue)
    return if (id == null) {
        Message.Pending(checkNotNull(localId), chatId, senderId, content, timestampMillis)
    } else {
        Message.Confirmed(id, checkNotNull(seq), localId, chatId, senderId, content, timestampMillis)
    }
}

fun MessageWithNextRow.toMessageWithNext() = MessageWithNext(message.toMessage(), nextSenderId, nextTimestampMillis)

fun Message.Pending.toEntity() = MessageEntity(
    id = null,
    seq = null,
    localId = localId,
    chatId = chatId,
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
