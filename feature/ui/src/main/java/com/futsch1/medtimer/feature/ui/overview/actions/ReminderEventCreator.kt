package com.futsch1.medtimer.feature.ui.overview.actions

import com.futsch1.medtimer.core.domain.model.ReminderEvent
import com.futsch1.medtimer.core.domain.model.ScheduledReminder
import com.futsch1.medtimer.core.domain.repository.ReminderEventRepository
import com.futsch1.medtimer.core.domain.repository.ReminderRepository
import com.futsch1.medtimer.core.ui.TimeFormatter
import com.futsch1.medtimer.feature.reminders.buildReminderEvent
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderEventCreator @Inject constructor(
    private val reminderEventRepository: ReminderEventRepository,
    private val reminderRepository: ReminderRepository,
    private val timeFormatter: TimeFormatter,
) {
    suspend fun getOrCreateReminderEvent(scheduledReminder: ScheduledReminder, reminderTimeStamp: Long): ReminderEvent {
        val existingReminderEvent = reminderEventRepository.fetch(scheduledReminder.reminder.id, scheduledReminder.timestamp.epochSecond)
        if (existingReminderEvent != null) {
            return existingReminderEvent
        }

        val reminder = reminderRepository.fetch(scheduledReminder.reminder.id) ?: scheduledReminder.reminder
        val newReminderEvent = buildReminderEvent(
            reminderTimeStamp, scheduledReminder.medicine, reminder, reminderEventRepository, timeFormatter
        )
        return reminderEventRepository.create(newReminderEvent)
    }
}
