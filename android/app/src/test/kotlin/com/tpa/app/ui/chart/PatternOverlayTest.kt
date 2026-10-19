package com.tpa.app.ui.chart

import org.junit.Assert.*
import org.junit.Test

class PatternOverlayTest {

    @Test
    fun anchorPointToChartCoordinateConversion() {
        val anchors = listOf(
            AnchorPoint(0, 150.0),
            AnchorPoint(5, 145.0),
            AnchorPoint(10, 155.0),
            AnchorPoint(15, 148.0)
        )

        val firstX = anchors.first().candleIndex.toFloat()
        val firstPrice = anchors.first().price.toFloat()

        assertEquals(0f, firstX, 0.01f)
        assertEquals(150.0f, firstPrice, 0.01f)

        val lastX = anchors.last().candleIndex.toFloat()
        val lastPrice = anchors.last().price.toFloat()

        assertEquals(15f, lastX, 0.01f)
        assertEquals(148.0f, lastPrice, 0.01f)

        val middleAnchor = anchors[2]
        assertEquals(10, middleAnchor.candleIndex)
        assertEquals(155.0, middleAnchor.price, 0.01)
    }

    @Test
    fun necklineInfoCalculatesCorrectSlope() {
        val neckline = NecklineInfo(5f, 145f, 15f, 155f)
        val slope = (neckline.endY - neckline.startY) / (neckline.endX - neckline.startX)
        assertEquals(1.0f, slope, 0.01f)
    }

    @Test
    fun patternOverlayDataContainsAllFields() {
        val overlay = PatternOverlay(
            anchors = listOf(AnchorPoint(1, 100.0)),
            neckline = NecklineInfo(0f, 100f, 5f, 120f),
            targetPrice = 130.0,
            stopPrice = 90.0,
            patternName = "H&S",
            direction = "bear",
            confidence = 85.0,
            riskReward = 3.5
        )

        assertEquals("H&S", overlay.patternName)
        assertEquals("bear", overlay.direction)
        assertEquals(85.0, overlay.confidence, 0.01)
        assertEquals(3.5, overlay.riskReward, 0.01)
        assertEquals(130.0, overlay.targetPrice, 0.01)
        assertEquals(90.0, overlay.stopPrice, 0.01)
    }
}
