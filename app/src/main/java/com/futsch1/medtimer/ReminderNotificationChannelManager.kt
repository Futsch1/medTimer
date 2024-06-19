package com.futsch1.medtimer

import android.app.NotificationManager
import android.content.Context

class ReminderNotificationChannelManager {
    companion object {
        fun initialize(context: Context) {
            // Clean up previous notification settings
            val notificationManager: NotificationManager = context.getSystemService(
                NotificationManager::class.java
            )
            var channelId = 1
            while (true) {
                val channel = notificationManager.getNotificationChannel(
                    "com.futsch1.medTimer.NOTIFICATION$channelId"
                )
                if (channel == null && channelId > 3) {
                    break
                }
                if (channel != null) {
                    notificationManager.deleteNotificationChannel(channel.id)
                }
                channelId++
            }

            createChannel(context, Importance.DEFAULT)
            createChannel(context, Importance.HIGH)
        }

        fun getNotificationChannel(
            context: Context,
            importance: Importance
        ): ReminderNotificationChannel {
            return createChannel(context, importance)
        }

        private fun createChannel(
            context: Context,
            importance: Importance
        ): ReminderNotificationChannel {
            val channel = ReminderNotificationChannel(
                context,
                importance.value,
                if (importance == Importance.HIGH) R.string.high else R.string.default_
            )
            return channel
        }
    }

    enum class Importance(val value: Int) {
        DEFAULT(NotificationManager.IMPORTANCE_DEFAULT),
        HIGH(NotificationManager.IMPORTANCE_HIGH)
    }
}
