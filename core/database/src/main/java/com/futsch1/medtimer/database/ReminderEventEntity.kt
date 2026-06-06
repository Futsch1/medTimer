package com.futsch1.medtimer.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.futsch1.medtimer.database.ReminderEventEntity.ReminderEntityStatus

val allStatusValues: List<ReminderEntityStatus> = ReminderEntityStatus.entries
val statusValuesWithoutDelete: List<ReminderEntityStatus> = ReminderEntityStatus.entries.filterNot { it == ReminderEntityStatus.DELETED }
val statusValuesWithoutDeletedAndAcknowledged: List<ReminderEntityStatus> =
    ReminderEntityStatus.entries.filterNot { it == ReminderEntityStatus.ACKNOWLEDGED || it == ReminderEntityStatus.DELETED }


@Entity(tableName = "ReminderEvent", indices = [Index("reminderId"), Index("remindedTimestamp")])
class ReminderEventEntity(
    @PrimaryKey(autoGenerate = true) var reminderEventId: Int = 0,
    var medicineName: String = "",
    var amount: String = "",
    @ColumnInfo(defaultValue = "0") var color: Int = 0,
    @ColumnInfo(defaultValue = "false") var useColor: Boolean = false,
    var status: ReminderEntityStatus = ReminderEntityStatus.RAISED,
    var remindedTimestamp: Long = 0,
    var processedTimestamp: Long = 0,
    var reminderId: Int = 0,
    @ColumnInfo(defaultValue = "0") var notificationId: Int = 0,
    @ColumnInfo(defaultValue = "0") var iconId: Int = 0,
    @ColumnInfo(defaultValue = "0") var remainingRepeats: Int = 0,
    @ColumnInfo(defaultValue = "false") var stockHandled: Boolean = false,
    @ColumnInfo(defaultValue = "false") var askForAmount: Boolean = false,
    @ColumnInfo(defaultValue = "[]") var tags: List<String> = listOf(),
    @ColumnInfo(defaultValue = "0") var lastIntervalReminderTimeInMinutes: Int = 0,
    @ColumnInfo(defaultValue = "") var notes: String = "",
    @ColumnInfo(defaultValue = "TIME_BASED") var reminderType: ReminderEntityType = ReminderEntityType.TIME_BASED,
    @ColumnInfo(defaultValue = "-1.0") var stockBefore: Double = -1.0,
    @ColumnInfo(defaultValue = "-1.0") var stockAfter: Double = -1.0,
    @ColumnInfo(defaultValue = "") var stockUnit: String = "",
) {
    enum class ReminderEntityStatus {
        RAISED,
        TAKEN,
        SKIPPED,
        DELETED,
        ACKNOWLEDGED
    }
}
