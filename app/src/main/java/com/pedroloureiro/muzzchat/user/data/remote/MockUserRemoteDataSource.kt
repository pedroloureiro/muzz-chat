package com.pedroloureiro.muzzchat.user.data.remote

// MOCK DATA: the app has no login, so the signed-in user is fixed. The seed messages in mock_messages.json use this id.
class MockUserRemoteDataSource : UserRemoteDataSource {
    override suspend fun me() = UserDto(id = "00000000-0000-0000-0000-000000000001", name = "You")
}
