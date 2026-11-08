package com.pedroloureiro.muzzchat.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pedroloureiro.muzzchat.ui.theme.MuzzChatTheme
import com.pedroloureiro.muzzchat.ui.theme.Spacing
import com.pedroloureiro.muzzchat.ui.theme.colorPrimary
import com.pedroloureiro.muzzchat.ui.theme.textOnPrimary

private val AvatarSize = 36.dp

@Composable
fun Avatar(name: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(AvatarSize)
            .background(colorPrimary, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = name.first().uppercaseChar().toString(),
            style = MaterialTheme.typography.labelLarge,
            color = textOnPrimary,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AvatarPreview() {
    MuzzChatTheme {
        Avatar(name = "Sarah", modifier = Modifier.padding(Spacing.md))
    }
}
