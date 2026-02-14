package com.futsch1.medtimer.reminders.notificationFactory

import android.app.PendingIntent
import com.futsch1.medtimer.reminders.ReminderContext
import com.futsch1.medtimer.reminders.getAcknowledgedActionIntent
import com.futsch1.medtimer.reminders.getRefillActionIntent
import com.futsch1.medtimer.reminders.notificationData.ProcessedNotificationData
import com.futsch1.medtimer.reminders.notificationData.ReminderNotification

class StockIntentBuilder(val reminderContext: ReminderContext, val reminderNotification: ReminderNotification) {
    val processedNotificationData = ProcessedNotificationData.fromReminderNotificationData(reminderNotification.reminderNotificationData)

    val pendingAcknowledged = getAcknowledgedPendingIntent()
    val pendingRefill = getRefillPendingIntent()

    private fun getAcknowledgedPendingIntent(): PendingIntent {
        val notifyAcknowledged = getAcknowledgedActionIntent(reminderContext, processedNotificationData)
        return reminderContext.getPendingIntentBroadcast(
            reminderNotification.reminderNotificationData.notificationId,
            notifyAcknowledged,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun getRefillPendingIntent(): PendingIntent {
        val notifyRefill = getRefillActionIntent(reminderContext, processedNotificationData)
        return reminderContext.getPendingIntentBroadcast(
            reminderNotification.reminderNotificationData.notificationId,
            notifyRefill,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }
}