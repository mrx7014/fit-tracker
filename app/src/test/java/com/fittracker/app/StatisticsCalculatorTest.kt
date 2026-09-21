package com.fittracker.app

import org.junit.Assert.assertEquals
import org.junit.Test

class StatisticsCalculatorTest {
    private val lifts = listOf(
        Lift(1, "Squat", "2026-09-20", 100f, 5),
        Lift(2, "Bench press", "2026-09-20", 60f, 8),
        Lift(3, "Squat", "2026-09-21", 105f, 3),
    )

    @Test
    fun totals_are_calculated_from_all_lifts() {
        assertEquals(265f, StatisticsCalculator.totalWeight(lifts), 0.001f)
        assertEquals(16, StatisticsCalculator.totalReps(lifts))
        assertEquals(1295f, StatisticsCalculator.totalVolume(lifts), 0.001f)
        assertEquals(2, StatisticsCalculator.activeDays(lifts))
    }

    @Test
    fun empty_history_returns_zero_totals() {
        assertEquals(0f, StatisticsCalculator.totalWeight(emptyList()), 0.001f)
        assertEquals(0, StatisticsCalculator.totalReps(emptyList()))
        assertEquals(0f, StatisticsCalculator.totalVolume(emptyList()), 0.001f)
        assertEquals(0, StatisticsCalculator.activeDays(emptyList()))
    }
}
