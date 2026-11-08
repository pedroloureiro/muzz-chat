package com.pedroloureiro.muzzchat.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import com.pedroloureiro.muzzchat.R
import com.pedroloureiro.muzzchat.ui.theme.Alpha
import com.pedroloureiro.muzzchat.ui.theme.ComponentShapes
import com.pedroloureiro.muzzchat.ui.theme.Dimens
import com.pedroloureiro.muzzchat.ui.theme.MuzzChatTheme
import com.pedroloureiro.muzzchat.ui.theme.Scale
import com.pedroloureiro.muzzchat.ui.theme.Spacing
import com.pedroloureiro.muzzchat.ui.theme.colorBorder
import com.pedroloureiro.muzzchat.ui.theme.colorBorderPressed
import com.pedroloureiro.muzzchat.ui.theme.colorInputBackground
import com.pedroloureiro.muzzchat.ui.theme.colorPrimary
import com.pedroloureiro.muzzchat.ui.theme.colorPrimaryDisabled
import com.pedroloureiro.muzzchat.ui.theme.colorPrimaryPressed
import com.pedroloureiro.muzzchat.ui.theme.colorSurface
import com.pedroloureiro.muzzchat.ui.theme.textDisabled
import com.pedroloureiro.muzzchat.ui.theme.textOnPrimary
import com.pedroloureiro.muzzchat.ui.theme.textPrimary
import com.pedroloureiro.muzzchat.ui.theme.textSecondary

enum class ButtonVariant { PRIMARY, SECONDARY, FLOATING }

@Composable
fun Button(
    onClick: () -> Unit,
    interactionSource: MutableInteractionSource,
    modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.PRIMARY,
    icon: ImageVector? = null,
    label: String? = null,
    enabled: Boolean = true,
) {
    when (variant) {
        ButtonVariant.PRIMARY -> PrimaryButton(onClick, interactionSource, modifier, icon, label, enabled)
        ButtonVariant.SECONDARY -> SecondaryButton(onClick, interactionSource, modifier, icon, label, enabled)
        ButtonVariant.FLOATING -> FloatingButton(onClick, interactionSource, modifier, icon, label, enabled)
    }
}

@Composable
private fun PrimaryButton(
    onClick: () -> Unit,
    interactionSource: MutableInteractionSource,
    modifier: Modifier,
    icon: ImageVector?,
    contentDescription: String?,
    enabled: Boolean,
) {
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) Scale.buttonPrimaryPressed else 1f,
        label = "primaryButtonScale",
    )
    val background = when {
        !enabled -> colorPrimaryDisabled
        isPressed -> colorPrimaryPressed
        else -> colorPrimary
    }
    Box(
        modifier = modifier
            .scale(scale)
            .size(Dimens.buttonPrimarySize)
            .background(background, ComponentShapes.buttonPrimary)
            .clickable(interactionSource, indication = null, enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(Dimens.iconSize),
                tint = if (enabled) textOnPrimary else textDisabled,
            )
        }
    }
}

@Composable
private fun SecondaryButton(
    onClick: () -> Unit,
    interactionSource: MutableInteractionSource,
    modifier: Modifier,
    icon: ImageVector?,
    label: String?,
    enabled: Boolean,
) {
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) Scale.buttonSecondaryPressed else 1f,
        label = "secondaryButtonScale",
    )
    val shape = ComponentShapes.buttonSecondary
    val contentColor = if (enabled) textSecondary else textDisabled
    Row(
        modifier = modifier
            .scale(scale)
            .alpha(if (enabled) 1f else Alpha.disabled)
            .background(if (isPressed) colorInputBackground else colorSurface, shape)
            .border(Dimens.borderWidth, if (isPressed) colorBorderPressed else colorBorder, shape)
            .clickable(interactionSource, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(Dimens.iconSize),
                tint = contentColor,
            )
        }
        if (label != null) {
            Text(text = label, style = MaterialTheme.typography.labelLarge, color = contentColor)
        }
    }
}

@Composable
private fun FloatingButton(
    onClick: () -> Unit,
    interactionSource: MutableInteractionSource,
    modifier: Modifier,
    icon: ImageVector?,
    contentDescription: String?,
    enabled: Boolean,
) {
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) Scale.buttonFloatingPressed else 1f,
        label = "floatingButtonScale",
    )
    val shape = ComponentShapes.buttonFloating
    Box(
        modifier = modifier
            .scale(scale)
            .alpha(if (enabled) 1f else Alpha.disabled)
            .size(Dimens.buttonPrimarySize)
            .background(if (isPressed) colorInputBackground else colorSurface, shape)
            .border(Dimens.borderWidth, if (isPressed) colorBorderPressed else colorBorder, shape)
            .clickable(interactionSource, indication = null, enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(Dimens.iconSize),
                tint = if (enabled) textPrimary else textDisabled,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PrimaryButtonPreview() {
    MuzzChatTheme {
        Row(
            modifier = Modifier.padding(Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            listOf(true, false).forEach { enabled ->
                Button(
                    onClick = {},
                    interactionSource = remember { MutableInteractionSource() },
                    icon = ImageVector.vectorResource(R.drawable.ic_send),
                    label = "Send",
                    enabled = enabled,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SecondaryButtonPreview() {
    MuzzChatTheme {
        Row(
            modifier = Modifier.padding(Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            listOf(true, false).forEach { enabled ->
                Button(
                    onClick = {},
                    interactionSource = remember { MutableInteractionSource() },
                    variant = ButtonVariant.SECONDARY,
                    icon = ImageVector.vectorResource(R.drawable.ic_reply),
                    label = "Reply as Sarah",
                    enabled = enabled,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FloatingButtonPreview() {
    MuzzChatTheme {
        Row(
            modifier = Modifier.padding(Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            listOf(true, false).forEach { enabled ->
                Button(
                    onClick = {},
                    interactionSource = remember { MutableInteractionSource() },
                    variant = ButtonVariant.FLOATING,
                    icon = ImageVector.vectorResource(R.drawable.ic_arrow_down),
                    label = "Jump to latest",
                    enabled = enabled,
                )
            }
        }
    }
}
