package com.andreilima.capychat.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = CapyPrimary,
    onPrimary = Color.White,
    secondary = CapySecondary,
    onSecondary = Color.White,
    background = CapyBackground,
    onBackground = CapyText,
    surface = CapySurface,
    onSurface = CapyText,
    surfaceVariant = Color(0xFFE9EDC9), // Tom levemente esverdeado para variação
    onSurfaceVariant = CapyText,
    outline = CapyOutline,
    error = CapyError
)

// Esquema de cores escuras para evitar crashes se o sistema mudar para dark mode
private val DarkColors = darkColorScheme(
    primary = Color(0xFFEBC194),
    onPrimary = Color(0xFF3D2B1F),
    secondary = Color(0xFFC7A88D),
    onSecondary = Color(0xFF3D2B1F),
    background = Color(0xFF2B2922),
    onBackground = Color(0xFFFEFAE0),
    surface = Color(0xFF35332C),
    onSurface = Color(0xFFFEFAE0),
    outline = CapyOutline,
    error = CapyError
)

@Composable
fun CapyChatTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}
