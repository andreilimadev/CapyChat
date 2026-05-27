package com.andreilima.capychat.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary          = CapyPrimary,
    onPrimary        = CapyOnPrimary,
    secondary        = CapyAccentBlue,
    onSecondary      = CapyWhite,
    background       = CapyBackground,
    onBackground     = CapyTextDark,
    surface          = CapySurface,
    onSurface        = CapyTextDark,
    surfaceVariant   = CapySurfaceVariant,
    onSurfaceVariant = CapyTextMuted,
    outline          = CapyOutline,
    error            = CapyError,
    errorContainer   = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF8B1A1A)
)

private val DarkColors = darkColorScheme(
    primary          = Color(0xFFC49A6C),   // caramelo claro — legível no escuro
    onPrimary        = Color(0xFF2B1A0A),

    secondary        = Color(0xFF7BAFD4),   // azul mais suave no escuro
    onSecondary      = Color(0xFF0D1F2D),

    background       = Color(0xFF1A1612),   // marrom muito escuro, não preto puro
    onBackground     = Color(0xFFEDE5D8),   // bege claro para texto

    surface          = Color(0xFF26201A),   // um tom acima do background
    onSurface        = Color(0xFFEDE5D8),

    surfaceVariant   = Color(0xFF352C24),   // cards e inputs
    onSurfaceVariant = Color(0xFFBBAB98),   // texto secundário

    outline          = Color(0xFF5C4F42),
    error            = Color(0xFFFFB4AB),
    errorContainer   = Color(0xFF5C1A1A),
    onErrorContainer = Color(0xFFFFDAD6)
)

@Composable
fun CapyChatTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = CapyTypography,
        shapes      = CapyShapes,
        content     = content
    )
}