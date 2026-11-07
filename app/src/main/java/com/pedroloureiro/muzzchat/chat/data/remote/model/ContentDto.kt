package com.pedroloureiro.muzzchat.chat.data.remote.model

import kotlinx.serialization.Serializable

@Serializable
data class ContentDto(val type: String, val value: String)
