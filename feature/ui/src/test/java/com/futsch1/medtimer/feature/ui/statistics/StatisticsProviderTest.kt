package com.futsch1.medtimer.feature.ui.statistics

import com.futsch1.medtimer.core.domain.model.ReminderEvent
import com.futsch1.medtimer.core.domain.repository.ReminderEventRepository
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StatisticsProviderTest {

    private val repository: ReminderEventRepository = mock()
    private val provider = StatisticsProvider(repository)

    // ── getTakenSkippedData ───────────────────────────────────────────────────

    @Test
    fun `getTakenSkippedData with days=0 counts all taken and skipped regardless of age`() = runTest {
        whenever(repository.getAllWithoutDeleted()).thenReturn(
            listOf(
                event(status = ReminderEvent.ReminderStatus.TAKEN, daysAgo = 100),
                event(status = ReminderEvent.ReminderStatus.TAKEN, daysAgo = 1),
                event(status = ReminderEvent.ReminderStatus.SKIPPED, daysAgo = 50),
            )
        )

        val result = provider.getTakenSkippedData(0)

        assertEquals(2L, result.taken)
        assertEquals(1L, result.skipped)
    }

    @Test
    fun `getTakenSkippedData with days filter excludes events outside the window`() = runTest {
        whenever(repository.getAllWithoutDeleted()).thenReturn(
            listOf(
                event(status = ReminderEvent.ReminderStatus.TAKEN, daysAgo = 2),
                event(status = ReminderEvent.ReminderStatus.TAKEN, daysAgo = 10),
            )
        )

        val result = provider.getTakenSkippedData(7)

        assertEquals(1L, result.taken)
    }

    @Test
    fun `getTakenSkippedData returns zeros when repository is empty`() = runTest {
        whenever(repository.getAllWithoutDeleted()).thenReturn(emptyList())

        val result = provider.getTakenSkippedData(7)

        assertEquals(0L, result.taken)
        assertEquals(0L, result.skipped)
    }

    @Test
    fun `getTakenSkippedData ignores raised and deleted events`() = runTest {
        whenever(repository.getAllWithoutDeleted()).thenReturn(
            listOf(
                event(status = ReminderEvent.ReminderStatus.RAISED, daysAgo = 1),
                event(status = ReminderEvent.ReminderStatus.TAKEN, daysAgo = 1),
            )
        )

        val result = provider.getTakenSkippedData(0)

        assertEquals(1L, result.taken)
        assertEquals(0L, result.skipped)
    }

    // ── getLastDaysReminders ──────────────────────────────────────────────────

    @Test
    fun `getLastDaysReminders returns empty data when repository is empty`() = runTest {
        whenever(repository.getAllWithoutDeleted()).thenReturn(emptyList())

        val result = provider.getLastDaysReminders(7)

        assertTrue(result.series.isEmpty())
        assertTrue(result.epochDays.isEmpty())
    }

    @Test
    fun `getLastDaysReminders returns one epoch day per requested day`() = runTest {
        whenever(repository.getAllWithoutDeleted()).thenReturn(
            listOf(event(status = ReminderEvent.ReminderStatus.TAKEN, daysAgo = 1))
        )

        val result = provider.getLastDaysReminders(7)

        assertEquals(7, result.epochDays.size)
    }

    @Test
    fun `getLastDaysReminders excludes events outside the window`() = runTest {
        whenever(repository.getAllWithoutDeleted()).thenReturn(
            listOf(
                event(status = ReminderEvent.ReminderStatus.TAKEN, daysAgo = 2, medicineName = "Vitamin X"),
                event(status = ReminderEvent.ReminderStatus.TAKEN, daysAgo = 10, medicineName = "Vitamin X"),
            )
        )

        val result = provider.getLastDaysReminders(7)

        assertEquals(1, result.series.size)
        assertEquals(1, result.series[0].counts.sum())
    }

    @Test
    fun `getLastDaysReminders groups events by medicine name`() = runTest {
        whenever(repository.getAllWithoutDeleted()).thenReturn(
            listOf(
                event(status = ReminderEvent.ReminderStatus.TAKEN, daysAgo = 1, medicineName = "Vitamin X"),
                event(status = ReminderEvent.ReminderStatus.TAKEN, daysAgo = 1, medicineName = "Medicine A"),
                event(status = ReminderEvent.ReminderStatus.TAKEN, daysAgo = 2, medicineName = "Vitamin X"),
            )
        )

        val result = provider.getLastDaysReminders(7)

        assertEquals(2, result.series.size)
        val vitaminSeries = result.series.first { it.medicineName == "Vitamin X" }
        assertEquals(2, vitaminSeries.counts.sum())
        val medicineSeries = result.series.first { it.medicineName == "Medicine A" }
        assertEquals(1, medicineSeries.counts.sum())
    }

    @Test
    fun `getLastDaysReminders excludes skipped events`() = runTest {
        whenever(repository.getAllWithoutDeleted()).thenReturn(
            listOf(
                event(status = ReminderEvent.ReminderStatus.TAKEN, daysAgo = 1, medicineName = "Vitamin X"),
                event(status = ReminderEvent.ReminderStatus.SKIPPED, daysAgo = 1, medicineName = "Medicine A"),
            )
        )

        val result = provider.getLastDaysReminders(7)

        // Only TAKEN events appear in the bar chart
        assertEquals(1, result.series.size)
        assertEquals("Vitamin X", result.series[0].medicineName)
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
