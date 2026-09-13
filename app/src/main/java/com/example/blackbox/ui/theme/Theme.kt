package com.example.blackbox.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

private val PitchDarkColorScheme = darkColorScheme(
    primary = TraceMintSuccess,
    onPrimary = Color.Black,
    primaryContainer = TraceSoftSurface,
    onPrimaryContainer = TraceMintSuccess,
    secondary = TraceMintSuccess,
    onSecondary = Color.Black,
    secondaryContainer = TraceElevatedSurface,
    onSecondaryContainer = TracePrimary,
    tertiary = TraceAmberWarning,
    onTertiary = Color.Black,
    tertiaryContainer = TraceSoftSurface,
    onTertiaryContainer = TraceAmberWarning,
    background = TraceCanvas,
    onBackground = TracePrimary,
    surface = TraceSurface,
    onSurface = TracePrimary,
    surfaceVariant = TraceElevatedSurface,
    onSurfaceVariant = TraceBody,
    outline = TraceHairline,
    outlineVariant = TraceHairlineStrong,
    error = TraceRedCritical,
    onError = Color.White
)

@Composable
fun BlackboxTheme(
    darkTheme: Boolean = true, // Precision Pitch Black visual identity by default
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalSpacing provides Spacing()) {
        MaterialTheme(
            colorScheme = PitchDarkColorScheme,
            typography = TRACETypography,
            shapes = TRACEShapes,
            content = content
        )
    }
}
