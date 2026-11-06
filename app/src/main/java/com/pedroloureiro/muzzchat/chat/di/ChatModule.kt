package com.pedroloureiro.muzzchat.chat.di

import com.pedroloureiro.muzzchat.chat.data.ChatRepositoryImpl
import com.pedroloureiro.muzzchat.chat.data.local.ChatLocalDataSource
import com.pedroloureiro.muzzchat.chat.data.local.RoomChatLocalDataSource
import com.pedroloureiro.muzzchat.chat.data.local.db.MessageDatabase
import com.pedroloureiro.muzzchat.chat.data.mock.MockData
import com.pedroloureiro.muzzchat.chat.data.remote.ChatRemoteDataSource
import com.pedroloureiro.muzzchat.chat.data.remote.MockChatRemoteDataSource
import com.pedroloureiro.muzzchat.chat.domain.repository.ChatRepository
import com.pedroloureiro.muzzchat.chat.domain.usecase.SendMessageUseCase
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

private val localModule = module {
    single { MessageDatabase.create(androidContext()) }
    single { get<MessageDatabase>().messageDao() }
    single<ChatLocalDataSource> { RoomChatLocalDataSource(get()) }
}

private val mockBackendModule = module {
    single<ChatRemoteDataSource> { MockChatRemoteDataSource(get()) }
    single { MockData(androidContext()) }
}

val chatModule = module {
    includes(localModule, mockBackendModule)
    single<ChatRepository> { ChatRepositoryImpl(local = get(), remote = get()) }
    factory { SendMessageUseCase(get()) }
}
