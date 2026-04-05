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
class ReminderEventEntity {
    @PrimaryKey(autoGenerate = true)
    var reminderEventId: Int = 0

    @Expose
    var medicineName: String = ""

    @Expose
    var amount: String = ""

    @ColumnInfo(defaultValue = "0")
    @Expose
    var color: Int = 0

    @ColumnInfo(defaultValue = "false")
    @Expose
    var useColor: Boolean = false

    @Expose
    var status: ReminderEntityStatus = ReminderEntityStatus.RAISED

    @Expose
    var remindedTimestamp: Long = 0

    @Expose
    var processedTimestamp: Long = 0

    @Expose
    var reminderId: Int = 0

    @ColumnInfo(defaultValue = "0")
    var notificationId: Int = 0

    @ColumnInfo(defaultValue = "0")
    @Expose
    var iconId: Int = 0

    @ColumnInfo(defaultValue = "0")
    var remainingRepeats: Int = 0

    @ColumnInfo(defaultValue = "false")
    var stockHandled: Boolean = false

    @ColumnInfo(defaultValue = "false")
    var askForAmount: Boolean = false

    @ColumnInfo(defaultValue = "[]")
    @Expose
    var tags: List<String> = listOf()

    @ColumnInfo(defaultValue = "0")
    @Expose
    var lastIntervalReminderTimeInMinutes: Int = 0

    @ColumnInfo(defaultValue = "")
    @Expose
    var notes: String = ""

    @ColumnInfo(defaultValue = "TIME_BASED")
    @Expose
    var reminderType: ReminderEntityType = ReminderEntityType.TIME_BASED

    enum class ReminderEntityStatus {
        RAISED,
        TAKEN,
        SKIPPED,
        DELETED,
        ACKNOWLEDGED
    }
}
