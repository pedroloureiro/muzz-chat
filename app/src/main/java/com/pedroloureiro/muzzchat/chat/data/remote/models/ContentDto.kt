package com.pedroloureiro.muzzchat.chat.data.remote.models

import kotlinx.serialization.Serializable

@Serializable
data class ContentDto(val type: String, val value: String)
