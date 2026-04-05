package com.futsch1.medtimer.reminders.notificationData

import com.futsch1.medtimer.database.FullMedicineEntity
import com.futsch1.medtimer.database.ReminderEntity
import com.futsch1.medtimer.model.ReminderEvent

data class ReminderNotificationPart(val reminder: ReminderEntity, val reminderEvent: ReminderEvent, val medicine: FullMedicineEntity)