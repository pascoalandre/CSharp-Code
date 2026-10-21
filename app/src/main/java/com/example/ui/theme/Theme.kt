package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = CSharpPurple,
    onPrimary = Color.White,
    primaryContainer = CSharpPurpleDark,
    onPrimaryContainer = Color.White,
    secondary = IdeTeal,
    onSecondary = Color.Black,
    secondaryContainer = IdeSurfaceVariant,
    onSecondaryContainer = IdeTextWhite,
    tertiary = IdeYellow,
    background = IdeBackground,
    onBackground = IdeTextWhite,
    surface = IdeSurface,
    onSurface = IdeTextWhite,
    surfaceVariant = IdeSurfaceVariant,
    onSurfaceVariant = IdeTextMuted,
    outline = IdeSurfaceBorder,
    error = IdeRed,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
