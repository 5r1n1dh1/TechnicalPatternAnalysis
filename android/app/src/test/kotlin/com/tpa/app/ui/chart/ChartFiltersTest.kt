package com.tpa.app.ui.chart

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChartFiltersTest {

    @Test
    fun intradayIntervalsOnlyOfferShortRanges() {
        listOf("5m", "15m", "30m").forEach { interval ->
            assertEquals(listOf("1D", "5D", "1M"), allowedRanges(interval))
        }
        assertEquals(
            listOf("1D", "5D", "1M", "3M", "6M", "1Y"),
            allowedRanges("1h")
        )
    }

    @Test
    fun dailyStartsAtFiveDays() {
        assertEquals(
            listOf("5D", "1M", "3M", "6M", "1Y", "2Y", "5Y", "10Y"),
            allowedRanges("1d")
        )
    }

    @Test
    fun weeklyAndMonthlyOnlyOfferLongRanges() {
        val expected = listOf("1Y", "2Y", "5Y", "10Y")
        assertEquals(expected, allowedRanges("1w"))
        assertEquals(expected, allowedRanges("1M"))
    }

    @Test
    fun oneDayRangeOnlyAllowsIntraday() {
        assertEquals(listOf("5m", "15m", "30m", "1h"), allowedIntervals("1D"))
    }

    @Test
    fun shortRangesAllowDailyOnlyFromFiveDays() {
        listOf("5D", "1M").forEach { range ->
            assertEquals(listOf("5m", "15m", "30m", "1h", "1d"), allowedIntervals(range))
        }
        listOf("3M", "6M").forEach { range ->
            assertEquals(listOf("1h", "1d"), allowedIntervals(range))
        }
    }

    @Test
    fun longRangesNeverOfferMinutes() {
        listOf("1Y").forEach { range ->
            assertEquals(listOf("1h", "1d", "1w", "1M"), allowedIntervals(range))
        }
        listOf("2Y", "5Y", "10Y").forEach { range ->
            assertEquals(listOf("1d", "1w", "1M"), allowedIntervals(range))
        }
    }

    @Test
    fun clampingFallsBackToTheFirstSupportedValue() {
        // Selecting 1D while on daily must switch to 5m, not keep an invalid 1d pair.
        assertEquals("5m", clampIntervalFor("1D", "1d"))
        assertEquals("5D", clampRangeFor("1d", "1D"))
        assertEquals("1D", clampRangeFor("5m", "1Y"))
        assertEquals("1h", clampIntervalFor("1Y", "5m"))
    }

    @Test
    fun clampingAlwaysProducesASupportedPair() {
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
