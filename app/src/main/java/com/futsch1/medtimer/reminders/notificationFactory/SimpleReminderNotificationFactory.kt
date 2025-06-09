package com.futsch1.medtimer.reminders.notificationFactory

import android.content.Context
import androidx.core.app.NotificationCompat
import com.futsch1.medtimer.database.FullMedicine
import com.futsch1.medtimer.database.Reminder
import com.futsch1.medtimer.database.ReminderEvent

class SimpleReminderNotificationFactory(
    context: Context,
    notificationId: Int,
    remindTime: String,
    medicine: FullMedicine,
    reminder: Reminder,
    reminderEvent: ReminderEvent,
    hasSameTimeReminders: Boolean
) : ReminderNotificationFactory(
    context,
    notificationId,
    remindTime,
    medicine,
    reminder,
    reminderEvent,
    hasSameTimeReminders
) {
    override fun build() {
        val notificationMessage = getNotificationString()

        builder.setStyle(NotificationCompat.BigTextStyle().bigText(notificationMessage))
            .setContentText(notificationMessage)

        buildActions()
    }
}