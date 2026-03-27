package com.futsch1.medtimer.reminders

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.util.Log
import com.futsch1.medtimer.LogTags
import com.futsch1.medtimer.reminders.notificationData.ReminderNotification
import com.futsch1.medtimer.reminders.notificationFactory.BigReminderNotificationFactory
import com.futsch1.medtimer.reminders.notificationFactory.ExpirationDateNotificationFactory
import com.futsch1.medtimer.reminders.notificationFactory.OutOfStockNotificationFactory
import com.futsch1.medtimer.reminders.notificationFactory.SimpleReminderNotificationFactory
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
    private val notificationSoundManager: NotificationSoundManager,
    private val notificationManager: NotificationManager,
    private val simpleReminderNotificationFactory: SimpleReminderNotificationFactory.Factory,
    private val bigReminderNotificationFactory: BigReminderNotificationFactory.Factory,
    private val outOfStockNotificationFactory: OutOfStockNotificationFactory.Factory,
    private val expirationDateNotificationFactory: ExpirationDateNotificationFactory.Factory
) {
    fun showNotification(reminderNotification: ReminderNotification, notificationId: Int = -1): Int {
        var notificationId = notificationId
        if (notificationId == -1) {
            notificationId = this.nextNotificationId
        }
        reminderNotification.reminderNotificationData.notificationId = notificationId

        val factory = when {
            reminderNotification.isOutOfStockNotification() -> outOfStockNotificationFactory.create(reminderNotification)
            reminderNotification.isExpirationDateNotification() -> expirationDateNotificationFactory.create(reminderNotification)
            reminderContext.preferencesDataSource.preferences.value.bigNotifications -> bigReminderNotificationFactory.create(reminderNotification)
            else -> simpleReminderNotificationFactory.create(reminderNotification)
        }

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
        notificationManager.notify(notificationId, notification)

        notificationSoundManager.restore()
    }
}
