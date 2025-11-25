package com.futsch1.medtimer.reminders.notificationFactory

import android.content.Context
import androidx.core.app.NotificationCompat
import com.futsch1.medtimer.reminders.notificationData.ReminderNotification

class SimpleReminderNotificationFactory(
    context: Context,
    reminderNotification: ReminderNotification
) : ReminderNotificationFactory(
    context,
    reminderNotification
) {
    override fun build() {
        val notificationMessage = notificationStrings.notificationString

        builder.setStyle(NotificationCompat.BigTextStyle().bigText(notificationMessage))
            .setContentText(notificationMessage)

        buildActions()
    }
}