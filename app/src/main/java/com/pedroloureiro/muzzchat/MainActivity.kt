package com.pedroloureiro.muzzchat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.pedroloureiro.muzzchat.chat.ui.ChatScreen
import com.pedroloureiro.muzzchat.ui.theme.MuzzChatTheme

// MOCK DATA: stands in for the chatId a chat list would navigate with, matching the mock seed's chat.
private const val MOCK_SARAH_CHAT_ID = "00000000-0000-0000-0000-000000000c01"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MuzzChatTheme {
                ChatScreen(chatId = MOCK_SARAH_CHAT_ID, onBack = { onBackPressedDispatcher.onBackPressed() })
            }
        }
    }
}
