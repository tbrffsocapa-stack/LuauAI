package com.luauai.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val LuauDarkColors = darkColorScheme(
    primary = LuauPrimary,
    secondary = LuauSecondary,
    background = LuauBackground,
    onBackground = LuauOnBackground,
    surface = LuauSurface,
    onSurface = LuauOnSurface,
    surfaceVariant = LuauSurfaceVariant,
    onPrimary = LuauOnPrimary,
    error = LuauError
)

@Composable
fun LuauAITheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LuauDarkColors,
        content = content
    )
}
