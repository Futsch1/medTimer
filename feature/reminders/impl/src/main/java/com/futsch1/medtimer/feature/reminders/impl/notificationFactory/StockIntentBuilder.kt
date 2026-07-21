package com.futsch1.medtimer.feature.reminders.impl.notificationFactory

import android.app.PendingIntent
import android.content.Context
import com.futsch1.medtimer.feature.reminders.impl.getAcknowledgedActionIntent
import com.futsch1.medtimer.feature.reminders.impl.getRefillActionIntent
import com.futsch1.medtimer.feature.reminders.impl.notificationData.ReminderNotification

class StockIntentBuilder(private val context: Context, private val reminderNotification: ReminderNotification) {
    private val reminderEventIds = reminderNotification.reminderNotificationData.reminderEventIds.toList()

    val pendingAcknowledged = getAcknowledgedPendingIntent()
    val pendingRefill = getRefillPendingIntent()

    private fun getAcknowledgedPendingIntent(): PendingIntent {
        val notifyAcknowledged = getAcknowledgedActionIntent(context, reminderEventIds)
        return PendingIntent.getBroadcast(
            context,
            reminderNotification.reminderNotificationData.notificationId,
            notifyAcknowledged,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun getRefillPendingIntent(): PendingIntent {
        val notifyRefill = getRefillActionIntent(context, reminderEventIds)
        return PendingIntent.getBroadcast(
            context,
            reminderNotification.reminderNotificationData.notificationId,
            notifyRefill,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }
}
