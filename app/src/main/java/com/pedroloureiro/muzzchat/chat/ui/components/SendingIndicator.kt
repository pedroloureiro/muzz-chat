package com.pedroloureiro.muzzchat.chat.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pedroloureiro.muzzchat.R
import com.pedroloureiro.muzzchat.ui.theme.MuzzChatTheme
import com.pedroloureiro.muzzchat.ui.theme.Spacing
import com.pedroloureiro.muzzchat.ui.theme.textSecondary

private const val DOT_COUNT = 3
private val DotSize = 6.dp
private const val DOT_DIM_ALPHA = 0.3f
private const val PULSE_MILLIS = 600
private const val STAGGER_MILLIS = 200

@Composable
fun SendingIndicator(modifier: Modifier = Modifier) {
    val description = stringResource(R.string.sending)
    val transition = rememberInfiniteTransition(label = "sendingIndicator")
    Row(
        modifier = modifier.semantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        repeat(DOT_COUNT) { index ->
            val alpha by transition.animateFloat(
                initialValue = DOT_DIM_ALPHA,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(PULSE_MILLIS),
                    repeatMode = RepeatMode.Reverse,
                    initialStartOffset = StartOffset(index * STAGGER_MILLIS),
                ),
                label = "sendingDot$index",
            )
            // Reading alpha in the draw phase redraws each frame without recomposing.
            Canvas(Modifier.size(DotSize)) { drawCircle(textSecondary, alpha = alpha) }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SendingIndicatorPreview() {
    MuzzChatTheme {
        SendingIndicator(Modifier.padding(Spacing.md))
    }
}
