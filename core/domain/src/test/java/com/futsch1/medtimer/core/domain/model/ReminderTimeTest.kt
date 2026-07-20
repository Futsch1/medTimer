package com.futsch1.medtimer.core.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

class ReminderTimeTest {

    @Test
    fun `minutes constructor stores value directly`() {
        assertEquals(480, ReminderTime(480).minutes)
        assertEquals(0, ReminderTime(0).minutes)
        assertEquals(1439, ReminderTime(1439).minutes)
    }

    @Test
    fun `LocalTime constructor converts correctly`() {
        assertEquals(0, ReminderTime(LocalTime.of(0, 0)).minutes)
        assertEquals(480, ReminderTime(LocalTime.of(8, 0)).minutes)
        assertEquals(540, ReminderTime(LocalTime.of(9, 0)).minutes)
        assertEquals(1439, ReminderTime(LocalTime.of(23, 59)).minutes)
    }

    @Test
    fun `getLocalTime converts minutes back to LocalTime`() {
        assertEquals(LocalTime.of(0, 0), ReminderTime(0).getLocalTime())
        assertEquals(LocalTime.of(8, 0), ReminderTime(480).getLocalTime())
        assertEquals(LocalTime.of(23, 59), ReminderTime(1439).getLocalTime())
    }

    @Test
    fun `getLocalTime wraps around correctly for values over 24 hours`() {
        assertEquals(LocalTime.of(1, 0), ReminderTime(1500).getLocalTime())
        assertEquals(LocalTime.of(0, 0), ReminderTime(1440).getLocalTime())
    }

    @Test
    fun `getLocalTime handles single digit minutes`() {
        assertEquals(LocalTime.of(0, 1), ReminderTime(1).getLocalTime())
        assertEquals(LocalTime.of(0, 59), ReminderTime(59).getLocalTime())
        assertEquals(LocalTime.of(1, 1), ReminderTime(61).getLocalTime())
    }

    @Test
    fun `compareTo returns zero for equal values`() {
        assertEquals(0, ReminderTime(480).compareTo(ReminderTime(480)))
    }

    @Test
    fun `compareTo returns positive when first is later`() {
        assertTrue(ReminderTime(500).compareTo(ReminderTime(480)) > 0)
    }

    @Test
    fun `compareTo returns negative when first is earlier`() {
        assertTrue(ReminderTime(480).compareTo(ReminderTime(500)) < 0)
    }

    @Test
    fun `compareTo at boundaries`() {
        assertTrue(ReminderTime(0).compareTo(ReminderTime(1439)) < 0)
        assertTrue(ReminderTime(1439).compareTo(ReminderTime(0)) > 0)
    }

    @Test
    fun `seconds is derived from minutes`() {
        assertEquals(0L, ReminderTime(0).seconds)
        assertEquals(28800L, ReminderTime(480).seconds)
        assertEquals(86340L, ReminderTime(1439).seconds)
    }

    @Test
    fun `isDuration defaults to false`() {
        assertEquals(false, ReminderTime(480).isDuration)
        assertEquals(false, ReminderTime(480, isDuration = false).isDuration)
    }

    @Test
    fun `isDuration can be set to true`() {
        assertEquals(true, ReminderTime(480, isDuration = true).isDuration)
    }

    @Test
    fun `DEFAULT_TIME constant is 480`() {
        assertEquals(480, ReminderTime.DEFAULT_TIME)
    }
}
