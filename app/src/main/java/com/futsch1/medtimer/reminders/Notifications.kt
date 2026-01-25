package com.futsch1.medtimer.reminders

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import com.futsch1.medtimer.LogTags
import com.futsch1.medtimer.reminders.notificationData.ReminderNotification
import com.futsch1.medtimer.reminders.notificationFactory.getReminderNotificationFactory

/**
 * Handles the creation and display of system notifications for reminders and stock alerts.
 *
 * This class manages notification IDs using [SharedPreferences] to ensure uniqueness across
 * app restarts and coordinates with notification factories to build and dispatch
 * [android.app.Notification] objects.
 *
 * @property context The application context used to access system services and shared preferences.
 */
@SuppressLint("DefaultLocale")
class Notifications(private val context: Context) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("medtimer.data", Context.MODE_PRIVATE)

    fun showNotification(reminderNotification: ReminderNotification, notificationId: Int = -1): Int {
        var notificationId = notificationId
        if (notificationId == -1) {
            notificationId = this.nextNotificationId
        }
        reminderNotification.reminderNotificationData.notificationId = notificationId

        val factory = getReminderNotificationFactory(
            context,
            reminderNotification
        )

        notify(notificationId, factory.create())
        Log.d(LogTags.REMINDER, String.format("Show notification nID %d: %s", notificationId, reminderNotification))

        return notificationId
    }

    private val nextNotificationId: Int
        get() {
            val notificationId = sharedPreferences.getInt("notificationId", 1)
            sharedPreferences.edit { putInt("notificationId", notificationId + 1) }

            return notificationId
        }

    private fun notify(notificationId: Int, notification: android.app.Notification) {
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        val soundManager = NotificationSoundManager(context)

        notificationManager.notify(notificationId, notification)

        soundManager.restore()
    }
}
