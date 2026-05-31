package com.futsch1.medtimer.feature.ui.statistics.calendar

import java.time.LocalDateTime

// Structured calendar event consumed by the Compose calendar (DayEventsCard). The standalone XML
// CalendarFragment still uses the Spanned-based flow on CalendarEventsViewModel for its icon rendering.
data class CalendarDayEvent(
    val time: LocalDateTime,
    val amount: String,
    val medicineName: String,
    val status: Status,
) {
    enum class Status { TAKEN, SKIPPED, RAISED, SCHEDULED }
}
