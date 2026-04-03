package com.futsch1.medtimer.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.futsch1.medtimer.LogTags
import com.futsch1.medtimer.preferences.PreferencesDataSource
import com.futsch1.medtimer.reminders.ReminderProcessorBroadcastReceiver.Companion.RECEIVER_PERMISSION
import com.futsch1.medtimer.reminders.notificationData.ReminderNotificationData
import com.futsch1.medtimer.widgets.WidgetUpdateReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import javax.inject.Inject

class AlarmProcessor @Inject constructor(
    @param:ApplicationContext
    private val context: Context,
    private val alarmManager: AlarmManager,
    private val timeAccess: TimeAccess,
    preferencesDataSource: PreferencesDataSource
) {
    private val exactReminders: Boolean = preferencesDataSource.preferences.value.exactReminders

    fun setAlarmForReminderNotification(scheduledReminderNotificationData: ReminderNotificationData) {
        val originalInstant = scheduledReminderNotificationData.remindInstant
        scheduledReminderNotificationData.remindInstant = adjustTimestamp(timeAccess, originalInstant)

        alarmManager.cancel(
            PendingIntent.getBroadcast(
                context, 0,
                getReminderAction(context),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        )
        for (reminderEventId in scheduledReminderNotificationData.reminderEventIds) {
            if (reminderEventId != 0) {
                cancelPendingReminderNotifications(reminderEventId)
            }
        }

        if (!scheduledReminderNotificationData.remindInstant.isAfter(timeAccess.now())) {
            Log.i(
                LogTags.REMINDER,
                String.format(
                    "Show reminder notification now: %s",
                    scheduledReminderNotificationData
                )
            )
            val reminderIntent = getReminderAction(context)
            scheduledReminderNotificationData.toIntent(reminderIntent)
            context.sendBroadcast(reminderIntent, RECEIVER_PERMISSION)
            return
        }

        val pendingIntent = scheduledReminderNotificationData.getPendingIntent(context)
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
        val intent = getReminderAction(context)
        val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        alarmManager.cancel(pendingIntent)
    }

    fun cancelPendingReminderNotifications(reminderEventId: Int) {
        alarmManager.cancel(
            PendingIntent.getBroadcast(
                context, reminderEventId,
                getReminderAction(context),
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
        val intent = Intent(context, WidgetUpdateReceiver::class.java).apply {
            action = "com.futsch1.medtimer.NEXT_REMINDER_WIDGET_UPDATE"
        }

        context.sendBroadcast(intent, "com.futsch1.medtimer.NOTIFICATION_PROCESSED")
    }

    companion object {
        fun adjustTimestamp(timeAccess: TimeAccess, instant: Instant): Instant {
            return if (delay >= 0) {
                Log.d(LogTags.REMINDER, "Debug schedule reminder in $delay milliseconds")
                val instantDebug = timeAccess.now().plusMillis(delay)
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
