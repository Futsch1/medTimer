package com.futsch1.medtimer.reminders.scheduling

import com.futsch1.medtimer.database.FullMedicine
import com.futsch1.medtimer.database.Reminder
import java.time.Instant

data class ScheduledReminder(
    val medicine: FullMedicine,
    val reminder: Reminder,
    val timestamp: Instant
)
