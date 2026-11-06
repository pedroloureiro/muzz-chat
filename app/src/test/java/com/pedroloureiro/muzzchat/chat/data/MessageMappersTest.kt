package com.pedroloureiro.muzzchat.chat.data

import com.pedroloureiro.muzzchat.chat.data.local.db.entity.MessageEntity
import com.pedroloureiro.muzzchat.chat.data.remote.ContentDto
import com.pedroloureiro.muzzchat.chat.data.remote.MessageDto
import com.pedroloureiro.muzzchat.chat.domain.model.ContentType
import com.pedroloureiro.muzzchat.chat.domain.model.MessageDeliveryStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class MessageMappersTest {

    private val dto = MessageDto(
        messageId = "id",
        senderId = "sender",
        content = ContentDto(type = "TEXT", value = "text"),
        timestamp = 1L
    )

    @Test
    fun `toEntity flattens the dto and defaults to SENT`() {
        val expected = MessageEntity(
            messageId = "id",
            senderId = "sender",
            contentType = ContentType.TEXT,
            contentValue = "text",
            timestamp = 1L,
            deliveryStatus = MessageDeliveryStatus.SENT
        )

        assertEquals(expected, dto.toEntity())
    }

    @Test
    fun `toEntity uses the given status`() {
        assertEquals(
            MessageDeliveryStatus.SENDING,
            dto.toEntity(status = MessageDeliveryStatus.SENDING).deliveryStatus
        )
    }
}
