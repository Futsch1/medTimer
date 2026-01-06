package com.futsch1.medtimer.reminders

import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.futsch1.medtimer.LogTags
import com.futsch1.medtimer.reminders.notificationData.ReminderNotificationData
import com.futsch1.medtimer.reminders.notificationData.ReminderNotificationData.Companion.fromInputData

class ShowReminderNotificationWorker(val context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {
    val alarmSetter = SetAlarmForReminderNotification(context)

    override fun doWork(): Result {
        val reminderNotificationData = fromInputData(inputData)
        Log.d(LogTags.REMINDER, "Scheduling reminder: $reminderNotificationData")

        // Check if given notification ID is already active
        if (!isNotificationActive(reminderNotificationData)) {
            alarmSetter.setAlarmForReminderNotification(reminderNotificationData, inputData)
        }

        return Result.success()
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