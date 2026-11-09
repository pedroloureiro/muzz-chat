package com.pedroloureiro.muzzchat.chat.di

import com.pedroloureiro.muzzchat.chat.data.ChatRepositoryImpl
import com.pedroloureiro.muzzchat.chat.data.MockChatApi
import com.pedroloureiro.muzzchat.chat.data.local.ChatLocalDataSource
import com.pedroloureiro.muzzchat.chat.data.local.RoomChatLocalDataSource
import com.pedroloureiro.muzzchat.chat.data.local.db.MessageDatabase
import com.pedroloureiro.muzzchat.chat.data.remote.ChatRemoteDataSource
import com.pedroloureiro.muzzchat.chat.data.remote.MockChatRemoteDataSource
import com.pedroloureiro.muzzchat.chat.domain.ChatRepository
import com.pedroloureiro.muzzchat.chat.domain.OtherUserDevice
import com.pedroloureiro.muzzchat.chat.domain.SendMessageUseCase
import com.pedroloureiro.muzzchat.chat.ui.ChatViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.binds
import org.koin.dsl.module

private val localModule = module {
    single { MessageDatabase.create(androidContext()) }
    single<ChatLocalDataSource> { RoomChatLocalDataSource(get()) }
}

private val mockBackendModule = module {
    single { MockChatRemoteDataSource(get()) } binds arrayOf(ChatRemoteDataSource::class, OtherUserDevice::class)
    single { MockChatApi(androidContext(), get()) }
}

val chatModule = module {
    includes(localModule, mockBackendModule)
    single<ChatRepository> { ChatRepositoryImpl(local = get(), remote = get()) }
    factory { SendMessageUseCase(get(), get()) }
    viewModel { (chatId: String, otherUserId: String) -> ChatViewModel(chatId, otherUserId, get(), get(), get(), get()) }
}
