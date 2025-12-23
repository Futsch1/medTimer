package com.futsch1.medtimer.reminders

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import com.futsch1.medtimer.R
import com.futsch1.medtimer.reminders.notificationData.ReminderNotificationData

class RemoteInputReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val results = RemoteInput.getResultsFromIntent(intent)
        if (results != null) {
            val snoozeTime = results.getCharSequence("snooze_time")
            val reminderNotificationData = ReminderNotificationData.fromBundle(intent.extras!!)
            val snoozeTimeInt = snoozeTime.toString().toIntOrNull() ?: 10
            confirmNotification(context, reminderNotificationData.notificationId)
            ReminderProcessor.requestSnooze(context, reminderNotificationData, snoozeTimeInt)
        }
    }

    private fun confirmNotification(context: Context, notificationId: Int) {
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.cancel(notificationId)
        for (notification in notificationManager.activeNotifications) {
            if (notification.id == notificationId) {
                val builder = NotificationCompat.Builder(context, notification.notification.channelId)
                builder.setSmallIcon(notification.notification.smallIcon.resId).setContentInfo(context.getString(R.id.snoozeButton))
                builder.setTimeoutAfter(2000)
                notificationManager.notify(notificationId, builder.build())
            }
        }
    }
}