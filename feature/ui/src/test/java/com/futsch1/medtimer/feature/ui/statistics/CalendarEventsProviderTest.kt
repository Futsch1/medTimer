package com.futsch1.medtimer.feature.ui.statistics

import com.futsch1.medtimer.core.datastore.PreferencesDataSource
import com.futsch1.medtimer.core.domain.model.ReminderEvent
import com.futsch1.medtimer.core.domain.repository.MedicineRepository
import com.futsch1.medtimer.core.domain.repository.ReminderEventRepository
import com.futsch1.medtimer.feature.ui.statistics.calendar.CalendarDayEvent
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.LocalDate
import java.time.ZoneId
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// Covers the shared past-event traversal through its structured renderer. Future (scheduled) events
// need the SchedulingSimulator and Medicine fixtures, so they're exercised with futureMonths = 0 here.
class CalendarEventsProviderTest {

    private val medicineRepository: MedicineRepository = mock()
    private val reminderEventRepository: ReminderEventRepository = mock()
    private val preferencesDataSource: PreferencesDataSource = mock()
    private val provider = CalendarEventsProvider(medicineRepository, reminderEventRepository, preferencesDataSource)

    @Test
    fun `getStructuredEvents buckets past events by day and skips deleted ones`() = runTest {
        whenever(medicineRepository.getAll()).thenReturn(emptyList())
        whenever(reminderEventRepository.getLastDays(any())).thenReturn(
            listOf(
                event(status = ReminderEvent.ReminderStatus.TAKEN, daysAgo = 1, medicineName = "Vitamin X"),
                event(status = ReminderEvent.ReminderStatus.DELETED, daysAgo = 1, medicineName = "Vitamin X"),
                event(status = ReminderEvent.ReminderStatus.SKIPPED, daysAgo = 2, medicineName = "Medicine A"),
            )
        )

        val result = provider.getStructuredEvents(ALL_MEDICINES, pastMonths = 3, futureMonths = 0)

        val today = LocalDate.now()
        val dayOne = today.minusDays(1)
        val dayTwo = today.minusDays(2)
        assertEquals(setOf(dayOne, dayTwo), result.keys)
        // The DELETED event on dayOne is dropped, leaving only the TAKEN one
        assertEquals(1, result.getValue(dayOne).size)
        assertEquals(CalendarDayEvent.Status.TAKEN, result.getValue(dayOne)[0].status)
        assertEquals("Vitamin X", result.getValue(dayOne)[0].medicineName)
        assertEquals(CalendarDayEvent.Status.SKIPPED, result.getValue(dayTwo)[0].status)
    }

    @Test
    fun `getStructuredEvents returns an empty map when there are no events`() = runTest {
        whenever(medicineRepository.getAll()).thenReturn(emptyList())
        whenever(reminderEventRepository.getLastDays(any())).thenReturn(emptyList())

        assertTrue(provider.getStructuredEvents(ALL_MEDICINES, pastMonths = 3, futureMonths = 0).isEmpty())
    }

    private fun event(
        status: ReminderEvent.ReminderStatus,
        daysAgo: Long,
        medicineName: String,
    ) = ReminderEvent.default().copy(
        status = status,
        medicineName = medicineName,
        // Anchor to noon of the target local day so bucketing is deterministic regardless of the time of
        // day the test runs — a near-midnight instant could otherwise bucket a day early or late.
        remindedTimestamp = LocalDate.now().minusDays(daysAgo).atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant(),
    )

    private companion object {
        const val ALL_MEDICINES = -1
    }
}
