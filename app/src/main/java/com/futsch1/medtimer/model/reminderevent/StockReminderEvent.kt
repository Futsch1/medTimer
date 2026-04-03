package com.futsch1.medtimer.model.reminderevent

import com.futsch1.medtimer.model.Reminder
import java.time.Instant

enum class StockReminderType {
    OUT_OF_STOCK,
    EXPIRATION_DATE,
    REFILL
}

data class StockReminderEvent(
    override val reminderEventId: Int,
    override val reminderId: Int,
    override val reminder: Reminder?,
    override val medicineName: String,
    override val amount: String,
    override val color: Int,
    override val useColor: Boolean,
    override val iconId: Int,
    override val tags: List<String>,
    override val status: ReminderStatus,
    override val remindedTimestamp: Instant,
    override val processedTimestamp: Instant,
    override val notificationId: Int,
    override val remainingRepeats: Int,
    override val notes: String,
    val reminderType: StockReminderType
) : ReminderEvent() {
    companion object {
        fun default(): StockReminderEvent = StockReminderEvent(
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
            reminderType = StockReminderType.OUT_OF_STOCK
        )
    }
}
