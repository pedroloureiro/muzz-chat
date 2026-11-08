package com.pedroloureiro.muzzchat.chat.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
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
import com.pedroloureiro.muzzchat.ui.components.TextInput
import com.pedroloureiro.muzzchat.ui.theme.Dimens
import com.pedroloureiro.muzzchat.ui.theme.MuzzChatTheme
import com.pedroloureiro.muzzchat.ui.theme.Spacing
import com.pedroloureiro.muzzchat.ui.theme.colorBorder
import com.pedroloureiro.muzzchat.ui.theme.colorSurface

@Composable
fun ChatInput(
    inputText: String,
    onInputChanged: (String) -> Unit,
    onSend: () -> Unit,
    onSendAsOther: () -> Unit,
    inputInteractionSource: MutableInteractionSource,
    sendInteractionSource: MutableInteractionSource,
    sendAsOtherInteractionSource: MutableInteractionSource,
    modifier: Modifier = Modifier,
) {
    val canSend = inputText.isNotBlank()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colorSurface),
    ) {
        HorizontalDivider(thickness = Dimens.dividerWidth, color = colorBorder)
        Column(
            modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Button(
                onClick = onSendAsOther,
                interactionSource = sendAsOtherInteractionSource,
                modifier = Modifier.align(Alignment.End),
                variant = ButtonVariant.SECONDARY,
                icon = ImageVector.vectorResource(R.drawable.ic_reply),
                label = stringResource(R.string.reply_as, Users.SARAH.name),
                enabled = canSend,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                TextInput(
                    value = inputText,
                    onValueChange = onInputChanged,
                    interactionSource = inputInteractionSource,
                    modifier = Modifier.weight(1f),
                    placeholder = stringResource(R.string.message_placeholder),
                )
                Button(
                    onClick = onSend,
                    interactionSource = sendInteractionSource,
                    icon = ImageVector.vectorResource(R.drawable.ic_send),
                    label = stringResource(R.string.send),
                    enabled = canSend,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ChatInputPreview() {
    MuzzChatTheme {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            listOf("", "Hey, Sara looks great").forEach { inputText ->
                ChatInput(
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
