package com.futsch1.medtimer.model.reminderevent

import com.futsch1.medtimer.model.Reminder
import java.time.Instant

sealed class ReminderEvent {
    abstract val reminderEventId: Int
    abstract val reminderId: Int
    abstract val reminder: Reminder?
    abstract val medicineName: String
    abstract val amount: String
    abstract val color: Int
    abstract val useColor: Boolean
    abstract val iconId: Int
    abstract val tags: List<String>
    abstract val status: ReminderStatus
    abstract val remindedTimestamp: Instant
    abstract val processedTimestamp: Instant
    abstract val notificationId: Int
    abstract val remainingRepeats: Int
    abstract val notes: String

    enum class ReminderStatus {
        RAISED,
        TAKEN,
        SKIPPED,
        DELETED,
        ACKNOWLEDGED
    }

    companion object {
        val allStatusValues: List<ReminderStatus> = ReminderStatus.entries
        val statusValuesWithoutDelete: List<ReminderStatus> =
            ReminderStatus.entries.filterNot { it == ReminderStatus.DELETED }
        val statusValuesWithoutDeletedAndAcknowledged: List<ReminderStatus> =
            ReminderStatus.entries.filterNot {
                it == ReminderStatus.ACKNOWLEDGED || it == ReminderStatus.DELETED
            }
    }
}
