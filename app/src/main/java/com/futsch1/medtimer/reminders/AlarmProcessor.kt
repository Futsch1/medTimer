package com.futsch1.medtimer.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.util.Log
import com.futsch1.medtimer.LogTags
import com.futsch1.medtimer.preferences.PreferencesNames
import com.futsch1.medtimer.reminders.notificationData.ReminderNotificationData
import com.futsch1.medtimer.widgets.WidgetUpdateReceiver
import java.time.Instant

/**
 * Handles the scheduling and cancellation of alarms for medication reminders using [AlarmManager].
 */
class AlarmProcessor(val reminderContext: ReminderContext) {
    private val alarmManager: AlarmManager = reminderContext.alarmManager

    fun setAlarmForReminderNotification(scheduledReminderNotificationData: ReminderNotificationData) {
        // Apply debug rescheduling
        val originalInstant = scheduledReminderNotificationData.remindInstant
        scheduledReminderNotificationData.remindInstant = adjustTimestamp(reminderContext, originalInstant)

        // Cancel potentially already running alarm and set new
        alarmManager.cancel(
            reminderContext.getPendingIntentBroadcast(
                0,
                getReminderAction(reminderContext),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        )
        for (reminderEventId in scheduledReminderNotificationData.reminderEventIds) {
            if (reminderEventId != 0) {
                cancelPendingReminderNotifications(reminderEventId)
            }
        }

        // If the alarm is in the future, schedule with alarm manager
        if (scheduledReminderNotificationData.remindInstant.isAfter(reminderContext.timeAccess.now())) {
            val pendingIntent = scheduledReminderNotificationData.getPendingIntent(reminderContext)

            if (canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, scheduledReminderNotificationData.remindInstant.toEpochMilli(), pendingIntent)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, scheduledReminderNotificationData.remindInstant.toEpochMilli(), pendingIntent)
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
            ReminderNotificationProcessor(reminderContext).processReminders(scheduledReminderNotificationData)
        }
    }

    fun cancelNextReminder() {
        // Pending reminders are distinguished by their request code, which is the reminder event id.
        // So if we cancel the reminderEventId 0, we cancel all the next reminder that was not yet raised.
        val intent = getReminderAction(reminderContext)
        val pendingIntent = reminderContext.getPendingIntentBroadcast(0, intent, PendingIntent.FLAG_IMMUTABLE)
        alarmManager.cancel(pendingIntent)
    }

    fun cancelPendingReminderNotifications(reminderEventId: Int) {
        alarmManager.cancel(
            reminderContext.getPendingIntentBroadcast(
                reminderEventId,
                getReminderAction(reminderContext),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        )
        Log.d(LogTags.REMINDER, "Cancel pending reminder notification alarms for reID $reminderEventId")
    }

    fun cancelPendingReminderNotifications(reminderNotificationData: ReminderNotificationData) {
        for (id in reminderNotificationData.reminderEventIds) {
            cancelPendingReminderNotifications(id)
        }
    }

    private fun canScheduleExactAlarms(): Boolean {
        val exactReminders = reminderContext.preferences.getBoolean(PreferencesNames.EXACT_REMINDERS, true)

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            exactReminders && (reminderContext.sdkInt >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms())
        } else {
            exactReminders
        }
    }

    private fun updateNextReminderWidget() {
        val intent = Intent("com.futsch1.medtimer.NEXT_REMINDER_WIDGET_UPDATE")
        reminderContext.setIntentClass(intent, WidgetUpdateReceiver::class.java)
        reminderContext.sendBroadcast(intent, "com.futsch1.medtimer.NOTIFICATION_PROCESSED")
    }

    companion object {
        fun adjustTimestamp(reminderContext: ReminderContext, instant: Instant): Instant {
            return if (delay >= 0) {
                Log.d(LogTags.REMINDER, "Debug schedule reminder in $delay milliseconds")
                val instantDebug = reminderContext.timeAccess.now().plusMillis(delay)
                repeats -= 1
                if (repeats < 0) {
                    delay = -1
                }
                instantDebug
            } else {
                instant
            }
        }

        var delay: Long = -1
        var repeats: Int = -1
    }

}
