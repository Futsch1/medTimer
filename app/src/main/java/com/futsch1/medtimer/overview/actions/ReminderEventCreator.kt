package com.futsch1.medtimer.overview.actions

import com.futsch1.medtimer.database.ReminderEventRepository
import com.futsch1.medtimer.di.Dispatcher
import com.futsch1.medtimer.di.MedTimerDispatchers
import com.futsch1.medtimer.helpers.TimeFormatter
import com.futsch1.medtimer.model.ScheduledReminder
import com.futsch1.medtimer.model.ReminderEvent
import com.futsch1.medtimer.reminders.ReminderNotificationProcessor
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderEventCreator @Inject constructor(
    private val reminderEventRepository: ReminderEventRepository,
    private val timeFormatter: TimeFormatter,
    @param:Dispatcher(MedTimerDispatchers.IO) private val ioDispatcher: CoroutineDispatcher
) {
    suspend fun getOrCreateReminderEvent(scheduledReminder: ScheduledReminder, reminderTimeStamp: Long): ReminderEvent = withContext(ioDispatcher) {
        val existingReminderEvent = reminderEventRepository.get(scheduledReminder.reminder.reminderId, scheduledReminder.timestamp.epochSecond)
        if (existingReminderEvent != null) {
            return@withContext existingReminderEvent
        }

        val newReminderEvent = ReminderNotificationProcessor.buildReminderEvent(
            reminderTimeStamp, scheduledReminder.medicine, scheduledReminder.reminder, reminderEventRepository, timeFormatter
        )
        return@withContext reminderEventRepository.create(newReminderEvent)
    }
}
