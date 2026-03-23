package com.futsch1.medtimer

import android.app.NotificationManager
import com.futsch1.medtimer.reminders.ReminderContext

class ReminderNotificationChannelManager {
    companion object {
        fun initialize(reminderContext: ReminderContext, notificationManager: NotificationManager) {
            // Clean up previous notification settings
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

            createChannel(reminderContext, notificationManager, Importance.DEFAULT)
            createChannel(reminderContext, notificationManager, Importance.HIGH)
        }

        fun getNotificationChannel(
            reminderContext: ReminderContext,
            notificationManager: NotificationManager,
            importance: Importance
        ): ReminderNotificationChannel {
            return createChannel(reminderContext, notificationManager, importance)
        }

        private fun createChannel(
            reminderContext: ReminderContext,
            notificationManager: NotificationManager,
            importance: Importance
        ): ReminderNotificationChannel {
            val channel = ReminderNotificationChannel(
                reminderContext,
                notificationManager,
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
