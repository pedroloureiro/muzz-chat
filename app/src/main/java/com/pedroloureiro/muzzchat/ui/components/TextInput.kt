package com.pedroloureiro.muzzchat.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.tooling.preview.Preview
import com.pedroloureiro.muzzchat.ui.theme.Alpha
import com.pedroloureiro.muzzchat.ui.theme.ComponentShapes
import com.pedroloureiro.muzzchat.ui.theme.Dimens
import com.pedroloureiro.muzzchat.ui.theme.MuzzChatTheme
import com.pedroloureiro.muzzchat.ui.theme.Spacing
import com.pedroloureiro.muzzchat.ui.theme.colorInputBackground
import com.pedroloureiro.muzzchat.ui.theme.colorPrimary
import com.pedroloureiro.muzzchat.ui.theme.textDisabled
import com.pedroloureiro.muzzchat.ui.theme.textPrimary
import com.pedroloureiro.muzzchat.ui.theme.textSecondary

@Composable
fun TextInput(
    value: String,
    onValueChange: (String) -> Unit,
    interactionSource: MutableInteractionSource,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    enabled: Boolean = true,
) {
    val isFocused by interactionSource.collectIsFocusedAsState()
    val shape = ComponentShapes.textInput
    val textStyle = MaterialTheme.typography.bodyMedium
    val focusRing = if (isFocused) {
        Modifier.border(Dimens.focusRingWidth, colorPrimary.copy(alpha = Alpha.focusRing), shape)
    } else {
        Modifier
    }
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .alpha(if (enabled) 1f else Alpha.disabled)
            .background(colorInputBackground, shape)
            .then(focusRing),
        enabled = enabled,
        textStyle = textStyle.copy(color = if (enabled) textPrimary else textDisabled),
        interactionSource = interactionSource,
        cursorBrush = SolidColor(colorPrimary),
        decorationBox = { innerTextField ->
            Box(modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm)) {
                if (value.isEmpty()) {
                    Text(text = placeholder, style = textStyle, color = textSecondary)
                }
                innerTextField()
            }
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun TextInputPreview() {
    MuzzChatTheme {
        Column(
            modifier = Modifier.padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            listOf(
                "" to true,
                "Hey, Sara looks great" to true,
                "Disabled" to false,
            ).forEach { (value, enabled) ->
                TextInput(
                    value = value,
                    onValueChange = {},
                    interactionSource = remember { MutableInteractionSource() },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = "Message…",
                    enabled = enabled,
                )
            }
        }
    }
}
