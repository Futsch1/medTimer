package com.futsch1.medtimer.reminders

import android.util.Log
import com.futsch1.medtimer.LogTags
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.database.ReminderEventRepository
import com.futsch1.medtimer.model.Medicine
import com.futsch1.medtimer.model.ReminderEvent
import com.futsch1.medtimer.model.ScheduledReminder
import com.futsch1.medtimer.preferences.PreferencesDataSource
import com.futsch1.medtimer.reminders.notificationData.ReminderNotificationData
import com.futsch1.medtimer.reminders.scheduling.ReminderScheduler
import javax.inject.Inject

class ScheduleNextReminderNotificationProcessor @Inject constructor(
    private val alarmProcessor: AlarmProcessor,
    private val medicineRepository: MedicineRepository,
    private val reminderEventRepository: ReminderEventRepository,
    private val timeAccess: TimeAccess,
    private val preferencesDataSource: PreferencesDataSource
) {

    suspend fun scheduleNextReminder() {
        val medicines = medicineRepository.getAll()
        val reminderEvents = reminderEventRepository.getForScheduling(medicines)
        Log.d(LogTags.REMINDER, "Schedule next reminders, considering events ${reminderEvents.map { it.reminderEventId }}")
        scheduleNextReminderInternal(medicines, reminderEvents)
    }

    private fun scheduleNextReminderInternal(
        medicines: List<Medicine>,
        reminderEvents: List<ReminderEvent>
    ) {
        val reminderScheduler = ReminderScheduler(timeAccess, preferencesDataSource)
        val scheduledReminders: List<ScheduledReminder> =
            reminderScheduler.schedule(medicines, reminderEvents)
        if (scheduledReminders.isNotEmpty()) {
            val scheduledReminderNotificationData =
                ReminderNotificationData.fromScheduledReminders(
                    if (preferencesDataSource.preferences.value.combineNotifications) scheduledReminders else listOf(
                        scheduledReminders[0]
                    )
                )
            alarmProcessor.setAlarmForReminderNotification(scheduledReminderNotificationData)
        } else {
            Log.d(LogTags.REMINDER, "No reminders scheduled")
            alarmProcessor.cancelNextReminder()
        }
    }
}
