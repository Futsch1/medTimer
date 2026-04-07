package com.futsch1.medtimer

import android.app.NotificationManager
import android.content.Context
import com.futsch1.medtimer.model.Medicine

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

            createChannel(context, notificationManager, Medicine.NotificationImportance.DEFAULT)
            createChannel(context, notificationManager, Medicine.NotificationImportance.HIGH)
        }

        fun getNotificationChannel(
            context: Context,
            notificationManager: NotificationManager,
            notificationImportance: Medicine.NotificationImportance
        ): ReminderNotificationChannel {
            return createChannel(context, notificationManager, notificationImportance)
        }

        private fun createChannel(
            context: Context,
            notificationManager: NotificationManager,
            notificationImportance: Medicine.NotificationImportance
        ): ReminderNotificationChannel {
            return ReminderNotificationChannel(
                context,
                notificationManager,
                notificationImportance.value,
                if (notificationImportance == Medicine.NotificationImportance.HIGH) R.string.high else R.string.default_
            )
        }
    }

}
