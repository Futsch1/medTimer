package com.futsch1.medtimer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.net.Uri

class ReminderNotificationChannel(
    context: Context,
    private val sound: Uri?,
    private val importance: Int
) {
    val id: String
        get() = notificationChannel!!.id
    private val notificationManager: NotificationManager = context.getSystemService(
        NotificationManager::class.java
    )
    private var notificationChannel: NotificationChannel? =
        notificationManager.getNotificationChannel(importance.toString())

    init {
        if (requiresCreation()) {
            createChannel()
        }
    }

    private fun createChannel() {
        if (notificationChannel != null) {
            notificationManager.deleteNotificationChannel(notificationChannel!!.id)
        }

        notificationChannel = NotificationChannel(importance.toString(), "MedTimer", importance)
        notificationChannel!!.description = "Notifications from MedTimer"
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .build()
        notificationChannel!!.setSound(sound, audioAttributes)

        notificationManager.createNotificationChannel(notificationChannel!!)
    }

    private fun requiresCreation(): Boolean {
        val channelSound: Uri? = notificationChannel?.sound
        return channelSound == null || channelSound != sound
    }
}