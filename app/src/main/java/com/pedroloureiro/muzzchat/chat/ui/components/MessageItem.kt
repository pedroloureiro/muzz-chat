package com.pedroloureiro.muzzchat.chat.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pedroloureiro.muzzchat.chat.domain.models.MessageDeliveryStatus
import com.pedroloureiro.muzzchat.chat.ui.models.ChatItem
import com.pedroloureiro.muzzchat.chat.ui.models.MessageDirection
import com.pedroloureiro.muzzchat.ui.theme.MuzzChatTheme
import com.pedroloureiro.muzzchat.ui.theme.Spacing

private const val BUBBLE_MAX_WIDTH_FRACTION = 0.8f
private val GroupedGap = 2.dp
private val SeparatedGap = 8.dp

@Composable
fun MessageItem(item: ChatItem.Message, modifier: Modifier = Modifier) {
    val isOutgoing = item.direction == MessageDirection.OUTGOING
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = if (item.isGrouped) GroupedGap else SeparatedGap),
        horizontalArrangement = if (isOutgoing) Arrangement.End else Arrangement.Start,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(BUBBLE_MAX_WIDTH_FRACTION),
            horizontalAlignment = if (isOutgoing) Alignment.End else Alignment.Start,
        ) {
            MessageBubble(
                content = item.content,
                direction = item.direction,
                isGrouped = item.isGrouped,
                deliveryStatus = item.deliveryStatus,
            )
            AnimatedVisibility(visible = item.deliveryStatus == MessageDeliveryStatus.SENDING) {
                SendingIndicator(Modifier.padding(top = Spacing.xs))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MessageItemPreview() {
    MuzzChatTheme {
        Column(modifier = Modifier.padding(Spacing.md)) {
            listOf(
                previewMessage("Nothing much", MessageDirection.INCOMING, isGrouped = true),
                previewMessage(
                    "Actually just about to go shopping, got any recommendations for a good shoe shop?",
                    MessageDirection.INCOMING,
                    isGrouped = false,
                ),
                previewMessage("Does 7pm work?", MessageDirection.OUTGOING, isGrouped = true),
                previewMessage("Or maybe 8pm?", MessageDirection.OUTGOING, isGrouped = false),
                previewMessage(
                    "On my way!",
                    MessageDirection.OUTGOING,
                    isGrouped = false,
                    deliveryStatus = MessageDeliveryStatus.SENDING,
                ),
            ).forEach { MessageItem(it) }
        }
    }
}

private fun previewMessage(
    content: String,
    direction: MessageDirection,
    isGrouped: Boolean,
    deliveryStatus: MessageDeliveryStatus = MessageDeliveryStatus.SENT,
) = ChatItem.Message(
    key = content,
    direction = direction,
    content = content,
    timestampMillis = 0L,
    isGrouped = isGrouped,
    deliveryStatus = deliveryStatus,
)
