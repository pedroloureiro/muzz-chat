package com.pedroloureiro.muzzchat.chat.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.pedroloureiro.muzzchat.R
import com.pedroloureiro.muzzchat.chat.ui.components.MessageItem
import com.pedroloureiro.muzzchat.chat.ui.components.MockChatInput
import com.pedroloureiro.muzzchat.chat.ui.components.SectionHeader
import com.pedroloureiro.muzzchat.chat.ui.models.ChatItem
import com.pedroloureiro.muzzchat.ui.components.Button
import com.pedroloureiro.muzzchat.ui.components.ButtonVariant
import com.pedroloureiro.muzzchat.ui.components.TopBar
import com.pedroloureiro.muzzchat.ui.theme.Spacing
import com.pedroloureiro.muzzchat.ui.theme.colorBackground
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

// A one-line message and its gap. A zero-height placeholder would let a jump land among older rows.
private val UnloadedRowHeight = 44.dp

@Composable
fun ChatScreen(
    chatId: String,
    title: String,
    otherUserId: String,
    onBack: () -> Unit,
    viewModel: ChatViewModel = koinViewModel { parametersOf(chatId, otherUserId) },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val messages = viewModel.messages.collectAsLazyPagingItems()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val backInteractionSource = remember { MutableInteractionSource() }
    val inputInteractionSource = remember { MutableInteractionSource() }
    val sendInteractionSource = remember { MutableInteractionSource() }
    val sendAsOtherInteractionSource = remember { MutableInteractionSource() }
    val jumpToLatestInteractionSource = remember { MutableInteractionSource() }
    val isNewestOutOfView by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 } }

    val newestKey = if (messages.itemCount > 0) messages.peek(0)?.key else null
    LaunchedEffect(newestKey) {
        // A new message and its header push the old newest down to index 2 at most.
        if (newestKey != null && listState.firstVisibleItemIndex <= 2) listState.animateScrollToItem(0)
    }

    // Deep in history the newest rows are unloaded placeholders, so snap to them first to see the send land.
    fun sendThenShow(send: () -> Unit) {
        send()
        scope.launch { listState.scrollToItem(0) }
    }

    Scaffold(
        topBar = {
            TopBar(
                title = title,
                onBack = onBack,
                backInteractionSource = backInteractionSource,
                modifier = Modifier.statusBarsPadding(),
            )
        },
        bottomBar = {
            MockChatInput(
                otherUserName = title,
                inputText = uiState.inputText,
                onInputChanged = viewModel::onInputChanged,
                onSend = { sendThenShow(viewModel::send) },
                onSendAsOther = { sendThenShow(viewModel::sendAsOther) },
                inputInteractionSource = inputInteractionSource,
                sendInteractionSource = sendInteractionSource,
                sendAsOtherInteractionSource = sendAsOtherInteractionSource,
                modifier = Modifier
                    .navigationBarsPadding()
                    .imePadding(),
            )
        },
        containerColor = colorBackground,
        contentWindowInsets = WindowInsets(0),
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                contentPadding = PaddingValues(horizontal = Spacing.md),
                reverseLayout = true,
            ) {
                items(messages.itemCount, key = messages.itemKey { it.key }) { index ->
                    when (val item = messages[index]) {
                        is ChatItem.Message -> MessageItem(item)
                        is ChatItem.TimestampHeader -> SectionHeader(item.label)
                        null -> Spacer(Modifier.height(UnloadedRowHeight))
                    }
                }
            }
            AnimatedVisibility(
                visible = isNewestOutOfView,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = Spacing.md, bottom = Spacing.sm),
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Button(
                    onClick = { scope.launch { listState.scrollToItem(0) } },
                    interactionSource = jumpToLatestInteractionSource,
                    variant = ButtonVariant.FLOATING,
                    icon = ImageVector.vectorResource(R.drawable.ic_arrow_down),
                    label = stringResource(R.string.jump_to_latest),
                )
            }
        }
    }
}
