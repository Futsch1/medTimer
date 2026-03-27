package com.futsch1.medtimer.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.futsch1.medtimer.LogTags
import com.futsch1.medtimer.reminders.ReminderProcessorBroadcastReceiver.Companion.RECEIVER_PERMISSION
import com.futsch1.medtimer.reminders.notificationData.ReminderNotificationData
import com.futsch1.medtimer.widgets.WidgetUpdateReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import javax.inject.Inject

/**
 * Handles the scheduling and cancellation of alarms for medication reminders using [AlarmManager].
 */
class AlarmProcessor @Inject constructor(
    @param:ApplicationContext
    private val context: Context,
    private val reminderContext: ReminderContext,
    private val alarmManager: AlarmManager
) {
    private val exactReminders: Boolean = reminderContext.preferencesDataSource.preferences.value.exactReminders

    fun setAlarmForReminderNotification(scheduledReminderNotificationData: ReminderNotificationData) {
        // Apply debug rescheduling
        val originalInstant = scheduledReminderNotificationData.remindInstant
        scheduledReminderNotificationData.remindInstant = adjustTimestamp(reminderContext, originalInstant)

        // Cancel potentially already running alarm and set new
        alarmManager.cancel(
            reminderContext.getPendingIntentBroadcast(
                0,
                getReminderAction(reminderContext.context),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        )
        for (reminderEventId in scheduledReminderNotificationData.reminderEventIds) {
            if (reminderEventId != 0) {
                cancelPendingReminderNotifications(reminderEventId)
            }
        }

        // Immediately remind if the alarm is not in the future
        if (!scheduledReminderNotificationData.remindInstant.isAfter(reminderContext.timeAccess.now())) {
            Log.i(
                LogTags.REMINDER,
                String.format(
                    "Show reminder notification now: %s",
                    scheduledReminderNotificationData
                )
            )
            val reminderIntent = getReminderAction(reminderContext.context)
            scheduledReminderNotificationData.toIntent(reminderIntent)
            context.sendBroadcast(reminderIntent, RECEIVER_PERMISSION)
            return
        }

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
    }

    fun cancelNextReminder() {
        // Pending reminders are distinguished by their request code, which is the reminder event id.
        // So if we cancel the reminderEventId 0, we cancel all the next reminder that was not yet raised.
        val intent = getReminderAction(reminderContext.context)
        val pendingIntent = reminderContext.getPendingIntentBroadcast(0, intent, PendingIntent.FLAG_IMMUTABLE)
        alarmManager.cancel(pendingIntent)
    }

    fun cancelPendingReminderNotifications(reminderEventId: Int) {
        alarmManager.cancel(
            reminderContext.getPendingIntentBroadcast(
                reminderEventId,
                getReminderAction(reminderContext.context),
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
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            exactReminders && alarmManager.canScheduleExactAlarms()
        } else {
            exactReminders
        }
    }

    private fun updateNextReminderWidget() {
        val intent = Intent("com.futsch1.medtimer.NEXT_REMINDER_WIDGET_UPDATE")
        reminderContext.setIntentClass(intent, WidgetUpdateReceiver::class.java)
        context.sendBroadcast(intent, "com.futsch1.medtimer.NOTIFICATION_PROCESSED")
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
