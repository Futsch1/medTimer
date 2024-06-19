package com.futsch1.medtimer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

class ReminderNotificationChannel(
    private val context: Context,
    private val importance: Int,
    private val nameId: Int
) {
    val id: String
        get() = notificationChannel.id
    private val notificationManager: NotificationManager = context.getSystemService(
        NotificationManager::class.java
    )
    private var notificationChannel: NotificationChannel =
        getOrCreateChannel()

    private fun getOrCreateChannel(): NotificationChannel {
        return notificationManager.getNotificationChannel(importance.toString()) ?: createChannel()
    }

    private fun createChannel(): NotificationChannel {
        val notificationChannel =
            NotificationChannel(importance.toString(), context.getString(nameId), importance)
        notificationChannel.description = context.getString(R.string.notification_title)

        notificationManager.createNotificationChannel(notificationChannel)
        return notificationChannel
    }
}