package com.futsch1.medtimer.feature.ui.overview.model

import com.futsch1.medtimer.core.domain.model.ReminderType
import java.time.Duration
import java.time.Instant
import java.time.LocalDate

/**
 * Everything the overview card shows about one event, kept as typed values so the composable owns
 * all locale-aware formatting.
 *
 * [time] is the reminded or scheduled time. [takenTime] is the processed time, shown after an arrow
 * when the event was taken and the user opted to see taken times. [interval] is the elapsed time
 * from the last interval reminder to when the dose was taken. [stock] is the stock level around the
 * dose, either recorded (past events) or projected (simulated ones). [expirationDate] is only set
 * for expiration reminders.
 */
data class OverviewEventContent(
    val reminderType: ReminderType,
    val time: Instant,
    val medicineName: String,
    val dose: String,
    val takenTime: Instant? = null,
    val interval: Duration? = null,
    val stock: StockChange? = null,
    val expirationDate: LocalDate? = null,
    val useRelativeTime: Boolean = false,
)

data class StockChange(val before: Double, val after: Double, val unit: String)
