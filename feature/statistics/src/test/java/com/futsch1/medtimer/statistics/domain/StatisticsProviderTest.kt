package com.futsch1.medtimer.statistics.domain

import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.database.ReminderEvent
import com.futsch1.medtimer.database.ReminderEvent.ReminderStatus
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.time.LocalDate
import java.time.ZoneId

class StatisticsProviderTest {

    private lateinit var repository: MedicineRepository
    private lateinit var provider: StatisticsProvider

    private fun createEvent(
        name: String,
        status: ReminderStatus,
        remindedTimestamp: Long,
        processedTimestamp: Long = remindedTimestamp
    ): ReminderEvent {
        return ReminderEvent().apply {
            medicineName = name
            this.status = status
            this.remindedTimestamp = remindedTimestamp
            this.processedTimestamp = processedTimestamp
        }
    }

    private fun epochSecondsDaysAgo(daysAgo: Int): Long {
        return LocalDate.now()
            .minusDays(daysAgo.toLong())
            .atStartOfDay(ZoneId.systemDefault())
            .toEpochSecond() + 3600 // Add 1 hour to be safely within the day
    }

    @Before
    fun setUp() {
        repository = mock(MedicineRepository::class.java)
        provider = StatisticsProvider(repository)
    }

    // --- getTakenSkippedData tests ---

    @Test
    fun `getTakenSkippedData counts taken and skipped correctly`() {
        val events = listOf(
            createEvent("Med A", ReminderStatus.TAKEN, epochSecondsDaysAgo(1)),
            createEvent("Med A", ReminderStatus.TAKEN, epochSecondsDaysAgo(2)),
            createEvent("Med A", ReminderStatus.TAKEN, epochSecondsDaysAgo(3)),
            createEvent("Med B", ReminderStatus.SKIPPED, epochSecondsDaysAgo(1)),
            createEvent("Med B", ReminderStatus.SKIPPED, epochSecondsDaysAgo(2)),
            createEvent("Med B", ReminderStatus.RAISED, epochSecondsDaysAgo(1)),
        )
        `when`(repository.allReminderEventsWithoutDeleted).thenReturn(events)

        val result = provider.getTakenSkippedData(7)
        assertEquals(3, result.taken)
        assertEquals(2, result.skipped)
    }

    @Test
    fun `getTakenSkippedData with days 0 returns totals without cutoff`() {
        val events = listOf(
            createEvent("Med A", ReminderStatus.TAKEN, epochSecondsDaysAgo(100)),
            createEvent("Med A", ReminderStatus.TAKEN, epochSecondsDaysAgo(200)),
            createEvent("Med B", ReminderStatus.SKIPPED, epochSecondsDaysAgo(365)),
        )
        `when`(repository.allReminderEventsWithoutDeleted).thenReturn(events)

        val result = provider.getTakenSkippedData(0)
        assertEquals(2, result.taken)
        assertEquals(1, result.skipped)
    }

    @Test
    fun `getTakenSkippedData respects day cutoff`() {
        val events = listOf(
            createEvent("Med A", ReminderStatus.TAKEN, epochSecondsDaysAgo(1)),
            createEvent("Med A", ReminderStatus.TAKEN, epochSecondsDaysAgo(10)), // outside 7-day window
            createEvent("Med B", ReminderStatus.SKIPPED, epochSecondsDaysAgo(2)),
        )
        `when`(repository.allReminderEventsWithoutDeleted).thenReturn(events)

        val result = provider.getTakenSkippedData(7)
        assertEquals(1, result.taken)
        assertEquals(1, result.skipped)
    }

    @Test
    fun `getTakenSkippedData with empty events`() {
        `when`(repository.allReminderEventsWithoutDeleted).thenReturn(emptyList())

        val result = provider.getTakenSkippedData(7)
        assertEquals(0, result.taken)
        assertEquals(0, result.skipped)
    }

    // --- getLastDaysReminders tests ---

    @Test
    fun `getLastDaysReminders groups events by normalized medicine name`() {
        val events = listOf(
            createEvent("Med A (1/3)", ReminderStatus.TAKEN, epochSecondsDaysAgo(1)),
            createEvent("Med A (2/3)", ReminderStatus.TAKEN, epochSecondsDaysAgo(1)),
            createEvent("Med B", ReminderStatus.TAKEN, epochSecondsDaysAgo(2)),
        )
        `when`(repository.allReminderEventsWithoutDeleted).thenReturn(events)

        val result = provider.getLastDaysReminders(7)
        assertEquals(2, result.size)

        val medAEntry = result.find { it.name == "Med A" }
        val medBEntry = result.find { it.name == "Med B" }
        assertEquals("Med A", medAEntry?.name)
        assertEquals("Med B", medBEntry?.name)
    }

    @Test
    fun `getLastDaysReminders maps correct y values for days`() {
        // Two events on "1 day ago"
        val events = listOf(
            createEvent("Med A", ReminderStatus.TAKEN, epochSecondsDaysAgo(1)),
            createEvent("Med A", ReminderStatus.TAKEN, epochSecondsDaysAgo(1)),
        )
        `when`(repository.allReminderEventsWithoutDeleted).thenReturn(events)

        val result = provider.getLastDaysReminders(3)
        assertEquals(1, result.size)

        val series = result[0]
        // yValues: [days-1 downTo 0] = [2, 1, 0] days ago
        // Events are at daysAgo=1, so index 1 should have count 2
        assertEquals(3, series.yValues.size)
        assertEquals(0, series.yValues[0]) // 2 days ago
        assertEquals(2, series.yValues[1]) // 1 day ago
        assertEquals(0, series.yValues[2]) // 0 days ago (today)
    }

    @Test
    fun `getLastDaysReminders only includes TAKEN events`() {
        val events = listOf(
            createEvent("Med A", ReminderStatus.TAKEN, epochSecondsDaysAgo(1)),
            createEvent("Med A", ReminderStatus.SKIPPED, epochSecondsDaysAgo(1)),
            createEvent("Med A", ReminderStatus.RAISED, epochSecondsDaysAgo(1)),
        )
        `when`(repository.allReminderEventsWithoutDeleted).thenReturn(events)

        val result = provider.getLastDaysReminders(3)
        assertEquals(1, result.size)
        // Only 1 TAKEN event on day 1
        assertEquals(1, result[0].yValues[1])
    }

    @Test
    fun `getLastDaysReminders with empty events returns empty list`() {
        `when`(repository.allReminderEventsWithoutDeleted).thenReturn(emptyList())

        val result = provider.getLastDaysReminders(7)
        assertEquals(0, result.size)
    }

    @Test
    fun `getLastDaysReminders produces correct xValues as epoch days`() {
        val events = listOf(
            createEvent("Med A", ReminderStatus.TAKEN, epochSecondsDaysAgo(0)),
        )
        `when`(repository.allReminderEventsWithoutDeleted).thenReturn(events)

        val result = provider.getLastDaysReminders(3)
        val todayEpochDay = LocalDate.now().toEpochDay()
        assertEquals(3, result[0].xValues.size)
        assertEquals(todayEpochDay - 2, result[0].xValues[0])
        assertEquals(todayEpochDay - 1, result[0].xValues[1])
        assertEquals(todayEpochDay, result[0].xValues[2])
    }
}
