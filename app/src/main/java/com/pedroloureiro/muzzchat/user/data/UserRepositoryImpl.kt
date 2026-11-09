package com.pedroloureiro.muzzchat.user.data

import com.pedroloureiro.muzzchat.user.data.remote.UserDto
import com.pedroloureiro.muzzchat.user.data.remote.UserRemoteDataSource
import com.pedroloureiro.muzzchat.user.domain.User
import com.pedroloureiro.muzzchat.user.domain.UserRepository

class UserRepositoryImpl(private val remote: UserRemoteDataSource) : UserRepository {
    override suspend fun currentUser(): User = remote.me().toUser()
}

private fun UserDto.toUser() = User(id = id, name = name)
