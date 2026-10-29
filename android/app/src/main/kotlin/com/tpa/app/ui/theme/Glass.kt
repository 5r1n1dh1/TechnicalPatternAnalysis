package com.tpa.app.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.hazeChild

private val CardShape = RoundedCornerShape(24.dp)
private val BarShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)

/**
 * Translucent frosted panel. Reads [LocalTpaTokens] so light and dark both get
 * the correct fill, border and highlight.
 */
@Composable
fun Modifier.glassSurface(
    shape: Shape = CardShape,
    alphaScale: Float = 1f
): Modifier {
    val tokens = LocalTpaTokens.current
    val base = tokens.glassFill
    val fill = base.copy(alpha = (base.alpha * alphaScale).coerceIn(0f, 1f))
    val bottom = fill.copy(alpha = fill.alpha * 0.55f)
    return this
        .clip(shape)
        .background(Brush.verticalGradient(listOf(fill, bottom)))
        .border(width = 1.dp, color = tokens.glassBorder, shape = shape)
}

@Composable
fun Modifier.glassCard(): Modifier = glassSurface(CardShape)

/**
 * Bottom bar: blurs the content behind it with Haze, then tints the result.
 * The tint is drawn over the blurred backdrop, never instead of it.
 */
@Composable
fun Modifier.glassBar(hazeState: HazeState): Modifier {
    val tokens = LocalTpaTokens.current
    val tintAlpha = if (tokens.isDark) 0.45f else 0.62f
    val tint = tokens.surface.copy(alpha = tintAlpha)
    val style = HazeStyle(
        tint = tint,
        blurRadius = 26.dp,
        noiseFactor = 0f
    )
    return this
        .hazeChild(hazeState, BarShape, style)
        .clip(BarShape)
        .background(
            Brush.verticalGradient(
                listOf(
                    tint.copy(alpha = tintAlpha * 0.92f),
                    tint.copy(alpha = tintAlpha)
                )
            )
        )
        .border(1.dp, tokens.glassBorder, BarShape)
}
