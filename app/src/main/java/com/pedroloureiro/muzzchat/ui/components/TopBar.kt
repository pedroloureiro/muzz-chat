package com.pedroloureiro.muzzchat.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pedroloureiro.muzzchat.R
import com.pedroloureiro.muzzchat.ui.theme.Dimens
import com.pedroloureiro.muzzchat.ui.theme.MuzzChatTheme
import com.pedroloureiro.muzzchat.ui.theme.Spacing
import com.pedroloureiro.muzzchat.ui.theme.colorBorder
import com.pedroloureiro.muzzchat.ui.theme.colorInputBackground
import com.pedroloureiro.muzzchat.ui.theme.colorSurface
import com.pedroloureiro.muzzchat.ui.theme.textPrimary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

private val BackButtonSize = 36.dp

@Composable
fun TopBar(
    title: String,
    onBack: () -> Unit,
    backInteractionSource: MutableInteractionSource,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colorSurface),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BackButton(onClick = onBack, interactionSource = backInteractionSource)
            Avatar(name = title, modifier = Modifier.padding(start = Spacing.xs))
            Text(
                text = title,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = Spacing.sm),
                style = MaterialTheme.typography.titleMedium,
                color = textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        HorizontalDivider(thickness = Dimens.dividerWidth, color = colorBorder)
    }
}

@Composable
private fun BackButton(onClick: () -> Unit, interactionSource: MutableInteractionSource) {
    val isPressed by interactionSource.collectIsPressedAsState()
    Box(
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .size(BackButtonSize)
            .background(if (isPressed) colorInputBackground else colorSurface, CircleShape)
            .clickable(interactionSource, indication = null, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(R.drawable.ic_back),
            contentDescription = stringResource(R.string.back),
            modifier = Modifier.size(Dimens.iconSize),
            tint = textPrimary,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TopBarPreview() {
    MuzzChatTheme {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            listOf(remember { MutableInteractionSource() }, PressedInteractionSource).forEach { source ->
                TopBar(title = "Sarah", onBack = {}, backInteractionSource = source)
            }
        }
    }
}

// A static preview never delivers a press, so this source reports one from the start.
private object PressedInteractionSource : MutableInteractionSource {
    override val interactions: Flow<Interaction> = flowOf(PressInteraction.Press(Offset.Zero))
    override suspend fun emit(interaction: Interaction) {}
    override fun tryEmit(interaction: Interaction) = true
}
