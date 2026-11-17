package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val FluentDarkColorScheme = darkColorScheme(
    primary = Win11AccentCyan,
    onPrimary = Win11NavyBg,
    primaryContainer = Win11AccentBlue,
    onPrimaryContainer = Win11TextPrimary,
    secondary = Win11AccentCobalt,
    onSecondary = Win11NavyBg,
    background = Win11NavyBg,
    onBackground = Win11TextPrimary,
    surface = Win11MicaSurface,
    onSurface = Win11TextPrimary,
    surfaceVariant = Win11CardSurface,
    onSurfaceVariant = Win11TextSecondary,
    outline = Win11BorderSubtle,
    error = Win11ErrorCoral
)

private val FluentShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(20.dp)
)

@Composable
fun Fluent11Theme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = FluentDarkColorScheme,
        typography = FluentTypography,
        shapes = FluentShapes,
        content = content
    )
}
