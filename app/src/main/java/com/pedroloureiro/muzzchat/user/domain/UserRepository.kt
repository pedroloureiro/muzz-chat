package com.pedroloureiro.muzzchat.user.domain

interface UserRepository {
    suspend fun currentUser(): User
}
