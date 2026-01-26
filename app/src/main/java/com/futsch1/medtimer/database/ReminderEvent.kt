package com.futsch1.medtimer.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.futsch1.medtimer.database.Reminder.ReminderType
import com.google.gson.annotations.Expose
import java.util.Objects

@Entity(indices = [Index("reminderId"), Index("remindedTimestamp")])
class ReminderEvent {
    @JvmField
    @PrimaryKey(autoGenerate = true)
    var reminderEventId: Int = 0

    @JvmField
    @Expose
    var medicineName: String = ""

    @JvmField
    @Expose
    var amount: String = ""

    @JvmField
    @ColumnInfo(defaultValue = "0")
    @Expose
    var color: Int = 0

    @JvmField
    @ColumnInfo(defaultValue = "false")
    @Expose
    var useColor: Boolean = false

    @JvmField
    @Expose
    var status: ReminderStatus = ReminderStatus.RAISED

    @JvmField
    @Expose
    var remindedTimestamp: Long = 0

    @JvmField
    @Expose
    var processedTimestamp: Long = 0

    @JvmField
    @Expose
    var reminderId: Int = 0

    @JvmField
    @ColumnInfo(defaultValue = "0")
    var notificationId: Int = 0

    @JvmField
    @ColumnInfo(defaultValue = "0")
    @Expose
    var iconId: Int = 0

    @JvmField
    @ColumnInfo(defaultValue = "0")
    var remainingRepeats: Int = 0

    @JvmField
    @ColumnInfo(defaultValue = "false")
    var stockHandled: Boolean = false

    @JvmField
    @ColumnInfo(defaultValue = "false")
    var askForAmount: Boolean = false

    @JvmField
    @ColumnInfo(defaultValue = "[]")
    @Expose
    var tags: List<String> = listOf()

    @JvmField
    @ColumnInfo(defaultValue = "0")
    @Expose
    var lastIntervalReminderTimeInMinutes: Int = 0

    @JvmField
    @ColumnInfo(defaultValue = "")
    @Expose
    var notes: String = ""

    @JvmField
    @ColumnInfo(defaultValue = "TIME_BASED")
    @Expose
    var reminderType: ReminderType = ReminderType.TIME_BASED

    val isOutOfStockOrExpirationReminder: Boolean
        get() = reminderType == ReminderType.OUT_OF_STOCK || reminderType == ReminderType.EXPIRATION_DATE

    override fun equals(other: Any?): Boolean {
        if (other == null || javaClass != other.javaClass) return false
        return membersEqual(other as ReminderEvent)
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

    private fun membersEqual(o: ReminderEvent): Boolean {
        return reminderEventId == o.reminderEventId &&
                medicineName == o.medicineName &&
                amount == o.amount && color == o.color && useColor == o.useColor && status == o.status && remindedTimestamp == o.remindedTimestamp && processedTimestamp == o.processedTimestamp && reminderId == o.reminderId && notificationId == o.notificationId && iconId == o.iconId && remainingRepeats == o.remainingRepeats && stockHandled == o.stockHandled && askForAmount == o.askForAmount &&
                tags == o.tags && lastIntervalReminderTimeInMinutes == o.lastIntervalReminderTimeInMinutes &&
                notes == o.notes && reminderType == o.reminderType
    }

    enum class ReminderStatus {
        RAISED,
        TAKEN,
        SKIPPED,
        DELETED,
        ACKNOWLEDGED
    }

    companion object {
        val allStatusValues: List<ReminderStatus> = ReminderStatus.entries
        val statusValuesWithoutDelete: List<ReminderStatus> = ReminderStatus.entries.filterNot { it == ReminderStatus.DELETED }
        val statusValuesWithoutAcknowledgedAndDeleted: List<ReminderStatus> =
            ReminderStatus.entries.filterNot { it == ReminderStatus.ACKNOWLEDGED || it == ReminderStatus.DELETED }
    }
}
