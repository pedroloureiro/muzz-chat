package com.pedroloureiro.muzzchat.chat.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pedroloureiro.muzzchat.R
import com.pedroloureiro.muzzchat.ui.theme.MuzzChatTheme
import com.pedroloureiro.muzzchat.ui.theme.Spacing
import com.pedroloureiro.muzzchat.ui.theme.colorSurfaceVariant
import com.pedroloureiro.muzzchat.ui.theme.textPrimary

private val PillShape = RoundedCornerShape(percent = 50)
private val PillHorizontalPadding = 12.dp
private val PillVerticalPadding = 4.dp

@Composable
fun StartOfChatMarker(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = Spacing.sm),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.start_of_chat),
            modifier = Modifier
                .background(colorSurfaceVariant, PillShape)
                .padding(horizontal = PillHorizontalPadding, vertical = PillVerticalPadding),
            style = MaterialTheme.typography.labelSmall,
            color = textPrimary,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun StartOfChatMarkerPreview() {
    MuzzChatTheme {
        StartOfChatMarker()
    }
}
