package com.example.blackbox.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = Mint,
    onPrimary = Color.White,
    primaryContainer = SoftGreenContainer,
    onPrimaryContainer = DarkTeal,
    secondary = SoftTeal,
    onSecondary = Color.White,
    secondaryContainer = SoftBlueContainer,
    onSecondaryContainer = DarkTeal,
    tertiary = WarmCoral,
    onTertiary = Color.White,
    tertiaryContainer = SoftCoralContainer,
    background = OffWhite,
    onBackground = CharcoalText,
    surface = CardWhite,
    onSurface = CharcoalText,
    surfaceVariant = SurfaceTint,
    onSurfaceVariant = MutedSlate,
    error = WarmCoral,
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = Mint,
    onPrimary = Color.White,
    primaryContainer = DarkTeal,
    secondary = SoftTeal,
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    onSurface = Color.White,
    error = WarmCoral
)

@Composable
fun BlackboxTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = TRACETypography,
        shapes = TRACEShapes,
        content = content
    )
}
