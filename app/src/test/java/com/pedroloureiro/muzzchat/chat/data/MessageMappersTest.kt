package com.pedroloureiro.muzzchat.chat.data

import com.pedroloureiro.muzzchat.chat.data.local.db.entity.MessageEntity
import com.pedroloureiro.muzzchat.chat.data.remote.model.ContentDto
import com.pedroloureiro.muzzchat.chat.data.remote.model.MessageDto
import com.pedroloureiro.muzzchat.chat.data.remote.model.OutgoingMessageDto
import com.pedroloureiro.muzzchat.chat.domain.model.Content
import com.pedroloureiro.muzzchat.chat.domain.model.ContentType
import com.pedroloureiro.muzzchat.chat.domain.model.Message
import org.junit.Assert.assertEquals
import org.junit.Test

class MessageMappersTest {

    private val pending = Message.Pending(
        localId = "local",
        senderId = "sender",
        content = Content(ContentType.TEXT, "text"),
        timestampMillis = 1L
    )
    private val pendingEntity = MessageEntity(
        id = null,
        localId = "local",
        senderId = "sender",
        contentType = ContentType.TEXT,
        contentValue = "text",
        timestampMillis = 1L
    )
    private val dto = MessageDto(
        id = "server",
        localId = "local",
        senderId = "sender",
        content = ContentDto(type = "TEXT", value = "text"),
        timestampMillis = 2L
    )
    private val confirmedEntity = pendingEntity.copy(id = "server", timestampMillis = 2L)

    @Test
    fun `dto toEntity flattens the content and keeps both ids`() {
        assertEquals(confirmedEntity, dto.toEntity())
    }

    @Test
    fun `entity without an id maps to a pending message`() {
        assertEquals(pending, pendingEntity.toMessage())
    }

    @Test
    fun `entity with an id maps to a confirmed message`() {
        val expected = Message.Confirmed(
            id = "server",
            localId = "local",
            senderId = "sender",
            content = Content(ContentType.TEXT, "text"),
            timestampMillis = 2L
        )

        assertEquals(expected, confirmedEntity.toMessage())
    }

    @Test
    fun `pending toEntity stores the message without an id`() {
        assertEquals(pendingEntity, pending.toEntity())
    }

    @Test
    fun `pending toDto sends the localId and the content type name`() {
        val expected = OutgoingMessageDto(
            localId = "local",
            senderId = "sender",
            content = ContentDto(type = "TEXT", value = "text"),
            timestampMillis = 1L
        )

        assertEquals(expected, pending.toDto())
    }
}
