package com.futsch1.medtimer

import android.app.NotificationChannel
import android.app.NotificationManager
import com.futsch1.medtimer.reminders.ReminderContext

class ReminderNotificationChannel(
    private val reminderContext: ReminderContext,
    private val importance: Int,
    private val nameId: Int
) {
    val id: String
        get() = notificationChannel.id
    private val notificationManager: NotificationManager = reminderContext.notificationManager
    private var notificationChannel: NotificationChannel =
        getOrCreateChannel()

    init {
        notificationChannel.setBypassDnd(true)
    }

    private fun getOrCreateChannel(): NotificationChannel {
        return notificationManager.getNotificationChannel(importance.toString()) ?: createChannel()
    }

    private fun createChannel(): NotificationChannel {
        val notificationChannel =
            NotificationChannel(importance.toString(), reminderContext.getString(nameId), importance)
        notificationChannel.description = reminderContext.getString(R.string.notification_title)

        notificationManager.createNotificationChannel(notificationChannel)
        return notificationChannel
    }
}