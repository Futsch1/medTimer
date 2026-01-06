package com.futsch1.medtimer.reminders

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.preference.PreferenceManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.futsch1.medtimer.LogTags
import com.futsch1.medtimer.ScheduledReminder
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.preferences.PreferencesNames
import com.futsch1.medtimer.reminders.notificationData.ReminderNotificationData
import com.futsch1.medtimer.reminders.scheduling.ReminderScheduler
import com.futsch1.medtimer.reminders.scheduling.ReminderScheduler.TimeAccess
import java.time.LocalDate
import java.time.ZoneId

/**
 * Worker that schedules the next reminder.
 */
open class ScheduleNextReminderNotificationWorker(val context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {
    val alarmSetter = SetAlarmForReminderNotification(context)

    override fun doWork(): Result {
        val medicineRepository = MedicineRepository(applicationContext as Application)
        val reminderScheduler = this.reminderScheduler
        val fullMedicines = medicineRepository.medicines
        val scheduledReminders: List<ScheduledReminder> =
            reminderScheduler.schedule(fullMedicines, medicineRepository.getReminderEventsForScheduling(fullMedicines))
        if (scheduledReminders.isNotEmpty()) {
            val combinedReminders = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PreferencesNames.COMBINE_NOTIFICATIONS, false)
            val scheduledReminderNotificationData =
                ReminderNotificationData.fromScheduledReminders(if (combinedReminders) scheduledReminders else listOf(scheduledReminders[0]))
            alarmSetter.setAlarmForReminderNotification(scheduledReminderNotificationData, inputData)
        } else {
            Log.d(LogTags.REMINDER, "No reminders scheduled")
            alarmSetter.cancelNextReminder()
        }

        return Result.success()
    }

    private val reminderScheduler: ReminderScheduler
        get() = ReminderScheduler(object : TimeAccess {
            override fun systemZone(): ZoneId {
                return ZoneId.systemDefault()
            }

            override fun localDate(): LocalDate {
                return LocalDate.now()
            }
        }, PreferenceManager.getDefaultSharedPreferences(context))

}
