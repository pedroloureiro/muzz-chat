package com.pedroloureiro.muzzchat.user.data

import com.pedroloureiro.muzzchat.user.data.remote.UserDto
import com.pedroloureiro.muzzchat.user.data.remote.UserRemoteDataSource
import com.pedroloureiro.muzzchat.user.domain.User
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class UserRepositoryImplTest {

    private val remote = FakeUserRemoteDataSource()
    private val repository = UserRepositoryImpl(remote)

    @Test
    fun currentUser_isTheSignedInUserFromTheRemote() = runTest {
        remote.me = UserDto(id = "me", name = "Me")

        assertEquals(User("me", "Me"), repository.currentUser())
    }

    private class FakeUserRemoteDataSource : UserRemoteDataSource {
        var me = UserDto(id = "", name = "")

        override suspend fun me() = me
    }
}
