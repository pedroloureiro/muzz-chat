package com.pedroloureiro.muzzchat

import android.app.Application
import com.pedroloureiro.muzzchat.chat.di.chatModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class MuzzChatApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@MuzzChatApplication)
            modules(chatModule)
        }
    }
}
