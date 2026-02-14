package com.futsch1.medtimer.reminders

import android.util.Log
import com.futsch1.medtimer.LogTags
import com.futsch1.medtimer.reminders.notificationData.ReminderNotificationData
import java.time.Instant

/**
 * Handles the snooze functionality for reminders.
 *
 * This class calculates a new reminder time based on the provided snooze duration,
 * cancels any existing notifications or pending alarms for the reminder, and

 * schedules a new alarm for the future.
 */
open class SnoozeProcessor(val reminderContext: ReminderContext) {
    val alarmSetter = AlarmProcessor(reminderContext)

    fun processSnooze(reminderNotificationData: ReminderNotificationData, snoozeTime: Int) {
        reminderNotificationData.remindInstant = Instant.now().plusSeconds(snoozeTime * 60L)
        Log.d(LogTags.REMINDER, "Snoozing reminder: $reminderNotificationData")

        // Cancel a potential repeat alarm
        alarmSetter.cancelPendingReminderNotifications(reminderNotificationData)

        alarmSetter.setAlarmForReminderNotification(reminderNotificationData)

        NotificationProcessor(reminderContext).cancelNotification(reminderNotificationData.notificationId)
    }
}
