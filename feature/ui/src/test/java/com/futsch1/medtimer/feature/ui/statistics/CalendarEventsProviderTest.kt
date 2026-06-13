package com.futsch1.medtimer.feature.ui.statistics

import com.futsch1.medtimer.core.datastore.PreferencesDataSource
import com.futsch1.medtimer.core.domain.model.Medicine
import com.futsch1.medtimer.core.domain.model.Reminder
import com.futsch1.medtimer.core.domain.model.ReminderEvent
import com.futsch1.medtimer.core.domain.model.ReminderType
import com.futsch1.medtimer.core.domain.model.ScheduledReminder
import com.futsch1.medtimer.core.domain.model.UserPreferences
import com.futsch1.medtimer.core.domain.repository.MedicineRepository
import com.futsch1.medtimer.core.domain.repository.ReminderEventRepository
import com.futsch1.medtimer.feature.reminders.FutureRemindersRepository
import com.futsch1.medtimer.feature.ui.statistics.calendar.CalendarDayEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

// Covers the shared past-event traversal through its structured renderer and future-entry bucketing
// via the injected FutureRemindersRepository stub.
class CalendarEventsProviderTest {

    private val medicineRepository: MedicineRepository = mock()
    private val reminderEventRepository: ReminderEventRepository = mock()
    private val preferencesDataSource: PreferencesDataSource = mock()
    private val futureRemindersRepository: FutureRemindersRepository = mock()

    private val simulatedRemindersFlow = MutableStateFlow<List<ScheduledReminder>>(emptyList())

    init {
        whenever(futureRemindersRepository.simulatedReminders).thenReturn(simulatedRemindersFlow)
        whenever(medicineRepository.getFlow(any())).thenReturn(flowOf(null))
        stubPreferences(showTakenTime = false)
    }

    private val provider = CalendarEventsProvider(
        medicineRepository,
        reminderEventRepository,
        preferencesDataSource,
        futureRemindersRepository,
    )

    @Test
    fun `getStructuredEvents buckets past events by day`() = runTest {
        whenever(medicineRepository.getAll()).thenReturn(emptyList())
        whenever(reminderEventRepository.getAllFlow(any(), any())).thenReturn(
            flowOf(
                listOf(
                    event(
                        status = ReminderEvent.ReminderStatus.TAKEN,
                        daysAgo = 1,
                        medicineName = "Vitamin X"
                    ),
                    event(
                        status = ReminderEvent.ReminderStatus.SKIPPED,
                        daysAgo = 2,
                        medicineName = "Medicine A"
                    ),
                )
            )
        )

        val result = provider.getStructuredEvents(ALL_MEDICINES, pastMonths = 3)

        val today = LocalDate.now()
        val dayOne = today.minusDays(1)
        val dayTwo = today.minusDays(2)
        assertEquals(setOf(dayOne, dayTwo), result.keys)
        assertEquals(1, result.getValue(dayOne).size)
        assertEquals(CalendarDayEvent.Status.TAKEN, result.getValue(dayOne)[0].status)
        assertEquals("Vitamin X", result.getValue(dayOne)[0].medicineName)
        assertEquals(CalendarDayEvent.Status.SKIPPED, result.getValue(dayTwo)[0].status)
    }

    @Test
    fun `getAllFlow is requested with deleted status excluded`() = runTest {
        whenever(medicineRepository.getAll()).thenReturn(emptyList())
        whenever(reminderEventRepository.getAllFlow(any(), any())).thenReturn(flowOf(emptyList()))

        provider.getStructuredEvents(ALL_MEDICINES, pastMonths = 3)

        verify(reminderEventRepository).getAllFlow(
            any(),
            eq(ReminderEvent.statusValuesWithoutDelete)
        )
    }

    @Test
    fun `getStructuredEvents carries the reminder type through to the structured event`() =
        runTest {
            whenever(medicineRepository.getAll()).thenReturn(emptyList())
            whenever(reminderEventRepository.getAllFlow(any(), any())).thenReturn(
                flowOf(
                    listOf(
                        event(
                            status = ReminderEvent.ReminderStatus.TAKEN,
                            daysAgo = 1,
                            medicineName = "Vitamin X"
                        )
                            .copy(reminderType = ReminderType.LINKED),
                    )
                )
            )

            val result =
                provider.getStructuredEvents(ALL_MEDICINES, pastMonths = 3)

            val dayOne = LocalDate.now().minusDays(1)
            assertEquals(ReminderType.LINKED, result.getValue(dayOne)[0].reminderType)
        }

