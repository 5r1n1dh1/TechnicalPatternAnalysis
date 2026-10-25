package com.tpa.app.ui.chart

import androidx.compose.ui.graphics.Color
import com.tpa.app.ui.theme.GlassBorderLight

data class PivotPoint(val idx: Int, val price: Double, val kind: String) // HIGH / LOW

object PivotMarker {
    val highColor = Color(0xFFFBBF24) // amber token
    val lowColor = Color(0xFFF87171)  // red token
    val glass = Color(0xCCFFFFFF)     // glass fill
    val border = GlassBorderLight
}
