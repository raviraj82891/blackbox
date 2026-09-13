package com.example.blackbox.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class Spacing(
    val space4: Dp = 4.dp,
    val space8: Dp = 8.dp,
    val space12: Dp = 12.dp,
    val space16: Dp = 16.dp,
    val space24: Dp = 24.dp,
    val space32: Dp = 32.dp,
    val space40: Dp = 40.dp,
    val space64: Dp = 64.dp,
    val space96: Dp = 96.dp
)

val LocalSpacing = staticCompositionLocalOf { Spacing() }

val TraceSpacing: Spacing
    @Composable
    @ReadOnlyComposable
    get() = LocalSpacing.current
