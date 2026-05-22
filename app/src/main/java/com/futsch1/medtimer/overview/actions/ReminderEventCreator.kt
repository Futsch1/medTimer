package com.futsch1.medtimer.overview.actions

import com.futsch1.medtimer.core.domain.model.ReminderEvent
import com.futsch1.medtimer.core.domain.model.ScheduledReminder
import com.futsch1.medtimer.core.domain.repository.ReminderEventRepository
import com.futsch1.medtimer.core.domain.repository.ReminderRepository
import com.futsch1.medtimer.helpers.TimeFormatter
import com.futsch1.medtimer.reminders.ReminderNotificationProcessor
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderEventCreator @Inject constructor(
    private val reminderEventRepository: ReminderEventRepository,
    private val reminderRepository: ReminderRepository,
    private val timeFormatter: TimeFormatter,
) {
    suspend fun getOrCreateReminderEvent(scheduledReminder: ScheduledReminder, reminderTimeStamp: Long): ReminderEvent {
        val existingReminderEvent = reminderEventRepository.get(scheduledReminder.reminder.id, scheduledReminder.timestamp.epochSecond)
        if (existingReminderEvent != null) {
            return existingReminderEvent
        }

        val reminder = reminderRepository.get(scheduledReminder.reminder.id) ?: scheduledReminder.reminder
        val newReminderEvent = ReminderNotificationProcessor.buildReminderEvent(
            reminderTimeStamp, scheduledReminder.medicine, reminder, reminderEventRepository, timeFormatter
        )
        return reminderEventRepository.create(newReminderEvent)
    }
}
