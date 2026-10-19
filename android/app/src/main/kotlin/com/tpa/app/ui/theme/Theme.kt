package com.tpa.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Accent tokens per design spec
val AccentGreen = Color(0xFF34D399)
val AccentRed = Color(0xFFFF4444)
val AccentBlue = Color(0xFF3B82F6)
val AccentYellow = Color(0xFFFBBF24)
val AccentPurple = Color(0xFF8B5CF6)
val AccentOrange = Color(0xFFF97316)

private val DarkColorScheme = darkColorScheme(
    primary = AccentGreen,
    secondary = AccentBlue,
    tertiary = AccentPurple,
    background = Color.Black,
    surface = Color.Black,
    error = AccentRed,
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White,
    onError = Color.Black
)

private val LightColorScheme = lightColorScheme(
    primary = AccentGreen,
    secondary = AccentBlue,
    tertiary = AccentPurple,
    background = Color.Black,
    surface = Color.Black,
    error = AccentRed,
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White,
    onError = Color.Black
)

@Composable
fun TechnicalPatternAnalysisTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme // Force dark theme for glassmorphism

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}