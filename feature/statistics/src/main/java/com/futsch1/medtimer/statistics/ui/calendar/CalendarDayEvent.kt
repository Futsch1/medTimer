package com.futsch1.medtimer.statistics.ui.calendar

import java.time.LocalDateTime

data class CalendarDayEvent(
    val time: LocalDateTime,
    val amount: String,
    val medicineName: String,
    val status: Status,
) {
    enum class Status { TAKEN, SKIPPED, RAISED, SCHEDULED }
}
