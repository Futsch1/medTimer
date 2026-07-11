package com.futsch1.medtimer.feature.reminders

import android.app.NotificationManager
import android.util.Log
import com.futsch1.medtimer.core.common.LogTags
import com.futsch1.medtimer.feature.reminders.notificationData.ReminderNotificationData
import javax.inject.Inject

/**
 * Responsible for triggering the display of a medication reminder notification.
 *
 * This class retrieves reminder data from its input, checks if a notification for the
 * specific reminder events is already active to prevent duplicates, and delegates
 * the actual notification scheduling to [AlarmProcessor].
 */
class ShowReminderNotificationProcessor @Inject constructor(
    private val alarmProcessor: AlarmProcessor,
    private val notificationProcessor: NotificationProcessor,
    private val notificationManager: NotificationManager
) {
    suspend fun showReminder(reminderNotificationData: ReminderNotificationData) {
        Log.d(LogTags.REMINDER, "Request show notification for reminder: $reminderNotificationData")

        // Remove pending notifications for those where we want to show the reminder
        synchronizeNotifications(reminderNotificationData)
        alarmProcessor.setSecondaryAlarm(reminderNotificationData)
    }

    private suspend fun synchronizeNotifications(reminderNotificationData: ReminderNotificationData) {
        val notificationDataToEventIds = mutableMapOf<Int, MutableList<Int>>()
        for (reminderEventId in reminderNotificationData.reminderEventIds) {
            val notificationData = getShownNotificationData(reminderEventId)
            if (notificationData != null) {
                Log.d(
                    LogTags.REMINDER,
                    "Notification nID ${notificationData.notificationId} found, but reminder $reminderEventId was rescheduled"
                )
                notificationDataToEventIds.getOrPut(notificationData.notificationId) { mutableListOf() }
                    .add(reminderEventId)
            }
        }

        for ((notificationId, reminderEventIds) in notificationDataToEventIds) {
            notificationProcessor.removeRemindersFromNotification(
                notificationId,
                reminderEventIds
            )
        }
    }

    private fun getShownNotificationData(reminderEventId: Int): ReminderNotificationData? {
        for (notification in notificationManager.activeNotifications) {
            val notificationData = ReminderNotificationData.fromBundle(notification.notification.extras)
            if (notificationData.reminderEventIds.contains(reminderEventId)) {
                return notificationData
            }
        }
        return null
    }
}