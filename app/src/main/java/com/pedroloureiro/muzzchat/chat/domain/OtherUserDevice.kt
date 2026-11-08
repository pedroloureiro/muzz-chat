package com.pedroloureiro.muzzchat.chat.domain

// MOCK DATA: stands in for Sarah's phone, which the exercise does not have. Her messages reach this device
// only through the incoming stream, like any message from another device.
interface OtherUserDevice {
    suspend fun send(chatId: String, text: String)
}
