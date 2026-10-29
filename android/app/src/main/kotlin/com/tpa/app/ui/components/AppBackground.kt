package com.tpa.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme

/** Soft aurora backdrop. Drawing gradients avoids extra blur layers. */
@Composable
fun AppBackground() {
    val scheme = MaterialTheme.colorScheme
    val tokens = com.tpa.app.ui.theme.LocalTpaTokens.current
    Canvas(modifier = Modifier.fillMaxSize().background(scheme.background)) {
        drawRect(
            brush = Brush.verticalGradient(
                listOf(
                    scheme.background,
                    scheme.surface.copy(alpha = if (tokens.isDark) 0.65f else 0.9f),
                    scheme.background
                )
            )
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(tokens.primary.copy(alpha = 0.20f), Color.Transparent),
                center = Offset(size.width * 0.92f, size.height * 0.06f),
                radius = size.minDimension * 0.75f
            ),
            radius = size.minDimension * 0.75f,
            center = Offset(size.width * 0.92f, size.height * 0.06f)
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(tokens.secondary.copy(alpha = 0.14f), Color.Transparent),
                center = Offset(size.width * 0.05f, size.height * 0.55f),
                radius = size.minDimension * 0.8f
            ),
            radius = size.minDimension * 0.8f,
            center = Offset(size.width * 0.05f, size.height * 0.55f)
        )
    }
}
