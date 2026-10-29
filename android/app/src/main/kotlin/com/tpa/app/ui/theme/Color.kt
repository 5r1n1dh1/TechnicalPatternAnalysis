package com.tpa.app.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

internal data class TpaPaletteTokens(
    val isDark: Boolean,
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
    val warning: Color,
    val outline: Color,
    val bullish: Color,
    val bearish: Color,
    val neutral: Color,
    val accent: Color,
    val glassFill: Color,
    val glassBorder: Color,
)

internal val LightTpaTokens = TpaPaletteTokens(
    isDark = false,
    background = Color(0xFFF4F7FB),
    surface = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFFFFFFF),
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF0F172A),
    onSurfaceMuted = Color(0xFF52606D),
    primary = AccentGreen,
    secondary = AccentBlue,
    tertiary = AccentPurple,
    error = AccentRed,
    warning = Color(0xFFB45309),
    outline = Color(0xFF0F172A),
    bullish = Color(0xFF059669),
    bearish = Color(0xFFDC2626),
    neutral = Color(0xFF64748B),
    accent = AccentGreen,
    glassFill = Color(0xE6FFFFFF),
    glassBorder = Color(0x1A0F172A),
)

internal val DarkTpaTokens = TpaPaletteTokens(
    isDark = true,
    background = Color(0xFF070B14),
    surface = Color(0xFF0F172A),
    surfaceContainer = Color(0xFF1E293B),
    onBackground = Color(0xFFF8FAFC),
    onSurface = Color(0xFFF8FAFC),
    onSurfaceMuted = Color(0xFF94A3B8),
    primary = Color(0xFF34D399),
    secondary = Color(0xFF60A5FA),
    tertiary = Color(0xFFA78BFA),
    error = Color(0xFFF87171),
    warning = Color(0xFFFBBF24),
    outline = Color(0xFFF8FAFC),
    bullish = Color(0xFF34D399),
    bearish = Color(0xFFF87171),
    neutral = Color(0xFF94A3B8),
    accent = Color(0xFF34D399),
    glassFill = Color(0x14FFFFFF),
    glassBorder = Color(0x24FFFFFF),
)

internal val LocalTpaTokens = staticCompositionLocalOf { LightTpaTokens }
