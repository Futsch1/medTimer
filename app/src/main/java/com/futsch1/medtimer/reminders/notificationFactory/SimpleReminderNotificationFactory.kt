package com.futsch1.medtimer.reminders.notificationFactory

import android.app.NotificationManager
import androidx.core.app.NotificationCompat
import com.futsch1.medtimer.reminders.ReminderContext
import com.futsch1.medtimer.reminders.notificationData.ReminderNotification

class SimpleReminderNotificationFactory(
    reminderContext: ReminderContext,
    reminderNotification: ReminderNotification,
    notificationManager: NotificationManager
) : ReminderNotificationFactory(
    reminderContext,
    reminderNotification,
    notificationManager
) {
    override fun build() {
        val notificationMessage = notificationStrings.notificationString

        builder.setStyle(NotificationCompat.BigTextStyle().bigText(notificationMessage))
        builder.setContentText(notificationMessage)

        buildActions()
    }
}