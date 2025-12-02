package com.futsch1.medtimer.reminders

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.preference.PreferenceManager
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkRequest
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.futsch1.medtimer.ActivityCodes
import com.futsch1.medtimer.LogTags
import com.futsch1.medtimer.ScheduledReminder
import com.futsch1.medtimer.WorkManagerAccess
import com.futsch1.medtimer.database.MedicineRepository
import com.futsch1.medtimer.preferences.PreferencesNames
import com.futsch1.medtimer.reminders.ReminderProcessor.Companion.getReminderAction
import com.futsch1.medtimer.reminders.ReminderProcessor.Companion.requestRescheduleNowForTests
import com.futsch1.medtimer.reminders.notificationData.ReminderNotificationData
import com.futsch1.medtimer.reminders.scheduling.ReminderScheduler
import com.futsch1.medtimer.reminders.scheduling.ReminderScheduler.TimeAccess
import com.futsch1.medtimer.widgets.WidgetUpdateReceiver
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Worker that schedules the next reminder.
 */
open class RescheduleWork(@JvmField protected val context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {
    private val alarmManager: AlarmManager = context.getSystemService(AlarmManager::class.java)

    override fun doWork(): Result {
        val medicineRepository = MedicineRepository(applicationContext as Application)
        val reminderScheduler = this.reminderScheduler
        val fullMedicines = medicineRepository.medicines
        val scheduledReminders: List<ScheduledReminder> =
            reminderScheduler.schedule(fullMedicines, medicineRepository.getReminderEventsForScheduling(fullMedicines))
        if (scheduledReminders.isNotEmpty()) {
            val combinedReminders = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PreferencesNames.COMBINE_NOTIFICATIONS, true)
            val scheduledReminderNotificationData =
                ReminderNotificationData.fromScheduledReminders(if (combinedReminders) scheduledReminders else listOf(scheduledReminders[0]))
            this.enqueueNotification(scheduledReminderNotificationData)
        } else {
            Log.d(LogTags.SCHEDULER, "No reminders scheduled")
            this.cancelNextReminder()
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

    protected fun enqueueNotification(scheduledReminderNotificationData: ReminderNotificationData) {
        // Apply debug rescheduling
        var scheduledInstant = scheduledReminderNotificationData.remindInstant
        val debugReschedule = DebugReschedule(context, inputData)
        scheduledInstant = debugReschedule.adjustTimestamp(scheduledInstant)

        // Cancel potentially already running alarm and set new
        alarmManager.cancel(PendingIntent.getBroadcast(context, 0, Intent(), PendingIntent.FLAG_IMMUTABLE))
        for (reminderEventId in scheduledReminderNotificationData.reminderEventIds) {
            alarmManager.cancel(PendingIntent.getBroadcast(context, reminderEventId, Intent(), PendingIntent.FLAG_IMMUTABLE))
        }

        // If the alarm is in the future, schedule with alarm manager
        if (scheduledInstant.isAfter(Instant.now())) {
            val pendingIntent = scheduledReminderNotificationData.getPendingIntent(context)

            if (canScheduleExactAlarms(alarmManager)) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, scheduledInstant.toEpochMilli(), pendingIntent)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, scheduledInstant.toEpochMilli(), pendingIntent)
            }

            Log.i(
                LogTags.SCHEDULER,
                String.format(
                    "Scheduled reminder: %s",
                    scheduledReminderNotificationData
                )
            )

            updateNextReminderWidget()
        } else {
            // Immediately remind
            Log.i(
                LogTags.SCHEDULER,
                String.format(
                    "Scheduling reminder now: %s",
                    scheduledReminderNotificationData
                )
            )
            val builder = Data.Builder()
            scheduledReminderNotificationData.toBuilder(builder)
            val reminderWork: WorkRequest =
                OneTimeWorkRequest.Builder(ReminderWork::class.java)
                    .setInputData(builder.build())
                    .build()
            WorkManagerAccess.getWorkManager(context).enqueue(reminderWork)
        }

        debugReschedule.scheduleRepeat()
    }

    private fun cancelNextReminder() {
        // Pending reminders are distinguished by their request code, which is the reminder event id.
        // So if we cancel the reminderEventId 0, we cancel all the next reminder that was not yet raised.
        val intent = getReminderAction(context)
        val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        alarmManager.cancel(pendingIntent)
    }

    private fun canScheduleExactAlarms(alarmManager: AlarmManager): Boolean {
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        val exactReminders = sharedPref.getBoolean(PreferencesNames.EXACT_REMINDERS, true)

        return exactReminders && (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms())
    }

    private fun updateNextReminderWidget() {
        val intent = Intent(context, WidgetUpdateReceiver::class.java)
        intent.setAction("com.futsch1.medtimer.NEXT_REMINDER_WIDGET_UPDATE")
        context.sendBroadcast(intent, "com.futsch1.medtimer.NOTIFICATION_PROCESSED")
    }

    private class DebugReschedule(var context: Context, inputData: Data) {
        var delay: Long = inputData.getLong(ActivityCodes.EXTRA_SCHEDULE_FOR_TESTS, -1)
        var repeats: Int = inputData.getInt(ActivityCodes.EXTRA_REMAINING_REPEATS, -1)

        fun adjustTimestamp(instant: Instant): Instant {
            return if (delay >= 0) {
                Instant.now().plusMillis(delay)
            } else {
                instant
            }
        }

        fun scheduleRepeat() {
            if (delay >= 0 && repeats > 0) {
                requestRescheduleNowForTests(context, delay, repeats - 1)
            }
        }
    }
}
