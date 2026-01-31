package com.futsch1.medtimer.reminders.notificationFactory

import android.app.PendingIntent
import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.futsch1.medtimer.reminders.getAcknowledgedActionIntent
import com.futsch1.medtimer.reminders.getRefillActionIntent
import com.futsch1.medtimer.reminders.notificationData.ProcessedNotificationData
import com.futsch1.medtimer.reminders.notificationData.ReminderNotification

class StockIntentBuilder(val context: Context, val reminderNotification: ReminderNotification) {
    val defaultSharedPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    val processedNotificationData = ProcessedNotificationData.fromReminderNotificationData(reminderNotification.reminderNotificationData)

    val pendingAcknowledged = getAcknowledgedPendingIntent()
    val pendingRefill = getRefillPendingIntent()

    private fun getAcknowledgedPendingIntent(): PendingIntent {
        val notifyAcknowledged = getAcknowledgedActionIntent(context, processedNotificationData)
        return PendingIntent.getBroadcast(
            context,
            reminderNotification.reminderNotificationData.notificationId,
            notifyAcknowledged,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun getRefillPendingIntent(): PendingIntent {
        val notifyRefill = getRefillActionIntent(context, processedNotificationData)
        return PendingIntent.getBroadcast(
            context,
            reminderNotification.reminderNotificationData.notificationId,
            notifyRefill,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }
}