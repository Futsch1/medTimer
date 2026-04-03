package com.futsch1.medtimer

import android.app.NotificationManager
import android.content.Context

class ReminderNotificationChannelManager {
    companion object {
        fun initialize(context: Context, notificationManager: NotificationManager) {
            var channelId = 1
            while (true) {
                val channel = notificationManager.getNotificationChannel(
                    "com.futsch1.medtimer.NOTIFICATION$channelId"
                )
                if (channel == null && channelId > 3) {
                    break
                }
                if (channel != null) {
                    notificationManager.deleteNotificationChannel(channel.id)
                }
                channelId++
            }

            createChannel(context, notificationManager, Importance.DEFAULT)
            createChannel(context, notificationManager, Importance.HIGH)
        }

        fun getNotificationChannel(
            context: Context,
            notificationManager: NotificationManager,
            importance: Importance
        ): ReminderNotificationChannel {
            return createChannel(context, notificationManager, importance)
        }

        private fun createChannel(
            context: Context,
            notificationManager: NotificationManager,
            importance: Importance
        ): ReminderNotificationChannel {
            return ReminderNotificationChannel(
                context,
                notificationManager,
                importance.value,
                if (importance == Importance.HIGH) R.string.high else R.string.default_
            )
        }
    }

    enum class Importance(val value: Int) {
        DEFAULT(NotificationManager.IMPORTANCE_DEFAULT),
        HIGH(NotificationManager.IMPORTANCE_HIGH)
    }
}
