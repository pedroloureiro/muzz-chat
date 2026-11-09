package com.pedroloureiro.muzzchat.chat.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import com.pedroloureiro.muzzchat.R
import com.pedroloureiro.muzzchat.ui.components.Button
import com.pedroloureiro.muzzchat.ui.components.ButtonVariant
import com.pedroloureiro.muzzchat.ui.theme.MuzzChatTheme
import com.pedroloureiro.muzzchat.ui.theme.Spacing

// MOCK DATA: Sarah has no device in this exercise, so her messages are sent from this one.
// The real app shows ChatInput alone.
@Composable
fun MockChatInput(
    otherUserName: String,
    inputText: String,
    onInputChanged: (String) -> Unit,
    onSend: () -> Unit,
    onSendAsOther: () -> Unit,
    inputInteractionSource: MutableInteractionSource,
    sendInteractionSource: MutableInteractionSource,
    sendAsOtherInteractionSource: MutableInteractionSource,
    modifier: Modifier = Modifier,
) {
    ChatInput(
        inputText = inputText,
        onInputChanged = onInputChanged,
        onSend = onSend,
        inputInteractionSource = inputInteractionSource,
        sendInteractionSource = sendInteractionSource,
        modifier = modifier,
    ) {
        Button(
            onClick = onSendAsOther,
            interactionSource = sendAsOtherInteractionSource,
            variant = ButtonVariant.FLOATING,
            icon = ImageVector.vectorResource(R.drawable.ic_send_as_other),
            label = stringResource(R.string.reply_as, otherUserName),
            enabled = inputText.isNotBlank(),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MockChatInputPreview() {
    MuzzChatTheme {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            listOf("", "Hey, Sara looks great").forEach { inputText ->
                MockChatInput(
                    otherUserName = "Sarah",
                    inputText = inputText,
                    onInputChanged = {},
                    onSend = {},
                    onSendAsOther = {},
                    inputInteractionSource = remember { MutableInteractionSource() },
                    sendInteractionSource = remember { MutableInteractionSource() },
                    sendAsOtherInteractionSource = remember { MutableInteractionSource() },
                )
            }
        }
    }
}
