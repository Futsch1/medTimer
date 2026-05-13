package com.futsch1.medtimer.reminders

import android.util.Log
import com.futsch1.medtimer.LogTags
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.database.ReminderEventRepository
import com.futsch1.medtimer.preferences.PreferencesDataSource
import com.futsch1.medtimer.reminders.notificationData.ReminderNotificationData
import com.futsch1.medtimer.reminders.scheduling.ReminderScheduler
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScheduleNextReminderNotificationProcessor @Inject constructor(
    private val alarmProcessor: AlarmProcessor,
    private val reminderNotificationProcessor: ReminderNotificationProcessor,
    private val medicineRepository: MedicineRepository,
    private val reminderEventRepository: ReminderEventRepository,
    private val timeAccess: TimeAccess,
    private val preferencesDataSource: PreferencesDataSource
) {
    private val mutex = Mutex()

    suspend fun scheduleNextReminder() {
        mutex.withLock {
            var futureReminderScheduled = false
            val medicines = medicineRepository.getAll()

            while(!futureReminderScheduled) {
                val reminderEvents = reminderEventRepository.getForScheduling(medicines)
                Log.d(
                    LogTags.REMINDER,
                    "Schedule next reminders, considering events ${reminderEvents.map { it.reminderEventId }}"
                )

                val scheduledReminders =
                    ReminderScheduler(timeAccess, preferencesDataSource).schedule(
                        medicines,
                        reminderEvents
                    )
                if (scheduledReminders.isEmpty()) {
                    Log.d(LogTags.REMINDER, "No reminders scheduled")
                    alarmProcessor.cancelNextReminder()
                    return
                }

                val data = ReminderNotificationData.fromScheduledReminders(
                    if (preferencesDataSource.preferences.value.combineNotifications) scheduledReminders
                    else listOf(scheduledReminders[0])
                )

                futureReminderScheduled = alarmProcessor.setAlarmForReminderNotification(data, reminderNotificationProcessor)
            }
        }
    }
}
