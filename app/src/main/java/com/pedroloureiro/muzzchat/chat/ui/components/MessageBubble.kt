package com.pedroloureiro.muzzchat.chat.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pedroloureiro.muzzchat.chat.domain.models.MessageDeliveryStatus
import com.pedroloureiro.muzzchat.chat.ui.models.MessageDirection
import com.pedroloureiro.muzzchat.ui.theme.MuzzChatTheme
import com.pedroloureiro.muzzchat.ui.theme.Spacing
import com.pedroloureiro.muzzchat.ui.theme.colorPrimary
import com.pedroloureiro.muzzchat.ui.theme.colorSurfaceVariant
import com.pedroloureiro.muzzchat.ui.theme.textOnPrimary
import com.pedroloureiro.muzzchat.ui.theme.textPrimary

private val CornerRadius = 18.dp
private val TailCornerRadius = 4.dp
private val SentTailShape = RoundedCornerShape(
    topStart = CornerRadius,
    topEnd = CornerRadius,
    bottomEnd = TailCornerRadius,
    bottomStart = CornerRadius,
)
private val ReceivedTailShape = RoundedCornerShape(
    topStart = CornerRadius,
    topEnd = CornerRadius,
    bottomEnd = CornerRadius,
    bottomStart = TailCornerRadius,
)
private val GroupedShape = RoundedCornerShape(CornerRadius)
private val PaddingHorizontal = 12.dp
private val PaddingVertical = 8.dp
private const val SENDING_ALPHA = 0.5f

@Composable
fun MessageBubble(
    content: String,
    direction: MessageDirection,
    isGrouped: Boolean,
    deliveryStatus: MessageDeliveryStatus,
    modifier: Modifier = Modifier,
) {
    val isOutgoing = direction == MessageDirection.OUTGOING
    val shape = when {
        isGrouped -> GroupedShape
        isOutgoing -> SentTailShape
        else -> ReceivedTailShape
    }
    val alpha by animateFloatAsState(
        targetValue = if (deliveryStatus == MessageDeliveryStatus.SENDING) SENDING_ALPHA else 1f,
        label = "messageBubbleAlpha",
    )
    Text(
        text = content,
        modifier = modifier
            .alpha(alpha)
            .background(if (isOutgoing) colorPrimary else colorSurfaceVariant, shape)
            .padding(horizontal = PaddingHorizontal, vertical = PaddingVertical),
        style = MaterialTheme.typography.bodyMedium,
        color = if (isOutgoing) textOnPrimary else textPrimary,
    )
}

@Preview(showBackground = true)
@Composable
private fun MessageBubblePreview() {
    MuzzChatTheme {
        Column(
            modifier = Modifier.padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            listOf(
                Triple("Does 7pm work for you?", MessageDirection.OUTGOING, false),
                Triple("Yeh for sure, sounds fun", MessageDirection.INCOMING, false),
                Triple("What are you up to?", MessageDirection.OUTGOING, true),
                Triple("Nothing much", MessageDirection.INCOMING, true),
            ).forEach { (content, direction, isGrouped) ->
                MessageBubble(
                    content = content,
                    direction = direction,
                    isGrouped = isGrouped,
                    deliveryStatus = MessageDeliveryStatus.SENT,
                    modifier = Modifier.align(
                        if (direction == MessageDirection.OUTGOING) Alignment.End else Alignment.Start
                    ),
                )
            }
            MessageBubble(
                content = "On my way!",
                direction = MessageDirection.OUTGOING,
                isGrouped = false,
                deliveryStatus = MessageDeliveryStatus.SENDING,
                modifier = Modifier.align(Alignment.End),
            )
        }
    }
}
