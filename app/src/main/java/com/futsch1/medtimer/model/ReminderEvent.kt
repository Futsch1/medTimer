package com.futsch1.medtimer.model

import java.time.Instant

data class ReminderEvent(
    val reminderEventId: Int,
    val reminderId: Int,
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
    val reminderType: ReminderType,
    val stockHandled: Boolean,
    val askForAmount: Boolean,
    val lastIntervalReminderTimeInMinutes: Int
) {
    enum class ReminderStatus { RAISED, TAKEN, SKIPPED, DELETED, ACKNOWLEDGED }

    companion object {
        val allStatusValues: List<ReminderStatus> = ReminderStatus.entries
        val statusValuesWithoutDelete: List<ReminderStatus> =
            ReminderStatus.entries.filterNot { it == ReminderStatus.DELETED }
        val statusValuesTakenOrSkipped: List<ReminderStatus> =
            ReminderStatus.entries.filter {
                it == ReminderStatus.TAKEN || it == ReminderStatus.SKIPPED
            }

        fun default(): ReminderEvent = ReminderEvent(
            reminderEventId = 0,
            reminderId = 0,
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
            reminderType = ReminderType.TIME_BASED,
            stockHandled = false,
            askForAmount = false,
            lastIntervalReminderTimeInMinutes = 0
        )
    }
}