    @Test
    fun `getStructuredEvents carries the taken time when the user opted to show taken times`() =
        runTest {
            stubPreferences(showTakenTime = true)
            whenever(medicineRepository.getAll()).thenReturn(emptyList())
            val processed =
                LocalDate.now().minusDays(1).atTime(13, 5).atZone(ZoneId.systemDefault())
                    .toInstant()
            whenever(reminderEventRepository.getAllFlow(any(), any())).thenReturn(
                flowOf(
                    listOf(
                        event(
                            ReminderEvent.ReminderStatus.TAKEN,
                            daysAgo = 1,
                            medicineName = "Vitamin X",
                            processedTimestamp = processed
                        ),
                    )
                )
            )

            val structured =
                provider.getStructuredEvents(ALL_MEDICINES, pastMonths = 3)
                    .getValue(LocalDate.now().minusDays(1))[0]

            assertEquals(
                LocalDateTime.ofInstant(processed, ZoneId.systemDefault()),
                structured.takenTime
            )
        }

    @Test
    fun `getStructuredEvents computes the interval and omits the taken time when taken times are hidden`() =
        runTest {
            stubPreferences(showTakenTime = false)
            whenever(medicineRepository.getAll()).thenReturn(emptyList())
            // Anchor the dose to a whole minute so the interval comes out to a clean 90 minutes.
            val processed = Instant.ofEpochSecond(1_700_000_400)
            val lastIntervalMinutes = (processed.epochSecond / 60).toInt() - 90
            whenever(reminderEventRepository.getAllFlow(any(), any())).thenReturn(
                flowOf(
                    listOf(
                        event(
                            ReminderEvent.ReminderStatus.TAKEN,
                            daysAgo = 1,
                            medicineName = "Vitamin X",
                            processedTimestamp = processed,
                            lastIntervalReminderTimeInMinutes = lastIntervalMinutes,
                        ),
                    )
                )
            )

            val structured =
                provider.getStructuredEvents(ALL_MEDICINES, pastMonths = 3)
                    .getValue(LocalDate.now().minusDays(1))[0]

            assertEquals(Duration.ofMinutes(90), structured.interval)
            assertNull(structured.takenTime)
        }

    @Test
    fun `getStructuredEvents returns an empty map when there are no events`() = runTest {
        whenever(medicineRepository.getAll()).thenReturn(emptyList())
        whenever(reminderEventRepository.getAllFlow(any(), any())).thenReturn(flowOf(emptyList()))

        assertTrue(
            provider.getStructuredEvents(ALL_MEDICINES, pastMonths = 3).isEmpty()
        )
    }

    @Test
    fun `getStructuredEvents buckets future simulated reminders by day`() = runTest {
        val medicine = Medicine.default().copy(id = 1, name = "Vitamin X")
        val reminder = Reminder.default().copy(medicineRelId = 1)
        whenever(medicineRepository.getAll()).thenReturn(listOf(medicine))
        whenever(medicineRepository.fetch(any())).thenReturn(null)
        whenever(reminderEventRepository.getAllFlow(any(), any())).thenReturn(flowOf(emptyList()))

        val tomorrow = LocalDate.now().plusDays(1)
        val tomorrowInstant = tomorrow.atTime(9, 0).atZone(ZoneId.systemDefault()).toInstant()
        simulatedRemindersFlow.value = listOf(
            ScheduledReminder(medicine, reminder, tomorrowInstant)
        )

        val result = provider.getStructuredEvents(ALL_MEDICINES, pastMonths = 0)

        assertEquals(1, result.size)
        assertEquals(tomorrow, result.keys.first())
        assertEquals(CalendarDayEvent.Status.SCHEDULED, result.getValue(tomorrow)[0].status)
        assertEquals("Vitamin X", result.getValue(tomorrow)[0].medicineName)
    }

    private fun event(
        status: ReminderEvent.ReminderStatus,
        daysAgo: Long,
        medicineName: String,
        processedTimestamp: Instant = Instant.EPOCH,
        lastIntervalReminderTimeInMinutes: Int = 0,
    ) = ReminderEvent.default().copy(
        status = status,
        medicineName = medicineName,
        // Anchor to noon of the target local day so bucketing is deterministic regardless of the time of
        // day the test runs — a near-midnight instant could otherwise bucket a day early or late.
        remindedTimestamp = LocalDate.now().minusDays(daysAgo).atTime(12, 0)
            .atZone(ZoneId.systemDefault()).toInstant(),
        processedTimestamp = processedTimestamp,
        lastIntervalReminderTimeInMinutes = lastIntervalReminderTimeInMinutes,
    )

    private fun stubPreferences(showTakenTime: Boolean) {
        val preferences =
            mock<UserPreferences> { on { showTakenTimeInOverview } doReturn showTakenTime }
        whenever(preferencesDataSource.preferences).thenReturn(MutableStateFlow(preferences))
    }

    private companion object {
        const val ALL_MEDICINES = -1
    }
}
