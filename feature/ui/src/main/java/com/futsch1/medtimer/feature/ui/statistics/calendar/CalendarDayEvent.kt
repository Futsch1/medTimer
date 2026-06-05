package com.futsch1.medtimer.feature.ui.statistics.calendar

import com.futsch1.medtimer.core.domain.model.ReminderType
import java.time.Duration
import java.time.LocalDateTime

// Structured calendar event consumed by the Compose calendar (DayEventsCard). The standalone XML
// CalendarFragment still uses the Spanned-based flow on CalendarEventsViewModel for its icon rendering.
//
// [time] is the reminded time (always shown). [takenTime] is the processed time, shown after an arrow
// when the event was taken and the user opted to see taken times. [interval] is the elapsed time from
// the last interval reminder to when the dose was taken; both are kept as structured values so the
// composable owns all locale-aware formatting.
data class CalendarDayEvent(
    val time: LocalDateTime,
    val amount: String,
    val medicineName: String,
    val status: Status,
    val reminderType: ReminderType = ReminderType.TIME_BASED,
    val takenTime: LocalDateTime? = null,
    val interval: Duration? = null,
) {
    enum class Status { TAKEN, SKIPPED, RAISED, SCHEDULED }
}
