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
import org.junit.Assert.assertEquals
import org.junit.Test

class MessageMappersTest {

    private val pending = Message.Pending(
        clientMessageId = "client",
        chatId = "chat",
        senderId = "sender",
        content = Content(ContentType.TEXT, "text"),
        timestampMillis = 1L
    )
    private val pendingEntity = MessageEntity(
        clientMessageId = "client",
        serverMessageId = null,
        seq = null,
        chatId = "chat",
        senderId = "sender",
        contentType = ContentType.TEXT,
        contentValue = "text",
        timestampMillis = 1L
    )
    private val dto = MessageDto(
        clientMessageId = "client",
        serverMessageId = "server",
        seq = 7L,
        chatId = "chat",
        senderId = "sender",
        content = ContentDto(type = "TEXT", value = "text"),
        timestampMillis = 2L
    )
    private val confirmedEntity = pendingEntity.copy(serverMessageId = "server", seq = 7L, timestampMillis = 2L)

    @Test
    fun `dto toEntity flattens the content and keeps both ids, the seq and the chat`() {
        assertEquals(confirmedEntity, dto.toEntity())
    }

    @Test
    fun `entity without a serverMessageId maps to a pending message`() {
        assertEquals(pending, pendingEntity.toMessage())
    }

    @Test
    fun `entity with a serverMessageId maps to a confirmed message`() {
        val expected = Message.Confirmed(
            clientMessageId = "client",
            serverMessageId = "server",
            seq = 7L,
            chatId = "chat",
            senderId = "sender",
            content = Content(ContentType.TEXT, "text"),
            timestampMillis = 2L
        )

        assertEquals(expected, confirmedEntity.toMessage())
    }

    @Test
    fun `row toMessageWithNext keeps the next message's sender and timestamp`() {
        val row = MessageWithNextRow(pendingEntity, nextSenderId = "next", nextTimestampMillis = 3L)

        assertEquals(MessageWithNext(pending, nextSenderId = "next", nextTimestampMillis = 3L), row.toMessageWithNext())
    }

    @Test
    fun `pending toEntity stores the message in its chat without a serverMessageId or seq`() {
        assertEquals(pendingEntity, pending.toEntity())
    }

    @Test
    fun `pending toDto sends the clientMessageId and the content type name, and no sender`() {
        val expected = OutgoingMessageDto(
            clientMessageId = "client",
            content = ContentDto(type = "TEXT", value = "text"),
            timestampMillis = 1L
        )

        assertEquals(expected, pending.toDto())
    }
}
