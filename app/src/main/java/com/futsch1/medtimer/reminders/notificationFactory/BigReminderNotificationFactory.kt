package com.futsch1.medtimer.reminders.notificationFactory

import android.content.Context
import com.futsch1.medtimer.database.FullMedicine
import com.futsch1.medtimer.database.Reminder
import com.futsch1.medtimer.database.ReminderEvent

class BigReminderNotificationFactory(
    context: Context,
    notificationId: Int,
    remindTime: String,
    medicine: FullMedicine,
    reminder: Reminder,
    reminderEvent: ReminderEvent
) : ReminderNotificationFactory(
    context,
    notificationId,
    remindTime,
    medicine,
    reminder,
    reminderEvent
) {
}