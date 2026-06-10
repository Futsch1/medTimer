package com.futsch1.medtimer.feature.reminders

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.futsch1.medtimer.core.domain.model.Medicine
import com.futsch1.medtimer.core.ui.R

class ReminderNotificationChannel(
    private val context: Context,
    private val notificationManager: NotificationManager,
    private val reminderChannel: Medicine.ReminderChannel,
    private val nameId: Int
) {
    val id: String
        get() = notificationChannel.id
    private var notificationChannel: NotificationChannel =
        getOrCreateChannel()

    init {
        notificationChannel.setBypassDnd(true)
    }

    private fun getOrCreateChannel(): NotificationChannel {
        return notificationManager.getNotificationChannel(reminderChannel.channelId) ?: createChannel()
    }

    private fun createChannel(): NotificationChannel {
        val notificationChannel =
            NotificationChannel(reminderChannel.channelId, context.getString(nameId), reminderChannel.importance)
        notificationChannel.description = context.getString(R.string.notification_title)

        notificationManager.createNotificationChannel(notificationChannel)
        return notificationChannel
    }
}
