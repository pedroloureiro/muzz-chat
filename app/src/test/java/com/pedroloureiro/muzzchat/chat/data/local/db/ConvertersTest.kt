package com.pedroloureiro.muzzchat.chat.data.local.db

import com.pedroloureiro.muzzchat.chat.domain.models.ContentType
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
    fun `content types are stored by name`() {
        assertEquals("TEXT", converters.fromContentType(ContentType.TEXT))
    }
}
