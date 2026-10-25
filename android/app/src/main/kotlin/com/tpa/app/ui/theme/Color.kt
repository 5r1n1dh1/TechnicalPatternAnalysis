package com.tpa.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.lightColorScheme

private val AccentGreen = Color(0xFF059669)
private val AccentRed = Color(0xFFDC2626)
private val AccentBlue = Color(0xFF3B82F6)
private val AccentYellow = Color(0xFFFBBF24)
private val AccentPurple = Color(0xFF8B5CF6)
private val AccentOrange = Color(0xFFF97316)

internal data class TpaPaletteTokens(
    val background: Color,
    val surface: Color,
    val surfaceContainer: Color,
    val onBackground: Color,
    val onSurface: Color,
    val onSurfaceMuted: Color,
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val error: Color,
    val bullish: Color,
    val bearish: Color,
    val neutral: Color,
    val accent: Color,
    val glassFill: Color,
    val glassBorder: Color,
)

internal val lightPalettes: Map<String, ColorScheme> = mapOf(
    "surface" to lightColorScheme(
                primary = AccentGreen,
                secondary = AccentBlue,
                tertiary = AccentPurple,
                background = Color(0xFFF6F8FA),
                surface = Color(0xFFF6F8FA),
                surfaceContainer = Color(0xFFFFFFFF),
                error = AccentRed,
                onBackground = Color(0xFF0F172A),
                onSurface = Color(0xFF0F172A),
                onSurfaceMuted = Color(0xFF52606D),
                onError = Color(0xFFFFFFFF),
            )
)

internal fun lightScheme(): ColorScheme = lightPalettes["surface"]!!
