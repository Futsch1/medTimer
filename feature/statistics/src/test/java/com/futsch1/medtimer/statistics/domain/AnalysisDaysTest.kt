package com.futsch1.medtimer.statistics.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class AnalysisDaysTest {

    @Test
    fun `fromDays returns ONE_DAY for 1`() {
        assertEquals(AnalysisDays.ONE_DAY, AnalysisDays.fromDays(1))
    }

    @Test
    fun `fromDays returns TWO_DAYS for 2`() {
        assertEquals(AnalysisDays.TWO_DAYS, AnalysisDays.fromDays(2))
    }

    @Test
    fun `fromDays returns THREE_DAYS for 3`() {
        assertEquals(AnalysisDays.THREE_DAYS, AnalysisDays.fromDays(3))
    }

    @Test
    fun `fromDays returns SEVEN_DAYS for 7`() {
        assertEquals(AnalysisDays.SEVEN_DAYS, AnalysisDays.fromDays(7))
    }

    @Test
    fun `fromDays returns FOURTEEN_DAYS for 14`() {
        assertEquals(AnalysisDays.FOURTEEN_DAYS, AnalysisDays.fromDays(14))
    }

    @Test
    fun `fromDays returns THIRTY_DAYS for 30`() {
        assertEquals(AnalysisDays.THIRTY_DAYS, AnalysisDays.fromDays(30))
    }

    @Test
    fun `fromDays returns DEFAULT for unknown value`() {
        assertEquals(AnalysisDays.DEFAULT, AnalysisDays.fromDays(5))
    }

    @Test
    fun `fromDays returns DEFAULT for large unknown value`() {
        assertEquals(AnalysisDays.DEFAULT, AnalysisDays.fromDays(99))
    }

    @Test
    fun `DEFAULT is SEVEN_DAYS`() {
        assertEquals(AnalysisDays.SEVEN_DAYS, AnalysisDays.DEFAULT)
    }
}
