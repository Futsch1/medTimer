package com.futsch1.medtimer.model.reminderevent

import com.futsch1.medtimer.model.Reminder
import java.time.Instant

enum class ReminderEventType {
    TIME_BASED, LINKED, CONTINUOUS_INTERVAL, WINDOWED_INTERVAL, OUT_OF_STOCK, EXPIRATION_DATE, REFILL
}

data class ReminderEvent(
    val reminderEventId: Int,
    val reminderId: Int,
    val reminder: Reminder?,
    val medicineName: String,
    val amount: String,
    val color: Int,
    val useColor: Boolean,
    val iconId: Int,
    val tags: List<String>,
    val status: ReminderStatus,
    val remindedTimestamp: Instant,
    val processedTimestamp: Instant,
    val notificationId: Int,
    val remainingRepeats: Int,
    val notes: String,
    val reminderType: ReminderEventType,
    val stockHandled: Boolean,
    val askForAmount: Boolean,
    val lastIntervalReminderTimeInMinutes: Int
) {
    enum class ReminderStatus { RAISED, TAKEN, SKIPPED, DELETED, ACKNOWLEDGED }

    companion object {
        val allStatusValues: List<ReminderStatus> = ReminderStatus.entries
        val statusValuesWithoutDelete: List<ReminderStatus> =
            ReminderStatus.entries.filterNot { it == ReminderStatus.DELETED }
        val statusValuesWithoutDeletedAndAcknowledged: List<ReminderStatus> =
            ReminderStatus.entries.filterNot {
                it == ReminderStatus.ACKNOWLEDGED || it == ReminderStatus.DELETED
            }

        fun default(): ReminderEvent = ReminderEvent(
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
            reminderType = ReminderEventType.TIME_BASED,
            stockHandled = false,
            askForAmount = false,
            lastIntervalReminderTimeInMinutes = 0
        )
    }
}
