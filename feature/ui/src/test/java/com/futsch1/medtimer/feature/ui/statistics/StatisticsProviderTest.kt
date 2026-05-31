package com.futsch1.medtimer.feature.ui.statistics

import com.futsch1.medtimer.core.domain.model.ReminderEvent
import org.junit.Test
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StatisticsProviderTest {

    private val provider = StatisticsProvider()

    // ── getTakenSkippedData ───────────────────────────────────────────────────

    @Test
    fun `getTakenSkippedData with days=0 counts all taken and skipped regardless of age`() {
        val events = listOf(
            event(status = ReminderEvent.ReminderStatus.TAKEN, daysAgo = 100),
            event(status = ReminderEvent.ReminderStatus.TAKEN, daysAgo = 1),
            event(status = ReminderEvent.ReminderStatus.SKIPPED, daysAgo = 50),
        )

        val result = provider.getTakenSkippedData(events, 0)

        assertEquals(2L, result.taken)
        assertEquals(1L, result.skipped)
    }

    @Test
    fun `getTakenSkippedData with days filter excludes events outside the window`() {
        val events = listOf(
            event(status = ReminderEvent.ReminderStatus.TAKEN, daysAgo = 2),
            event(status = ReminderEvent.ReminderStatus.TAKEN, daysAgo = 10),
        )

        val result = provider.getTakenSkippedData(events, 7)

        assertEquals(1L, result.taken)
    }

    @Test
    fun `getTakenSkippedData returns zeros when there are no events`() {
        val result = provider.getTakenSkippedData(emptyList(), 7)

        assertEquals(0L, result.taken)
        assertEquals(0L, result.skipped)
    }

    @Test
    fun `getTakenSkippedData ignores raised events`() {
        val events = listOf(
            event(status = ReminderEvent.ReminderStatus.RAISED, daysAgo = 1),
            event(status = ReminderEvent.ReminderStatus.TAKEN, daysAgo = 1),
        )

        val result = provider.getTakenSkippedData(events, 0)

        assertEquals(1L, result.taken)
        assertEquals(0L, result.skipped)
    }

    // ── getLastDaysReminders ──────────────────────────────────────────────────

    @Test
    fun `getLastDaysReminders returns empty data when there are no events`() {
        val result = provider.getLastDaysReminders(emptyList(), 7)

        assertTrue(result.series.isEmpty())
        assertTrue(result.epochDays.isEmpty())
    }

    @Test
    fun `getLastDaysReminders returns one epoch day per requested day`() {
        val events = listOf(event(status = ReminderEvent.ReminderStatus.TAKEN, daysAgo = 1))

        val result = provider.getLastDaysReminders(events, 7)

        assertEquals(7, result.epochDays.size)
    }

    @Test
    fun `getLastDaysReminders excludes events outside the window`() {
        val events = listOf(
            event(status = ReminderEvent.ReminderStatus.TAKEN, daysAgo = 2, medicineName = "Vitamin X"),
            event(status = ReminderEvent.ReminderStatus.TAKEN, daysAgo = 10, medicineName = "Vitamin X"),
        )

        val result = provider.getLastDaysReminders(events, 7)

        assertEquals(1, result.series.size)
        assertEquals(1, result.series[0].counts.sum())
    }

    @Test
    fun `getLastDaysReminders groups events by medicine name`() {
        val events = listOf(
            event(status = ReminderEvent.ReminderStatus.TAKEN, daysAgo = 1, medicineName = "Vitamin X"),
            event(status = ReminderEvent.ReminderStatus.TAKEN, daysAgo = 1, medicineName = "Medicine A"),
            event(status = ReminderEvent.ReminderStatus.TAKEN, daysAgo = 2, medicineName = "Vitamin X"),
        )

        val result = provider.getLastDaysReminders(events, 7)

        assertEquals(2, result.series.size)
        val vitaminSeries = result.series.first { it.medicineName == "Vitamin X" }
        assertEquals(2, vitaminSeries.counts.sum())
        val medicineSeries = result.series.first { it.medicineName == "Medicine A" }
        assertEquals(1, medicineSeries.counts.sum())
    }

    @Test
    fun `getLastDaysReminders excludes skipped events`() {
        val events = listOf(
            event(status = ReminderEvent.ReminderStatus.TAKEN, daysAgo = 1, medicineName = "Vitamin X"),
            event(status = ReminderEvent.ReminderStatus.SKIPPED, daysAgo = 1, medicineName = "Medicine A"),
        )

        val result = provider.getLastDaysReminders(events, 7)

        // Only TAKEN events appear in the bar chart
        assertEquals(1, result.series.size)
        assertEquals("Vitamin X", result.series[0].medicineName)
    }

    // ── aggregate ─────────────────────────────────────────────────────────────

    @Test
    fun `aggregate folds one events list into per-day, period and total tallies`() {
        val events = listOf(
            event(status = ReminderEvent.ReminderStatus.TAKEN, daysAgo = 1, medicineName = "Vitamin X"),
            event(status = ReminderEvent.ReminderStatus.TAKEN, daysAgo = 10, medicineName = "Vitamin X"),
            event(status = ReminderEvent.ReminderStatus.SKIPPED, daysAgo = 2, medicineName = "Vitamin X"),
        )

        val data = provider.aggregate(events, 7)

        // Per-day bar chart only counts TAKEN within the window
        assertEquals(1, data.perDay.series.size)
        assertEquals(1, data.perDay.series[0].counts.sum())
        // Windowed pie: 1 taken (daysAgo=1), 1 skipped (daysAgo=2); the 10-day-old taken is excluded
        assertEquals(1L, data.period.taken)
        assertEquals(1L, data.period.skipped)
        // All-time pie: both taken events count
        assertEquals(2L, data.total.taken)
        assertEquals(1L, data.total.skipped)
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private fun event(
        status: ReminderEvent.ReminderStatus = ReminderEvent.ReminderStatus.TAKEN,
        daysAgo: Long = 1,
        medicineName: String = "Vitamin X",
    ) = ReminderEvent.default().copy(
        status = status,
        medicineName = medicineName,
        // Subtract an extra hour so we're safely within the target date, not near midnight
        remindedTimestamp = Instant.now().minus(daysAgo, ChronoUnit.DAYS).minus(1, ChronoUnit.HOURS),
    )
}
