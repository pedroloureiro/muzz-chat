package com.pedroloureiro.muzzchat.user.di

import com.pedroloureiro.muzzchat.user.data.UserRepositoryImpl
import com.pedroloureiro.muzzchat.user.data.remote.MockUserRemoteDataSource
import com.pedroloureiro.muzzchat.user.data.remote.UserRemoteDataSource
import com.pedroloureiro.muzzchat.user.domain.UserRepository
import org.koin.dsl.module

val userModule = module {
    single<UserRemoteDataSource> { MockUserRemoteDataSource() }
    single<UserRepository> { UserRepositoryImpl(get()) }
}
