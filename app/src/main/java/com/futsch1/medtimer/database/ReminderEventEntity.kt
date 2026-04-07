package com.futsch1.medtimer.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.futsch1.medtimer.database.ReminderEventEntity.ReminderEntityStatus
import com.google.gson.annotations.Expose

val allStatusValues: List<ReminderEntityStatus> = ReminderEntityStatus.entries
val statusValuesWithoutDelete: List<ReminderEntityStatus> = ReminderEntityStatus.entries.filterNot { it == ReminderEntityStatus.DELETED }
val statusValuesWithoutDeletedAndAcknowledged: List<ReminderEntityStatus> =
    ReminderEntityStatus.entries.filterNot { it == ReminderEntityStatus.ACKNOWLEDGED || it == ReminderEntityStatus.DELETED }


@Entity(tableName = "ReminderEvent", indices = [Index("reminderId"), Index("remindedTimestamp")])
class ReminderEventEntity(
    @PrimaryKey(autoGenerate = true) var reminderEventId: Int = 0,
    @field:Expose var medicineName: String = "",
    @field:Expose var amount: String = "",
    @ColumnInfo(defaultValue = "0") @field:Expose var color: Int = 0,
    @ColumnInfo(defaultValue = "false") @field:Expose var useColor: Boolean = false,
    @field:Expose var status: ReminderEntityStatus = ReminderEntityStatus.RAISED,
    @field:Expose var remindedTimestamp: Long = 0,
    @field:Expose var processedTimestamp: Long = 0,
    @field:Expose var reminderId: Int = 0,
    @ColumnInfo(defaultValue = "0") var notificationId: Int = 0,
    @ColumnInfo(defaultValue = "0") @field:Expose var iconId: Int = 0,
    @ColumnInfo(defaultValue = "0") var remainingRepeats: Int = 0,
    @ColumnInfo(defaultValue = "false") var stockHandled: Boolean = false,
    @ColumnInfo(defaultValue = "false") var askForAmount: Boolean = false,
    @ColumnInfo(defaultValue = "[]") @field:Expose var tags: List<String> = listOf(),
    @ColumnInfo(defaultValue = "0") @field:Expose var lastIntervalReminderTimeInMinutes: Int = 0,
    @ColumnInfo(defaultValue = "") @field:Expose var notes: String = "",
    @ColumnInfo(defaultValue = "TIME_BASED") @field:Expose var reminderType: ReminderEntityType = ReminderEntityType.TIME_BASED,
) {
    enum class ReminderEntityStatus {
        RAISED,
        TAKEN,
        SKIPPED,
        DELETED,
        ACKNOWLEDGED
    }
}
