package com.futsch1.medtimer.reminders.notificationFactory

import androidx.core.app.NotificationCompat
import com.futsch1.medtimer.reminders.ReminderContext
import com.futsch1.medtimer.reminders.notificationData.ReminderNotification

class SimpleReminderNotificationFactory(
    reminderContext: ReminderContext,
    reminderNotification: ReminderNotification
) : ReminderNotificationFactory(
    reminderContext,
    reminderNotification
) {
    override fun build() {
        val notificationMessage = notificationStrings.notificationString

        builder.setStyle(NotificationCompat.BigTextStyle().bigText(notificationMessage))
            .setContentText(notificationMessage)

        buildActions()
    }
}