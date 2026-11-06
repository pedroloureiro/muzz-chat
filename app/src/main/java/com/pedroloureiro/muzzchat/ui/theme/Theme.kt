package com.pedroloureiro.muzzchat.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = colorPrimary,
    onPrimary = textOnPrimary,
    background = colorBackground,
    onBackground = textPrimary,
    surface = colorSurface,
    onSurface = textPrimary,
    surfaceVariant = colorSurfaceVariant,
    onSurfaceVariant = textSecondary,
    outline = colorBorder,
)

@Composable
fun MuzzChatTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}
