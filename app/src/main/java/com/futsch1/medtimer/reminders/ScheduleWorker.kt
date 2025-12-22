package com.futsch1.medtimer.reminders

import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.work.WorkerParameters
import com.futsch1.medtimer.LogTags
import com.futsch1.medtimer.reminders.notificationData.ReminderNotificationData
import com.futsch1.medtimer.reminders.notificationData.ReminderNotificationData.Companion.fromInputData

class ScheduleWorker(context: Context, workerParams: WorkerParameters) : RescheduleWorker(context, workerParams) {
    override fun doWork(): Result {
        val inputData = getInputData()

        val reminderNotificationData = fromInputData(inputData)
        Log.d(LogTags.REMINDER, "Scheduling reminder: $reminderNotificationData")

        // Check if given notification ID is already active
        if (!isNotificationActive(reminderNotificationData)) {
            enqueueNotification(reminderNotificationData)
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
                    if (reminderEventIds.sorted() == notificationReminderEventIds.sorted()) {
                        Log.d(
                            LogTags.REMINDER,
                            String.format("Notification nID %d already active, do not schedule again", reminderNotificationData.notificationId)
                        )
                        return true
                    }
                }
            }
        }
        return false
    }
}