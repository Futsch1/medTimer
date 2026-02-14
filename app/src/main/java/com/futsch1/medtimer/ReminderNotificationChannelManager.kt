package com.futsch1.medtimer

import android.app.NotificationManager
import com.futsch1.medtimer.reminders.ReminderContext

class ReminderNotificationChannelManager {
    companion object {
        fun initialize(reminderContext: ReminderContext) {
            // Clean up previous notification settings
            val notificationManager: NotificationManager = reminderContext.notificationManager
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

            createChannel(reminderContext, Importance.DEFAULT)
            createChannel(reminderContext, Importance.HIGH)
        }

        fun getNotificationChannel(
            reminderContext: ReminderContext,
            importance: Importance
        ): ReminderNotificationChannel {
            return createChannel(reminderContext, importance)
        }

        private fun createChannel(
            reminderContext: ReminderContext,
            importance: Importance
        ): ReminderNotificationChannel {
            val channel = ReminderNotificationChannel(
                reminderContext,
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
