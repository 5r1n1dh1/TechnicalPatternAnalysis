package com.tpa.app.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

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

internal val LightTpaTokens = TpaPaletteTokens(
    background = Color(0xFFF6F8FA),
    surface = Color(0xFFF6F8FA),
    surfaceContainer = Color(0xFFFFFFFF),
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF0F172A),
    onSurfaceMuted = Color(0xFF52606D),
    primary = AccentGreen,
    secondary = AccentBlue,
    tertiary = AccentPurple,
    error = AccentRed,
    bullish = Color(0xFF34D399),
    bearish = Color(0xFFF87171),
    neutral = Color(0xFF94A3B8),
    accent = AccentGreen,
    glassFill = Color(0xCCFFFFFF),
    glassBorder = Color(0x33000000),
)

internal val DarkTpaTokens = TpaPaletteTokens(
    background = Color(0xFF0B1120),
    surface = Color(0xFF111827),
    surfaceContainer = Color(0xFF1F2937),
    onBackground = Color(0xFFFFFFFF),
    onSurface = Color(0xFFFFFFFF),
    onSurfaceMuted = Color(0xFF94A3B8),
    primary = AccentGreen,
    secondary = AccentBlue,
    tertiary = AccentPurple,
    error = AccentRed,
    bullish = Color(0xFF34D399),
    bearish = Color(0xFFF87171),
    neutral = Color(0xFF94A3B8),
    accent = AccentGreen,
    glassFill = Color(0x1AFFFFFF),
    glassBorder = Color(0x1FFFFFFF),
)

internal val LocalTpaTokens = staticCompositionLocalOf { LightTpaTokens }
