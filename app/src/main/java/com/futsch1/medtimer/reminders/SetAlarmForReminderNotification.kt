package com.futsch1.medtimer.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.preference.PreferenceManager
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkRequest
import com.futsch1.medtimer.ActivityCodes
import com.futsch1.medtimer.LogTags
import com.futsch1.medtimer.WorkManagerAccess
import com.futsch1.medtimer.preferences.PreferencesNames
import com.futsch1.medtimer.reminders.ReminderWorkerReceiver.Companion.requestScheduleNowForTests
import com.futsch1.medtimer.reminders.notificationData.ReminderNotificationData
import com.futsch1.medtimer.widgets.WidgetUpdateReceiver
import java.time.Instant

/**
 * Helper class that sets an alarm for a reminder notification.
 */
class SetAlarmForReminderNotification(val context: Context) {
    private val alarmManager: AlarmManager = context.getSystemService(AlarmManager::class.java)

    fun setAlarmForReminderNotification(scheduledReminderNotificationData: ReminderNotificationData, inputData: Data) {
        // Apply debug rescheduling
        var scheduledInstant = scheduledReminderNotificationData.remindInstant
        val debugReschedule = DebugReschedule(context, inputData)
        scheduledInstant = debugReschedule.adjustTimestamp(scheduledInstant)

        // Cancel potentially already running alarm and set new
        alarmManager.cancel(PendingIntent.getBroadcast(context, 0, Intent(), PendingIntent.FLAG_IMMUTABLE))
        for (reminderEventId in scheduledReminderNotificationData.reminderEventIds) {
            if (reminderEventId != 0) {
                cancelRepeatReminderEvent(context, reminderEventId)
            }
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
                LogTags.REMINDER,
                String.format(
                    "Set alarm for reminder notification: %s",
                    scheduledReminderNotificationData
                )
            )

            updateNextReminderWidget()
        } else {
            // Immediately remind
            Log.i(
                LogTags.REMINDER,
                String.format(
                    "Show reminder notification now: %s",
                    scheduledReminderNotificationData
                )
            )
            val builder = Data.Builder()
            scheduledReminderNotificationData.toBuilder(builder)
            val reminderNotificationWorker: WorkRequest =
                OneTimeWorkRequest.Builder(ReminderNotificationWorker::class.java)
                    .setInputData(builder.build())
                    .build()
            WorkManagerAccess.getWorkManager(context).enqueue(reminderNotificationWorker)
        }

        debugReschedule.scheduleRepeat()
    }

    fun cancelNextReminder() {
        // Pending reminders are distinguished by their request code, which is the reminder event id.
        // So if we cancel the reminderEventId 0, we cancel all the next reminder that was not yet raised.
        val intent = getReminderAction(context)
        val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        alarmManager.cancel(pendingIntent)
    }

    private fun canScheduleExactAlarms(alarmManager: AlarmManager): Boolean {
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(context)
        val exactReminders = sharedPref.getBoolean(PreferencesNames.EXACT_REMINDERS, true)

        return exactReminders && (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms())
    }

    private fun updateNextReminderWidget() {
        val intent = Intent(context, WidgetUpdateReceiver::class.java)
        intent.setAction("com.futsch1.medtimer.NEXT_REMINDER_WIDGET_UPDATE")
        context.sendBroadcast(intent, "com.futsch1.medtimer.NOTIFICATION_PROCESSED")
    }

    companion object {
        fun cancelRepeatReminderEvent(context: Context, reminderEventId: Int) {
            val alarmManager = context.getSystemService(AlarmManager::class.java)
            alarmManager.cancel(PendingIntent.getBroadcast(context, reminderEventId, getReminderAction(context), PendingIntent.FLAG_IMMUTABLE))
            Log.d(LogTags.REMINDER, "Cancel repeat alarm for reID $reminderEventId")
        }
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
                requestScheduleNowForTests(context, delay, repeats - 1)
            }
        }
    }
}
