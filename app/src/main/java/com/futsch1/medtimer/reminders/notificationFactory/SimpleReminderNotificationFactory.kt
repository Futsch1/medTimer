package com.futsch1.medtimer.reminders.notificationFactory

import android.content.Context
import androidx.core.app.NotificationCompat
import com.futsch1.medtimer.reminders.notifications.Notification

class SimpleReminderNotificationFactory(
    context: Context,
    notification: Notification
) : ReminderNotificationFactory(
    context,
    notification
) {
    override fun build() {
        val notificationMessage = notificationStrings.notificationString

        builder.setStyle(NotificationCompat.BigTextStyle().bigText(notificationMessage))
            .setContentText(notificationMessage)

        buildActions()
    }
}