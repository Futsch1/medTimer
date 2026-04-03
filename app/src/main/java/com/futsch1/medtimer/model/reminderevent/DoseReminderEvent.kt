package com.futsch1.medtimer.model.reminderevent

import com.futsch1.medtimer.model.Reminder
import java.time.Instant

enum class DoseType {
    TIME_BASED,
    LINKED
}

data class DoseReminderEvent(
    override val reminderEventId: Int,
    override val reminderId: Int,
    override val reminder: Reminder?,
    override val medicineName: String,
    override val amount: String,
    override val color: Int,
    override val useColor: Boolean,
    override val iconId: Int,
    override val tags: List<String>,
    override val status: ReminderEvent.ReminderStatus,
    override val remindedTimestamp: Instant,
    override val processedTimestamp: Instant,
    override val notificationId: Int,
    override val remainingRepeats: Int,
    override val notes: String,
    val stockHandled: Boolean,
    val askForAmount: Boolean,
    val doseType: DoseType
) : ReminderEvent() {
    companion object {
        fun default(): DoseReminderEvent = DoseReminderEvent(
            reminderEventId = 0,
            reminderId = 0,
            reminder = null,
            medicineName = "",
            amount = "",
            color = 0,
            useColor = false,
            iconId = 0,
            tags = emptyList(),
            status = ReminderStatus.RAISED,
            remindedTimestamp = Instant.EPOCH,
            processedTimestamp = Instant.EPOCH,
            notificationId = 0,
            remainingRepeats = 0,
            notes = "",
            stockHandled = false,
            askForAmount = false,
            doseType = DoseType.TIME_BASED
        )
    }
}
