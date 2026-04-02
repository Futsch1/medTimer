package com.futsch1.medtimer.reminders.scheduling

import com.futsch1.medtimer.database.FullMedicineEntity
import com.futsch1.medtimer.database.ReminderEntity
import java.time.Instant

data class ScheduledReminder(
    val medicine: FullMedicineEntity,
    val reminder: ReminderEntity,
    val timestamp: Instant
)
