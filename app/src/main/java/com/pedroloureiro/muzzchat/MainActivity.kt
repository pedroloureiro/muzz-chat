package com.pedroloureiro.muzzchat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.pedroloureiro.muzzchat.chat.ui.ChatScreen
import com.pedroloureiro.muzzchat.ui.theme.MuzzChatTheme

// MOCK DATA: the values a chat list would navigate with, matching the mock seed's chat and Sarah's messages.
private const val MOCK_SARAH_CHAT_ID = "00000000-0000-0000-0000-000000000c01"
private const val MOCK_SARAH_NAME = "Sarah"
private const val MOCK_SARAH_USER_ID = "00000000-0000-0000-0000-000000000002"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MuzzChatTheme {
                ChatScreen(
                    chatId = MOCK_SARAH_CHAT_ID,
                    title = MOCK_SARAH_NAME,
                    otherUserId = MOCK_SARAH_USER_ID,
                    onBack = { onBackPressedDispatcher.onBackPressed() },
                )
            }
        }
    }
}
