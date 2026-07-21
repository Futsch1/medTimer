package com.futsch1.medtimer.feature.ui.impl.overview.actions

import com.futsch1.medtimer.core.datastore.PreferencesDataSource
import com.futsch1.medtimer.core.domain.model.ReminderEvent
import com.futsch1.medtimer.core.domain.model.ScheduledReminder
import com.futsch1.medtimer.core.domain.repository.ReminderEventRepository
import com.futsch1.medtimer.core.domain.repository.ReminderRepository
import com.futsch1.medtimer.core.ui.TimeFormatter
import com.futsch1.medtimer.feature.reminders.api.buildReminderEvent
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderEventCreator @Inject constructor(
    private val reminderEventRepository: ReminderEventRepository,
    private val reminderRepository: ReminderRepository,
    private val timeFormatter: TimeFormatter,
    private val preferencesDataSource: PreferencesDataSource
) {
    suspend fun getOrCreateReminderEvent(scheduledReminder: ScheduledReminder, reminderTimeStamp: Long): ReminderEvent {
        val existingReminderEvent = reminderEventRepository.fetch(scheduledReminder.reminder.id, scheduledReminder.timestamp.epochSecond)
        if (existingReminderEvent != null) {
            if (existingReminderEvent.remindedTimestamp.epochSecond != reminderTimeStamp) {
                val rescheduledEvent = existingReminderEvent.copy(remindedTimestamp = Instant.ofEpochSecond(reminderTimeStamp))
                reminderEventRepository.update(rescheduledEvent)
                return rescheduledEvent
            }
            return existingReminderEvent
        }

        val reminder = reminderRepository.fetch(scheduledReminder.reminder.id) ?: scheduledReminder.reminder
        val newReminderEvent = buildReminderEvent(
            reminderTimeStamp, scheduledReminder.medicine, reminder, reminderEventRepository, preferencesDataSource.preferences.value.numberOfRepetitions
        ) { timeFormatter.localDateToString(it) }
        return reminderEventRepository.create(newReminderEvent)
    }
}
