package com.pedroloureiro.muzzchat.chat.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.pedroloureiro.muzzchat.chat.domain.model.User
import com.pedroloureiro.muzzchat.chat.domain.model.Users
import com.pedroloureiro.muzzchat.chat.domain.repository.ChatRepository
import com.pedroloureiro.muzzchat.chat.domain.usecase.SendMessageUseCase
import com.pedroloureiro.muzzchat.chat.ui.model.ChatItem
import java.time.ZoneId
import java.util.Locale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(
    private val chatId: String,
    private val repository: ChatRepository,
    private val sendMessageUseCase: SendMessageUseCase
) : ViewModel() {

    val messages: Flow<PagingData<ChatItem>> = repository.getMessages(chatId)
        .map { it.toChatItems(System.currentTimeMillis(), ZoneId.systemDefault(), Locale.getDefault()) }
        .cachedIn(viewModelScope)

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch { repository.syncIncomingMessages(chatId) }
    }

    fun onInputChanged(text: String) = _uiState.update { it.copy(inputText = text) }

    fun sendMessage() = sendAs(Users.ME)

    fun sendAsOther() = sendAs(Users.SARAH)

    private fun sendAs(sender: User) {
        val text = _uiState.value.inputText
        if (text.isBlank()) return
        _uiState.update { it.copy(inputText = "") }
        viewModelScope.launch { sendMessageUseCase(chatId, text, sender) }
    }
}
