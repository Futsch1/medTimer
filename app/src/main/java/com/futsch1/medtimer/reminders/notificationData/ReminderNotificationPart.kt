package com.futsch1.medtimer.reminders.notificationData

import com.futsch1.medtimer.core.domain.model.Medicine
import com.futsch1.medtimer.core.domain.model.Reminder
import com.futsch1.medtimer.core.domain.model.ReminderEvent

data class ReminderNotificationPart(val reminder: Reminder, val reminderEvent: ReminderEvent, val medicine: Medicine)