package com.futsch1.medtimer.reminders

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.util.Log
import com.futsch1.medtimer.LogTags
import com.futsch1.medtimer.preferences.PersistentDataDataSource
import com.futsch1.medtimer.preferences.PreferencesDataSource
import com.futsch1.medtimer.reminders.notificationData.ReminderNotification
import com.futsch1.medtimer.reminders.notificationFactory.BigReminderNotificationFactory
import com.futsch1.medtimer.reminders.notificationFactory.ExpirationDateNotificationFactory
import com.futsch1.medtimer.reminders.notificationFactory.OutOfStockNotificationFactory
import com.futsch1.medtimer.reminders.notificationFactory.SimpleReminderNotificationFactory
import javax.inject.Inject

@SuppressLint("DefaultLocale")
class Notifications @Inject constructor(
    private val notificationSoundManager: NotificationSoundManager,
    private val notificationManager: NotificationManager,
    private val simpleReminderNotificationFactory: SimpleReminderNotificationFactory.Factory,
    private val bigReminderNotificationFactory: BigReminderNotificationFactory.Factory,
    private val outOfStockNotificationFactory: OutOfStockNotificationFactory.Factory,
    private val expirationDateNotificationFactory: ExpirationDateNotificationFactory.Factory,
    private val preferencesDataSource: PreferencesDataSource,
    private val persistentDataDataSource: PersistentDataDataSource
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
            preferencesDataSource.preferences.value.bigNotifications -> bigReminderNotificationFactory.create(reminderNotification)
            else -> simpleReminderNotificationFactory.create(reminderNotification)
        }

        notify(notificationId, factory.create())
        Log.d(LogTags.REMINDER, String.format("Show notification nID %d: %s", notificationId, reminderNotification))

        return notificationId
    }

    private val nextNotificationId: Int
        get() {
            val notificationId = persistentDataDataSource.data.value.notificationId
            persistentDataDataSource.increaseNotificationId()

            return notificationId
        }

    private fun notify(notificationId: Int, notification: android.app.Notification) {
        notificationManager.notify(notificationId, notification)

        notificationSoundManager.restore()
    }
}
