package com.tpa.app.ui.chart

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChartFiltersTest {

    @Test
    fun dailyOffersEveryRange() {
        assertEquals(ChartRanges, allowedRanges("1d"))
    }

    @Test
    fun weeklyAndMonthlyOnlyOfferLongRanges() {
        val expected = listOf("1Y", "2Y", "5Y", "10Y")
        assertEquals(expected, allowedRanges("1w"))
        assertEquals(expected, allowedRanges("1M"))
    }

    @Test
    fun shortRangesOnlyAllowDaily() {
        listOf("5D", "1M", "3M", "6M").forEach { range ->
            assertEquals(listOf("1d"), allowedIntervals(range))
        }
    }

    @Test
    fun longRangesAllowEveryEodInterval() {
        listOf("1Y", "2Y", "5Y", "10Y").forEach { range ->
            assertEquals(listOf("1d", "1w", "1M"), allowedIntervals(range))
        }
    }

    @Test
    fun clampingAlwaysProducesASupportedPair() {
        assertEquals("1Y", clampRangeFor("1w", "5D"))
        assertEquals("1d", clampIntervalFor("5D", "1w"))

        ChartRanges.forEach { range ->
            ChartIntervals.forEach { interval ->
                val clampedRange = clampRangeFor(interval, range)
                val clampedInterval = clampIntervalFor(clampedRange, interval)
                assertTrue(
                    "$clampedInterval/$clampedRange must be supported",
                    clampedInterval in allowedIntervals(clampedRange)
                )
                assertTrue(
                    "$clampedRange must be offered for $clampedInterval",
                    clampedRange in allowedRanges(clampedInterval)
                )
            }
        }
    }
}
