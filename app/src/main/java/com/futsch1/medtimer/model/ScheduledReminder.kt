package com.futsch1.medtimer.model

import com.futsch1.medtimer.database.FullMedicineEntity
import java.time.Instant

data class ScheduledReminder(
    val medicine: FullMedicineEntity,
    val reminder: Reminder,
    val timestamp: Instant
)