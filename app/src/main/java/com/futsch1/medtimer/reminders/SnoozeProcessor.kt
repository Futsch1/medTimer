package com.futsch1.medtimer.reminders

import android.util.Log
import com.futsch1.medtimer.LogTags
import com.futsch1.medtimer.reminders.notificationData.ReminderNotificationData
import java.time.Instant
import javax.inject.Inject

/**
 * Handles the snooze functionality for reminders.
 *
 * This class calculates a new reminder time based on the provided snooze duration,
 * cancels any existing notifications or pending alarms for the reminder, and

 * schedules a new alarm for the future.
 */
open class SnoozeProcessor @Inject constructor(
    val alarmProcessor: AlarmProcessor,
    val notificationProcessor: NotificationProcessor
) {

    fun processSnooze(reminderNotificationData: ReminderNotificationData, snoozeTime: Long) {
        reminderNotificationData.remindInstant = Instant.now().plusSeconds(snoozeTime * 60)
        Log.d(LogTags.REMINDER, "Snoozing reminder: $reminderNotificationData")

        // Cancel a potential repeat alarm
        alarmProcessor.cancelPendingReminderNotifications(reminderNotificationData)

        alarmProcessor.setAlarmForReminderNotification(reminderNotificationData)

        notificationProcessor.cancelNotification(reminderNotificationData.notificationId)
    }
}
