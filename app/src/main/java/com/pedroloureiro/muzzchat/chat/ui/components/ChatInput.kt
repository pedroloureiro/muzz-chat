package com.pedroloureiro.muzzchat.chat.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
import com.pedroloureiro.muzzchat.ui.components.Button
import com.pedroloureiro.muzzchat.ui.components.TextInput
import com.pedroloureiro.muzzchat.ui.theme.Dimens
import com.pedroloureiro.muzzchat.ui.theme.MuzzChatTheme
import com.pedroloureiro.muzzchat.ui.theme.Spacing
import com.pedroloureiro.muzzchat.ui.theme.colorBorder
import com.pedroloureiro.muzzchat.ui.theme.colorSurface

private const val MAX_INPUT_LINES = 8

@Composable
fun ChatInput(
    inputText: String,
    onInputChanged: (String) -> Unit,
    onSend: () -> Unit,
    inputInteractionSource: MutableInteractionSource,
    sendInteractionSource: MutableInteractionSource,
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colorSurface),
    ) {
        HorizontalDivider(thickness = Dimens.dividerWidth, color = colorBorder)
        Row(
            modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            TextInput(
                value = inputText,
                onValueChange = onInputChanged,
                interactionSource = inputInteractionSource,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = Dimens.buttonPrimarySize),
                placeholder = stringResource(R.string.message_placeholder),
                maxLines = MAX_INPUT_LINES,
            )
            Button(
                onClick = onSend,
                interactionSource = sendInteractionSource,
                icon = ImageVector.vectorResource(R.drawable.ic_send),
                label = stringResource(R.string.send),
                enabled = inputText.isNotBlank(),
            )
            trailing()
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ChatInputPreview() {
    MuzzChatTheme {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            listOf("", "Hey, Sara looks great", (1..10).joinToString("\n") { "Line $it" }).forEach { inputText ->
                ChatInput(
                    inputText = inputText,
                    onInputChanged = {},
                    onSend = {},
                    inputInteractionSource = remember { MutableInteractionSource() },
                    sendInteractionSource = remember { MutableInteractionSource() },
                )
            }
        }
    }
}
