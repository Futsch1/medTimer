package com.futsch1.medtimer.feature.ui.overview

import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNull

class OverviewWeekNavigationTest {

    // A Thursday-to-Wednesday week, as the strip anchors weeks to three days before today.
    private val weekOfFirst = (0L..6L).map { FIRST.plusDays(it) }

    @Test
    fun `keeps a selection that is in the visible week`() {
        assertNull(selectionForVisibleWeek(weekOfFirst, FIRST.plusDays(3), today = FIRST.plusWeeks(2)))
    }

    @Test
    fun `selects today when it is in the visible week`() {
        assertEquals(FIRST.plusDays(2), selectionForVisibleWeek(weekOfFirst, FIRST.plusWeeks(2), today = FIRST.plusDays(2)))
    }

    @Test
    fun `selects the last day when the selection lies after the visible week`() {
        assertEquals(weekOfFirst.last(), selectionForVisibleWeek(weekOfFirst, FIRST.plusWeeks(2), today = FIRST.plusWeeks(3)))
    }

    @Test
    fun `selects the first day when the selection lies before the visible week`() {
        assertEquals(FIRST, selectionForVisibleWeek(weekOfFirst, FIRST.minusWeeks(2), today = FIRST.plusWeeks(3)))
    }

    @Test
    fun `previous week targets its last day when today is not in it`() {
        assertEquals(FIRST.minusDays(1), adjacentWeekTarget(FIRST.plusDays(3), DayOfWeek.THURSDAY, -1, today = FIRST.plusWeeks(2)))
    }

    @Test
    fun `next week targets its first day when today is not in it`() {
        assertEquals(FIRST.plusWeeks(1), adjacentWeekTarget(FIRST.plusDays(3), DayOfWeek.THURSDAY, 1, today = FIRST.plusWeeks(3)))
    }

    @Test
    fun `adjacent week targets today when it is in that week`() {
        val today = FIRST.plusWeeks(1).plusDays(4)
        assertEquals(today, adjacentWeekTarget(FIRST.plusDays(3), DayOfWeek.THURSDAY, 1, today))
    }

    private companion object {
        val FIRST: LocalDate = LocalDate.of(2026, 10, 8)
    }
}
