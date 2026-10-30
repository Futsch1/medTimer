package com.futsch1.medtimer.feature.ui.medicine

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.futsch1.medtimer.core.domain.model.ReminderType
import com.futsch1.medtimer.core.ui.IntervalUnit
import com.futsch1.medtimer.core.ui.TimeFormatter
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

enum class NewReminderStockReminderType { ONCE, ALWAYS, DAILY }
enum class NewReminderExpirationReminderType { ONCE, DAILY }

data class NewReminderDraft(
    val selectedType: ReminderType? = null,
    val amount: String = "",
    val time: LocalTime = LocalTime.of(8, 0),
    val interval: String = "12",
    val intervalUnit: IntervalUnit = IntervalUnit.HOURS,
    val intervalStartsFromProcessed: Boolean = false,
    val intervalStart: LocalDateTime = LocalDateTime.of(LocalDate.now(), LocalTime.of(8, 0)),
    val dailyStart: LocalTime = LocalTime.of(8, 0),
    val dailyEnd: LocalTime = LocalTime.of(23, 0),
    val threshold: String = "",
    val stockReminderType: NewReminderStockReminderType = NewReminderStockReminderType.ONCE,
    val expirationDays: String = "1",
    val expirationReminderType: NewReminderExpirationReminderType = NewReminderExpirationReminderType.ONCE,
)

interface NewReminderState {
    val draft: NewReminderDraft
    val invalid: Boolean
    val created: Boolean
    val timeText: String
    val intervalStartText: String
    val dailyStartText: String
    val dailyEndText: String
}

class MutableNewReminderState(private val timeFormatter: TimeFormatter) : NewReminderState {
    override var draft by mutableStateOf(NewReminderDraft())
    override var invalid by mutableStateOf(false)
    override var created by mutableStateOf(false)

    override val timeText: String
        get() = timeFormatter.minutesToTimeString(draft.time.toMinuteOfDay())

    override val intervalStartText: String
        get() = listOf(
            timeFormatter.localDateToString(draft.intervalStart.toLocalDate()),
            timeFormatter.minutesToTimeString(draft.intervalStart.toLocalTime().toMinuteOfDay()),
        ).joinToString(" | ")

    override val dailyStartText: String
        get() = timeFormatter.minutesToTimeString(draft.dailyStart.toMinuteOfDay())

    override val dailyEndText: String
        get() = timeFormatter.minutesToTimeString(draft.dailyEnd.toMinuteOfDay())

    private fun LocalTime.toMinuteOfDay() = hour * 60 + minute
}

class PreviewNewReminderState : NewReminderState {
    override var draft by mutableStateOf(NewReminderDraft())
    override var invalid by mutableStateOf(false)
    override val created by mutableStateOf(false)

    override val timeText by mutableStateOf("8:00")

    override val intervalStartText by mutableStateOf("30.10.2026 9:00")

    override val dailyStartText by mutableStateOf("9:00")

    override val dailyEndText by mutableStateOf("23:00")
}