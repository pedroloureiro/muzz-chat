package com.pedroloureiro.muzzchat.chat.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.pedroloureiro.muzzchat.ui.theme.MuzzChatTheme
import com.pedroloureiro.muzzchat.ui.theme.Spacing
import com.pedroloureiro.muzzchat.ui.theme.textSecondary

@Composable
fun SectionHeader(label: String, modifier: Modifier = Modifier) {
    Text(
        text = label,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.sm),
        style = MaterialTheme.typography.labelSmall,
        color = textSecondary,
        textAlign = TextAlign.Center,
    )
}

@Preview(showBackground = true)
@Composable
private fun SectionHeaderPreview() {
    MuzzChatTheme {
        SectionHeader(label = "Thursday 11:59")
    }
}
