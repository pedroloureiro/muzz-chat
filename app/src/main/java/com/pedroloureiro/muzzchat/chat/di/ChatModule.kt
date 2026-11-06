package com.pedroloureiro.muzzchat.chat.di

import androidx.room.Room
import com.pedroloureiro.muzzchat.chat.data.ChatRepositoryImpl
import com.pedroloureiro.muzzchat.chat.data.local.ChatLocalDataSource
import com.pedroloureiro.muzzchat.chat.data.local.RoomChatLocalDataSource
import com.pedroloureiro.muzzchat.chat.data.local.db.MessageDatabase
import com.pedroloureiro.muzzchat.chat.data.mock.MockData
import com.pedroloureiro.muzzchat.chat.data.remote.ChatRemoteDataSource
import com.pedroloureiro.muzzchat.chat.data.remote.MockChatRemoteDataSource
import com.pedroloureiro.muzzchat.chat.domain.repository.ChatRepository
import com.pedroloureiro.muzzchat.chat.domain.usecase.SendMessageUseCase
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val chatModule = module {
    single { Room.databaseBuilder(androidContext(), MessageDatabase::class.java, "messages.db").build() }
    single { get<MessageDatabase>().messageDao() }
    single<ChatLocalDataSource> { RoomChatLocalDataSource(get()) }
    single<ChatRemoteDataSource> { MockChatRemoteDataSource() }
    single { MockData(androidContext()) }
    single { CoroutineScope(SupervisorJob() + Dispatchers.Default) }
    single<ChatRepository> { ChatRepositoryImpl(get(), get(), get(), get()) }
    factory { SendMessageUseCase(get(), System::currentTimeMillis) { UUID.randomUUID().toString() } }
}
