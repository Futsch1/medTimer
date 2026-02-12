package com.futsch1.medtimer.reminders.notificationData

import com.futsch1.medtimer.database.FullMedicine
import com.futsch1.medtimer.database.Reminder
import com.futsch1.medtimer.database.ReminderEvent

class ReminderNotificationPart(val reminder: Reminder, var reminderEvent: ReminderEvent, val medicine: FullMedicine)