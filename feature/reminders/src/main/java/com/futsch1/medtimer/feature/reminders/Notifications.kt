package com.futsch1.medtimer.feature.reminders

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.util.Log
import com.futsch1.medtimer.core.common.LogTags
import com.futsch1.medtimer.core.datastore.PersistentDataDataSource
import com.futsch1.medtimer.core.datastore.PreferencesDataSource
import com.futsch1.medtimer.feature.reminders.notificationData.ReminderNotification
import com.futsch1.medtimer.feature.reminders.notificationFactory.BigReminderNotificationFactory
import com.futsch1.medtimer.feature.reminders.notificationFactory.ExpirationDateNotificationFactory
import com.futsch1.medtimer.feature.reminders.notificationFactory.OutOfStockNotificationFactory
import com.futsch1.medtimer.feature.reminders.notificationFactory.SimpleReminderNotificationFactory
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
        get() = persistentDataDataSource.getAndIncreaseNotificationId()

    private fun notify(notificationId: Int, notification: android.app.Notification) {
        notificationManager.notify(notificationId, notification)

        notificationSoundManager.restore()
    }
}
