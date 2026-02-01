package com.futsch1.medtimer.reminders

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.preference.PreferenceManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.futsch1.medtimer.LogTags
import com.futsch1.medtimer.database.FullMedicine
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.database.ReminderEvent
import com.futsch1.medtimer.preferences.PreferencesNames
import com.futsch1.medtimer.reminders.notificationData.ReminderNotificationData
import com.futsch1.medtimer.reminders.scheduling.ReminderScheduler
import com.futsch1.medtimer.reminders.scheduling.ReminderScheduler.TimeAccess
import com.futsch1.medtimer.reminders.scheduling.ScheduledReminder
import java.time.LocalDate
import java.time.ZoneId

/**
 * [Worker] responsible for calculating and scheduling the next medicine reminder notification.
 *
 * This worker retrieves all medicines and their history from the [MedicineRepository],
 * determines the next upcoming reminder(s) using [ReminderScheduler], and updates the
 * system alarm via [AlarmProcessor].
 *
 * It respects user preferences regarding notification combining:
 * - If notifications are combined, it schedules an alarm for all reminders occurring at the next time slot.
 * - If not combined, it schedules an alarm only for the single next reminder.
 *
 * If no future reminders are found, any existing next reminder alarm is cancelled.
 */
class ScheduleNextReminderNotificationWorker(val context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {
    val alarmSetter = AlarmProcessor(context)

    override fun doWork(): Result {
        val medicineRepository = MedicineRepository(applicationContext as Application)
        val fullMedicines = medicineRepository.medicines
        val reminderEvents = medicineRepository.getReminderEventsForScheduling(fullMedicines)

        scheduleNextReminder(fullMedicines, reminderEvents)

        return Result.success()
    }

    private fun scheduleNextReminder(
        fullMedicines: List<FullMedicine>,
        reminderEvents: List<ReminderEvent>
    ) {
        val reminderScheduler = this.reminderScheduler
        val scheduledReminders: List<ScheduledReminder> =
            reminderScheduler.schedule(fullMedicines, reminderEvents)
        if (scheduledReminders.isNotEmpty()) {
            val combinedReminders = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PreferencesNames.COMBINE_NOTIFICATIONS, false)
            val scheduledReminderNotificationData =
                ReminderNotificationData.fromScheduledReminders(if (combinedReminders) scheduledReminders else listOf(scheduledReminders[0]))
            alarmSetter.setAlarmForReminderNotification(scheduledReminderNotificationData, inputData)
        } else {
            Log.d(LogTags.REMINDER, "No reminders scheduled")
            alarmSetter.cancelNextReminder()
        }
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
