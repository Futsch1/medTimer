package com.futsch1.medtimer.reminders.notificationFactory

import android.content.Context
import androidx.core.app.NotificationCompat
import com.futsch1.medtimer.reminders.notificationData.ReminderNotificationData

class SimpleReminderNotificationFactory(
    context: Context,
    reminderNotificationData: ReminderNotificationData
) : ReminderNotificationFactory(
    context,
    reminderNotificationData
) {
    override fun build() {
        val notificationMessage = notificationStrings.notificationString

        builder.setStyle(NotificationCompat.BigTextStyle().bigText(notificationMessage))
            .setContentText(notificationMessage)

        buildActions()
    }
}