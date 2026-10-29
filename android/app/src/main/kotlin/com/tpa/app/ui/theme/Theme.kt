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
    primary = Color(0xFF34D399),
    onPrimary = Color(0xFF04140D),
    primaryContainer = Color(0xFF064E3B),
    onPrimaryContainer = Color(0xFFD1FAE5),
    secondary = Color(0xFF60A5FA),
    onSecondary = Color(0xFF06121F),
    tertiary = Color(0xFFA78BFA),
    onTertiary = Color(0xFF160A2E),
    background = Color(0xFF070B14),
    onBackground = Color(0xFFF8FAFC),
    surface = Color(0xFF0F172A),
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF1E293B),
    onSurfaceVariant = Color(0xFF94A3B8),
    surfaceContainer = Color(0xFF1E293B),
    outline = Color(0xFF475569),
    error = Color(0xFFF87171),
    onError = Color(0xFF2A0808),
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF059669),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1FAE5),
    onPrimaryContainer = Color(0xFF064E3B),
    secondary = Color(0xFF2563EB),
    onSecondary = Color.White,
    tertiary = Color(0xFF7C3AED),
    onTertiary = Color.White,
    background = Color(0xFFF4F7FB),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE9EEF5),
    onSurfaceVariant = Color(0xFF52606D),
    surfaceContainer = Color(0xFFFFFFFF),
    outline = Color(0xFFCBD5E1),
    error = Color(0xFFDC2626),
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
