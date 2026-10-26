package com.tpa.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

val AccentGreen = Color(0xFF059669)
val AccentRed = Color(0xFFDC2626)
val AccentBlue = Color(0xFF3B82F6)
val AccentYellow = Color(0xFFFBBF24)
val AccentPurple = Color(0xFF8B5CF6)
val AccentOrange = Color(0xFFF97316)

private val DarkColorScheme = darkColorScheme(
    primary = AccentGreen,
    secondary = AccentBlue,
    tertiary = AccentPurple,
    background = Color(0xFF0B1120),
    surface = Color(0xFF111827),
    surfaceContainer = Color(0xFF1F2937),
    error = AccentRed,
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White,
    onError = Color.Black,
)

private val LightColorScheme = lightColorScheme(
    primary = AccentGreen,
    secondary = AccentBlue,
    tertiary = AccentPurple,
    background = Color(0xFFF6F8FA),
    surface = Color(0xFFF6F8FA),
    surfaceContainer = Color(0xFFFFFFFF),
    error = AccentRed,
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF0F172A),
    onError = Color.White,
)

@Composable
fun TechnicalPatternAnalysisTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val tokens = if (darkTheme) DarkTpaTokens else LightTpaTokens
    CompositionLocalProvider(LocalTpaTokens provides tokens) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
