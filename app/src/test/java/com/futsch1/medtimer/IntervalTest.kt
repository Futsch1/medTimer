package com.futsch1.medtimer

import com.futsch1.medtimer.helpers.Interval
import com.futsch1.medtimer.helpers.IntervalUnit
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class IntervalTest {

    @Test
    fun testMinutesConstructor() {
        var interval = Interval(30)
        assertEquals(30, interval.minutesValue)
        assertEquals(IntervalUnit.MINUTES, interval.getUnit())
        assertEquals(30, interval.getValue())

        interval = Interval(130)
        assertEquals(130, interval.minutesValue)
        assertEquals(IntervalUnit.MINUTES, interval.getUnit())
        assertEquals(130, interval.getValue())
    }

    @Test
    fun testHoursConstructor() {
        var interval = Interval(2, IntervalUnit.HOURS)
        assertEquals(120, interval.minutesValue)
        assertEquals(IntervalUnit.HOURS, interval.getUnit())
        assertEquals(2, interval.getValue())

        interval = Interval(30, IntervalUnit.HOURS)
        assertEquals(30 * 60, interval.minutesValue)
        assertEquals(IntervalUnit.HOURS, interval.getUnit())
        assertEquals(30, interval.getValue())
    }

    @Test
    fun testDaysConstructor() {
        val interval = Interval(1, IntervalUnit.DAYS)
        assertEquals(1440, interval.minutesValue)
        assertEquals(IntervalUnit.DAYS, interval.getUnit())
        assertEquals(1, interval.getValue())
    }

    @Test
    fun testToString() {
        var interval = Interval(60)
        assertEquals("1 hours", interval.toString())

        interval = Interval(61)
        assertEquals("61 minutes", interval.toString())

        interval = Interval(1440)
        assertEquals("1 days", interval.toString())
    }
}