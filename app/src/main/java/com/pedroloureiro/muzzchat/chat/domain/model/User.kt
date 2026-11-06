package com.pedroloureiro.muzzchat.chat.domain.model

data class User(val id: String, val name: String)

object Users {
    val ME = User("00000000-0000-0000-0000-000000000001", "You")
    val SARAH = User("00000000-0000-0000-0000-000000000002", "Sarah")
}
