package com.futsch1.medtimer.core.ui

import kotlinx.collections.immutable.ImmutableList
import java.time.LocalDateTime

data class ReminderTableData(
    val rows: ImmutableList<ReminderTableRowData>,
    val columnHeaders: ImmutableList<String>
)

data class ReminderTableRowData(
    val eventId: Int,
    val takenAt: LocalDateTime?,
    // TODO: extract database types into a separate module and use proper type here
    val takenStatus: String,
    val medicineName: String,
    val dosage: String,
    val remindedAt: LocalDateTime,
) {
    val normalizedMedicineName: String = medicineName.lowercase()
}