package com.futsch1.medtimer.overview.actions

import com.futsch1.medtimer.database.ReminderEvent
import com.futsch1.medtimer.database.ReminderEventRepository
import com.futsch1.medtimer.di.Dispatcher
import com.futsch1.medtimer.di.MedTimerDispatchers
import com.futsch1.medtimer.helpers.TimeFormatter
import com.futsch1.medtimer.reminders.ReminderNotificationProcessor
import com.futsch1.medtimer.reminders.scheduling.ScheduledReminder
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
        var reminderEvent = reminderEventRepository.get(scheduledReminder.reminder.reminderId, scheduledReminder.timestamp.epochSecond)
        if (reminderEvent != null) {
            return@withContext reminderEvent
        }

        reminderEvent = ReminderNotificationProcessor.buildReminderEvent(
            reminderTimeStamp, scheduledReminder.medicine, scheduledReminder.reminder, reminderEventRepository, timeFormatter
        )
        reminderEvent.reminderEventId = reminderEventRepository.create(reminderEvent).toInt()
        return@withContext reminderEvent
    }
}
