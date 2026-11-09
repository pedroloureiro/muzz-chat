package com.pedroloureiro.muzzchat.chat.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.pedroloureiro.muzzchat.chat.domain.ChatRepository
import com.pedroloureiro.muzzchat.chat.domain.OtherUserDevice
import com.pedroloureiro.muzzchat.chat.domain.SendMessageUseCase
import com.pedroloureiro.muzzchat.chat.ui.models.ChatItem
import com.pedroloureiro.muzzchat.chat.ui.models.ChatUiState
import com.pedroloureiro.muzzchat.user.domain.UserRepository
import java.time.ZoneId
import java.util.Locale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(
    private val chatId: String,
    private val otherUserId: String,
    private val repository: ChatRepository,
    private val userRepository: UserRepository,
    private val sendMessageUseCase: SendMessageUseCase,
    private val otherUser: OtherUserDevice
) : ViewModel() {

    val messages: Flow<PagingData<ChatItem>> = flow {
        val currentUserId = userRepository.currentUser().id
        emitAll(
            repository.getMessages(chatId).map {
                it.toChatItems(currentUserId, System.currentTimeMillis(), ZoneId.systemDefault(), Locale.getDefault())
            }
        )
    }.cachedIn(viewModelScope)

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch { repository.syncIncomingMessages(chatId) }
    }

    fun onInputChanged(text: String) = _uiState.update { it.copy(inputText = text) }

    fun send() = takeInput { text -> sendMessageUseCase(chatId, text) }

    fun sendAsOther() = takeInput { text -> otherUser.sendAsAnother(chatId, otherUserId, text.trim()) }

    private fun takeInput(send: suspend (String) -> Unit) {
        val text = _uiState.value.inputText
        if (text.isBlank()) return
        _uiState.update { it.copy(inputText = "") }
        viewModelScope.launch { send(text) }
    }
}
