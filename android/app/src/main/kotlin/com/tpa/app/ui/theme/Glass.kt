package com.tpa.app.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val GlassWhite = Color(0xFFFFFFFF)
private val GlassBorder = Color(0x1FFFFFFF) // 12% white

/**
 * Glassmorphism surface: 8% white alpha fill, 1dp 12%-white border, 20dp corner radius.
 * Optional [elevation] adds a soft outer shadow for depth layering.
 */
@Composable
fun Modifier.glassSurface(
    elevation: Float = 0f
): Modifier {
    var m = this
        .clip(RoundedCornerShape(20.dp))
        .background(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(0x0DFFFFFF),
                    Color(0x08FFFFFF)
                )
            )
        )
        .border(
            width = 1.dp,
            color = GlassBorder,
            shape = RoundedCornerShape(20.dp)
        )
    if (elevation > 0f) {
        m = m.background(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0x1A000000),
                    Color(0x00000000)
                ),
                radius = elevation * 20f
            )
        )
    }
    return m
}

@Composable
fun Modifier.glassCard(): Modifier = glassSurface(elevation = 4f)
