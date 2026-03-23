package com.futsch1.medtimer.reminders

import android.annotation.SuppressLint
import android.util.Log
import com.futsch1.medtimer.LogTags
import com.futsch1.medtimer.reminders.notificationData.ReminderNotification
import com.futsch1.medtimer.reminders.notificationFactory.getReminderNotificationFactory
import javax.inject.Inject

/**
 * Handles the creation and display of system notifications for reminders and stock alerts.
 *
 * This class manages notification IDs to ensure uniqueness across
 * app restarts and coordinates with notification factories to build and dispatch
 * [android.app.Notification] objects.
 *
 * @property context The application context used to access system services and shared preferences.
 */
@SuppressLint("DefaultLocale")
class Notifications @Inject constructor(
    val reminderContext: ReminderContext,
    private val notificationSoundManager: NotificationSoundManager
) {
    fun showNotification(reminderNotification: ReminderNotification, notificationId: Int = -1): Int {
        var notificationId = notificationId
        if (notificationId == -1) {
            notificationId = this.nextNotificationId
        }
        reminderNotification.reminderNotificationData.notificationId = notificationId

        val factory = getReminderNotificationFactory(
            reminderContext,
            reminderNotification
        )

        notify(notificationId, factory.create())
        Log.d(LogTags.REMINDER, String.format("Show notification nID %d: %s", notificationId, reminderNotification))

        return notificationId
    }

    private val nextNotificationId: Int
        get() {
            val notificationId = reminderContext.persistentDataDataSource.data.value.notificationId
            reminderContext.persistentDataDataSource.increaseNotificationId()

            return notificationId
        }

    private fun notify(notificationId: Int, notification: android.app.Notification) {
        reminderContext.notificationManager.notify(notificationId, notification)

        notificationSoundManager.restore()
    }
}
