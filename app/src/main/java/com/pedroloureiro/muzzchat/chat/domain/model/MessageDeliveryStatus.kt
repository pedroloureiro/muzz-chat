package com.pedroloureiro.muzzchat.chat.domain.model

enum class MessageDeliveryStatus {
    SENDING, // optimistic insert, echo pending
    SENT     // echo confirmed
    // FAILED, RETRYING — reserved, see ADR-0001
}
