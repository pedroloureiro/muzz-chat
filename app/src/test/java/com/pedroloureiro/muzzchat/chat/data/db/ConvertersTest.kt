package com.pedroloureiro.muzzchat.chat.data.db

import org.junit.Assert.assertEquals
import org.junit.Test

class ConvertersTest {

    private val converters = Converters()

    @Test
    fun `every content type round-trips through its stored string`() {
        ContentType.entries.forEach { type ->
            assertEquals(type, converters.toContentType(converters.fromContentType(type)))
        }
    }

    @Test
    fun `every delivery status round-trips through its stored string`() {
        MessageDeliveryStatus.entries.forEach { status ->
            assertEquals(status, converters.toDeliveryStatus(converters.fromDeliveryStatus(status)))
        }
    }

    @Test
    fun `enums are stored by name`() {
        assertEquals("TEXT", converters.fromContentType(ContentType.TEXT))
        assertEquals("SENDING", converters.fromDeliveryStatus(MessageDeliveryStatus.SENDING))
        assertEquals("SENT", converters.fromDeliveryStatus(MessageDeliveryStatus.SENT))
    }
}
