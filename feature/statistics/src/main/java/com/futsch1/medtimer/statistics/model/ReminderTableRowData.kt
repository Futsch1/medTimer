package com.futsch1.medtimer.statistics.model

import com.futsch1.medtimer.database.ReminderEvent
import java.time.LocalDateTime

data class ReminderTableRowData(
    val eventId: Int,
    val takenAt: LocalDateTime?,
    // TODO: database types should not be used directly, they should be translated into domain types
    val takenStatus: ReminderEvent.ReminderStatus,
    val medicineName: String,
    val dosage: String,
    val remindedAt: LocalDateTime,
) {
    val normalizedMedicineName: String = medicineName.lowercase()
}