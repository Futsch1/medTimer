package com.futsch1.medtimer.statistics.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TakenSkippedDataTest {

    @Test
    fun `isEmpty returns true when both taken and skipped are zero`() {
        assertTrue(TakenSkippedData(0, 0, "title").isEmpty)
    }

    @Test
    fun `isEmpty returns false when taken is positive`() {
        assertFalse(TakenSkippedData(1, 0, "title").isEmpty)
    }

    @Test
    fun `isEmpty returns false when skipped is positive`() {
        assertFalse(TakenSkippedData(0, 1, "title").isEmpty)
    }

    @Test
    fun `percentages for 7 taken 3 skipped`() {
        val data = TakenSkippedData(7, 3, "title")
        assertEquals(70, data.takenPercent)
        assertEquals(30, data.skippedPercent)
    }

    @Test
    fun `percentages for 1 taken 2 skipped`() {
        val data = TakenSkippedData(1, 2, "title")
        assertEquals(33, data.takenPercent)
        assertEquals(67, data.skippedPercent)
    }

    @Test
    fun `percentages for 100 percent taken`() {
        val data = TakenSkippedData(10, 0, "title")
        assertEquals(100, data.takenPercent)
        assertEquals(0, data.skippedPercent)
    }

    @Test
    fun `percentages for 100 percent skipped`() {
        val data = TakenSkippedData(0, 5, "title")
        assertEquals(0, data.takenPercent)
        assertEquals(100, data.skippedPercent)
    }

    @Test
    fun `percentages for zero zero`() {
        val data = TakenSkippedData(0, 0, "title")
        assertEquals(0, data.takenPercent)
        assertEquals(0, data.skippedPercent)
    }

    @Test
    fun `takenPercent plus skippedPercent always equals 100 for non-empty data`() {
        val data = TakenSkippedData(1, 2, "title")
        assertEquals(100, data.takenPercent + data.skippedPercent)
    }
}
