package com.pedroloureiro.muzzchat.user.data.remote

interface UserRemoteDataSource {
    suspend fun me(): UserDto
}
