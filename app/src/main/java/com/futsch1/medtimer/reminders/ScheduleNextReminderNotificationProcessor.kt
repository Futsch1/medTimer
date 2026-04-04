package com.futsch1.medtimer.reminders

import android.util.Log
import com.futsch1.medtimer.LogTags
import com.futsch1.medtimer.database.FullMedicine
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.database.ReminderEvent
import com.futsch1.medtimer.database.ReminderEventRepository
import com.futsch1.medtimer.preferences.PreferencesDataSource
import com.futsch1.medtimer.reminders.notificationData.ReminderNotificationData
import com.futsch1.medtimer.reminders.scheduling.ReminderScheduler
import com.futsch1.medtimer.reminders.scheduling.ScheduledReminder
import javax.inject.Inject

class ScheduleNextReminderNotificationProcessor @Inject constructor(
    private val alarmProcessor: AlarmProcessor,
    private val medicineRepository: MedicineRepository,
    private val reminderEventRepository: ReminderEventRepository,
    private val timeAccess: TimeAccess,
    private val preferencesDataSource: PreferencesDataSource
) {

    suspend fun scheduleNextReminder(processedEvents: List<ReminderEvent> = emptyList()) {
        val fullMedicines = medicineRepository.getFullAll()
        val reminderEvents = reminderEventRepository.getForScheduling(fullMedicines)
        val allEvents = (reminderEvents + processedEvents).distinctBy { it.reminderEventId }

        scheduleNextReminderInternal(fullMedicines, allEvents)
    }

    private fun scheduleNextReminderInternal(
        fullMedicines: List<FullMedicine>,
        reminderEvents: List<ReminderEvent>
    ) {
        val reminderScheduler = ReminderScheduler(timeAccess, preferencesDataSource)
        val scheduledReminders: List<ScheduledReminder> =
            reminderScheduler.schedule(fullMedicines, reminderEvents)
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
