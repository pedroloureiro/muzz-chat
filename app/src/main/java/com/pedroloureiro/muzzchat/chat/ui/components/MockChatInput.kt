package com.pedroloureiro.muzzchat.chat.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import com.pedroloureiro.muzzchat.R
import com.pedroloureiro.muzzchat.chat.domain.models.Users
import com.pedroloureiro.muzzchat.ui.components.Button
import com.pedroloureiro.muzzchat.ui.components.ButtonVariant
import com.pedroloureiro.muzzchat.ui.theme.MuzzChatTheme
import com.pedroloureiro.muzzchat.ui.theme.Spacing
import com.pedroloureiro.muzzchat.ui.theme.colorSurface

// MOCK DATA: Sarah has no device in this exercise, so her messages are sent from this one.
// The real app shows ChatInput alone.
@Composable
fun MockChatInput(
    inputText: String,
    onInputChanged: (String) -> Unit,
    onSend: () -> Unit,
    onSendAsOther: () -> Unit,
    inputInteractionSource: MutableInteractionSource,
    sendInteractionSource: MutableInteractionSource,
    sendAsOtherInteractionSource: MutableInteractionSource,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colorSurface),
    ) {
        Button(
            onClick = onSendAsOther,
            interactionSource = sendAsOtherInteractionSource,
            modifier = Modifier
                .align(Alignment.End)
                .padding(horizontal = Spacing.md)
                .padding(top = Spacing.sm),
            variant = ButtonVariant.SECONDARY,
            icon = ImageVector.vectorResource(R.drawable.ic_reply),
            label = stringResource(R.string.reply_as, Users.SARAH.name),
            enabled = inputText.isNotBlank(),
        )
        ChatInput(
            inputText = inputText,
            onInputChanged = onInputChanged,
            onSend = onSend,
            inputInteractionSource = inputInteractionSource,
            sendInteractionSource = sendInteractionSource,
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
