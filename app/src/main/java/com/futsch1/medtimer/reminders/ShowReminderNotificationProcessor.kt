package com.futsch1.medtimer.reminders

import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.work.Worker
import com.futsch1.medtimer.LogTags
import com.futsch1.medtimer.reminders.notificationData.ReminderNotificationData

/**
 * Responsible for triggering the display of a medication reminder notification.
 *
 * This class retrieves reminder data from its input, checks if a notification for the
 * specific reminder events is already active to prevent duplicates, and delegates
 * the actual notification scheduling to [AlarmProcessor].
 */
class ShowReminderNotificationProcessor(val context: Context) {
    val alarmSetter = AlarmProcessor(context)

    fun showReminder(reminderNotificationData: ReminderNotificationData) {
        Log.d(LogTags.REMINDER, "Scheduling reminder: $reminderNotificationData")

        // Check if given notification ID is already active
        if (!isNotificationActive(reminderNotificationData)) {
            alarmSetter.setAlarmForReminderNotification(reminderNotificationData)
        }

        ScheduleNextReminderNotificationProcessor(context).scheduleNextReminder()
    }

    private fun isNotificationActive(reminderNotificationData: ReminderNotificationData): Boolean {
        if (reminderNotificationData.notificationId != -1) {
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            for (notification in notificationManager.activeNotifications) {
                if (notification.id == reminderNotificationData.notificationId) {
                    val notificationData = ReminderNotificationData.fromBundle(notification.notification.extras)
                    // Check if all reminder event IDs from the notification are also in the reminder notification data
                    val reminderEventIds = reminderNotificationData.reminderEventIds.toList()
                    val notificationReminderEventIds = notificationData.reminderEventIds.toList()
                    if (notificationReminderEventIds.containsAll(reminderEventIds)) {
                        Log.d(
                            LogTags.REMINDER,
                            "Notification nID ${reminderNotificationData.notificationId} found, reminder event IDs $reminderEventIds are in covered IDs $notificationReminderEventIds"
                        )
                        return true
                    }
                }
            }
        }
        return false
    }
}