package com.tennis.server.ui.theme

import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val DarkBlue = Color(0xFF1A1A2E)
val DarkBlueDarker = Color(0xFF16213E)
val TennisGreen = Color(0xFF2ECC71)
val GoldYellow = Color(0xFFF1C40F)
val LightText = Color(0xFFE0E0E0)
val DarkText = Color(0xFF121212)

private val AppColors = darkColors(
    primary = TennisGreen,
    primaryVariant = Color(0xFF27AE60),
    secondary = GoldYellow,
    background = DarkBlue,
    surface = DarkBlueDarker,
    onPrimary = DarkText,
    onSecondary = DarkText,
    onBackground = LightText,
    onSurface = LightText
)

@Composable
fun TennisServerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colors = AppColors,
        content = content
    )
}
