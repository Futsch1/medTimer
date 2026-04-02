package com.futsch1.medtimer.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.futsch1.medtimer.database.ReminderEntity.ReminderType
import com.futsch1.medtimer.database.ReminderEventEntity.ReminderStatus
import com.google.gson.annotations.Expose
import java.util.Objects

val allStatusValues: List<ReminderStatus> = ReminderStatus.entries
val statusValuesWithoutDelete: List<ReminderStatus> = ReminderStatus.entries.filterNot { it == ReminderStatus.DELETED }
val statusValuesWithoutDeletedAndAcknowledged: List<ReminderStatus> =
    ReminderStatus.entries.filterNot { it == ReminderStatus.ACKNOWLEDGED || it == ReminderStatus.DELETED }

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
    var status: ReminderStatus = ReminderStatus.RAISED

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
    var reminderType: ReminderType = ReminderType.TIME_BASED

    val isOutOfStockOrExpirationOrRefillReminder: Boolean
        get() = reminderType == ReminderType.OUT_OF_STOCK || reminderType == ReminderType.EXPIRATION_DATE || reminderType == ReminderType.REFILL

    override fun equals(other: Any?): Boolean {
        if (other !is ReminderEventEntity) return false
        return membersEqual(other)
    }

    override fun hashCode(): Int {
        return Objects.hash(
            reminderEventId,
            medicineName,
            amount,
            color,
            useColor,
            status,
            remindedTimestamp,
            processedTimestamp,
            reminderId,
            notificationId,
            iconId,
            remainingRepeats,
            stockHandled,
            askForAmount,
            tags,
            lastIntervalReminderTimeInMinutes,
            notes,
            reminderType
        )
    }

    private fun membersEqual(other: ReminderEventEntity): Boolean {
        return reminderEventId == other.reminderEventId &&
                medicineName == other.medicineName &&
                amount == other.amount && color == other.color && useColor == other.useColor && status == other.status && remindedTimestamp == other.remindedTimestamp && processedTimestamp == other.processedTimestamp && reminderId == other.reminderId && notificationId == other.notificationId && iconId == other.iconId && remainingRepeats == other.remainingRepeats && stockHandled == other.stockHandled && askForAmount == other.askForAmount &&
                tags == other.tags && lastIntervalReminderTimeInMinutes == other.lastIntervalReminderTimeInMinutes &&
                notes == other.notes && reminderType == other.reminderType
    }

    enum class ReminderStatus {
        RAISED,
        TAKEN,
        SKIPPED,
        DELETED,
        ACKNOWLEDGED
    }
}
