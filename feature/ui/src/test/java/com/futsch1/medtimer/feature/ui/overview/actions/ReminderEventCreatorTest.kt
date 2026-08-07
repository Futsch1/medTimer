package com.futsch1.medtimer.feature.ui.overview.actions

import com.futsch1.medtimer.core.datastore.PreferencesDataSource
import com.futsch1.medtimer.core.domain.model.Medicine
import com.futsch1.medtimer.core.domain.model.Reminder
import com.futsch1.medtimer.core.domain.model.ReminderEvent
import com.futsch1.medtimer.core.domain.model.ScheduledReminder
import com.futsch1.medtimer.core.domain.repository.ReminderEventRepository
import com.futsch1.medtimer.core.domain.repository.ReminderRepository
import com.futsch1.medtimer.core.ui.TimeFormatter
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Instant
import kotlin.test.assertEquals

class ReminderEventCreatorTest {

    private val reminderEventRepository: ReminderEventRepository = mock()
    private val reminderRepository: ReminderRepository = mock()
    private val timeFormatter: TimeFormatter = mock()
    private val preferencesDataSource: PreferencesDataSource = mock()

    private val creator = ReminderEventCreator(reminderEventRepository, reminderRepository, timeFormatter, preferencesDataSource)

    @Test
    fun `resolvePending moves an already-materialized event to the requested time`() = runTest {
        val originalTimestamp = Instant.ofEpochSecond(1_000)
        val newTimestamp = Instant.ofEpochSecond(2_000)
        val reminder = Reminder.default().copy(id = 7)
        val scheduledReminder = ScheduledReminder(
            medicine = Medicine.default().copy(id = 3),
            reminder = reminder,
            timestamp = originalTimestamp
        )
        val existingEvent = ReminderEvent.default().copy(
            reminderEventId = 42,
            reminderId = 7,
            remindedTimestamp = originalTimestamp
        )
        whenever(reminderEventRepository.fetch(7, originalTimestamp.epochSecond)).thenReturn(existingEvent)

        val result = creator.resolvePending(scheduledReminder, newTimestamp.epochSecond)

        assertEquals(existingEvent.copy(remindedTimestamp = newTimestamp), result)
    }

    @Test
    fun `getOrCreateReminderEvent upserts the resolved pending event`() = runTest {
        val originalTimestamp = Instant.ofEpochSecond(1_000)
        val newTimestamp = Instant.ofEpochSecond(2_000)
        val reminder = Reminder.default().copy(id = 7)
        val scheduledReminder = ScheduledReminder(
            medicine = Medicine.default().copy(id = 3),
            reminder = reminder,
            timestamp = originalTimestamp
        )
        val existingEvent = ReminderEvent.default().copy(
            reminderEventId = 42,
            reminderId = 7,
            remindedTimestamp = originalTimestamp
        )
        val pendingEvent = existingEvent.copy(remindedTimestamp = newTimestamp)
        whenever(reminderEventRepository.fetch(7, originalTimestamp.epochSecond)).thenReturn(existingEvent)
        whenever(reminderEventRepository.upsert(pendingEvent)).thenReturn(pendingEvent)

        val result = creator.getOrCreateReminderEvent(scheduledReminder, newTimestamp.epochSecond)

        assertEquals(newTimestamp, result.remindedTimestamp)
        verify(reminderEventRepository).upsert(pendingEvent)
    }
}
