package com.futsch1.medtimer.feature.reminders

import android.app.NotificationManager
import android.content.Context
import com.futsch1.medtimer.core.domain.model.Medicine
import com.futsch1.medtimer.core.ui.R

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

            createChannel(context, notificationManager, Medicine.ReminderChannel.DEFAULT)
            createChannel(context, notificationManager, Medicine.ReminderChannel.HIGH)
            createChannel(context, notificationManager, Medicine.ReminderChannel.OUT_OF_STOCK)
        }

        fun getNotificationChannel(
            context: Context,
            notificationManager: NotificationManager,
            reminderChannel: Medicine.ReminderChannel
        ): ReminderNotificationChannel {
            return createChannel(context, notificationManager, reminderChannel)
        }

        private fun createChannel(
            context: Context,
            notificationManager: NotificationManager,
            reminderChannel: Medicine.ReminderChannel
        ): ReminderNotificationChannel {
            val nameId = when (reminderChannel) {
                Medicine.ReminderChannel.HIGH -> R.string.high
                Medicine.ReminderChannel.DEFAULT -> R.string.default_
                Medicine.ReminderChannel.OUT_OF_STOCK -> R.string.out_of_stock_reminder
            }
            return ReminderNotificationChannel(
                context,
                notificationManager,
                reminderChannel,
                nameId
            )
        }
    }

}
