package com.futsch1.medtimer.reminders

import android.util.Log
import com.futsch1.medtimer.LogTags
import com.futsch1.medtimer.reminders.notificationData.ReminderNotificationData

/**
 * Responsible for triggering the display of a medication reminder notification.
 *
 * This class retrieves reminder data from its input, checks if a notification for the
 * specific reminder events is already active to prevent duplicates, and delegates
 * the actual notification scheduling to [AlarmProcessor].
 */
class ShowReminderNotificationProcessor(val reminderContext: ReminderContext) {
    val alarmSetter = AlarmProcessor(reminderContext)

    fun showReminder(reminderNotificationData: ReminderNotificationData) {
        Log.d(LogTags.REMINDER, "Request show notification for reminder: $reminderNotificationData")

        // Check if given notification ID is already active
        if (!isNotificationActive(reminderNotificationData)) {
            alarmSetter.setAlarmForReminderNotification(reminderNotificationData)
        }

        ScheduleNextReminderNotificationProcessor(reminderContext).scheduleNextReminder()
    }

    private fun isNotificationActive(reminderNotificationData: ReminderNotificationData): Boolean {
        val notificationData = getNotificationData(reminderNotificationData.notificationId)
        if (notificationData != null) {
            // Check if all reminder event IDs from the notification are also in the reminder notification data
            val reminderEventIds = reminderNotificationData.reminderEventIds.toList()
            val notificationReminderEventIds = notificationData.reminderEventIds.toList()
            if (notificationData.remindInstant != reminderNotificationData.remindInstant) {
                Log.d(
                    LogTags.REMINDER,
                    "Notification nID ${reminderNotificationData.notificationId} found, but reminder was rescheduled"
                )
                NotificationProcessor(reminderContext).removeRemindersFromNotification(
                    reminderNotificationData.notificationId,
                    reminderNotificationData.reminderEventIds.toList()
                )
                return false
            }
            if (notificationReminderEventIds.containsAll(reminderEventIds)) {
                Log.d(
                    LogTags.REMINDER,
                    "Notification nID ${reminderNotificationData.notificationId} found, reminder event IDs $reminderEventIds are in covered IDs $notificationReminderEventIds"
                )
                return true
            }
        }
        return false
    }

    private fun getNotificationData(notificationId: Int): ReminderNotificationData? {
        val notificationManager = reminderContext.notificationManager
        for (notification in notificationManager.activeNotifications) {
            if (notification.id == notificationId) {
                return ReminderNotificationData.fromBundle(notification.notification.extras)
            }
        }
        return null
    }
}