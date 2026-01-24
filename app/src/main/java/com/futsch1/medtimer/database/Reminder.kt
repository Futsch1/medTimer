package com.futsch1.medtimer.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.google.gson.annotations.Expose
import java.util.Objects

@Entity
class Reminder(@JvmField var medicineRelId: Int) {
    @JvmField
    @PrimaryKey(autoGenerate = true)
    @Expose
    var reminderId: Int = 0

    @JvmField
    @Expose
    var timeInMinutes: Int = DEFAULT_TIME

    @JvmField
    @ColumnInfo(defaultValue = "0")
    var createdTimestamp: Long = 0

    @JvmField
    @ColumnInfo(defaultValue = "1")
    @Expose
    var consecutiveDays: Int = 1

    @JvmField
    @ColumnInfo(defaultValue = "0")
    @Expose
    var pauseDays: Int = 0

    @JvmField
    @ColumnInfo(defaultValue = "")
    @Expose
    var instructions: String = ""

    @JvmField
    @ColumnInfo(defaultValue = "19823") // 10.4.24
    @Expose
    var cycleStartDay: Long = 0

    @JvmField
    @Expose
    var amount: String = "?"

    @JvmField
    @ColumnInfo(defaultValue = "[true, true, true, true, true, true, true]")
    @Expose
    var days: MutableList<Boolean> = mutableListOf(true, true, true, true, true, true, true)

    @JvmField
    @ColumnInfo(defaultValue = "true")
    @Expose
    var active: Boolean = true

    @JvmField
    @ColumnInfo(defaultValue = "0")
    @Expose
    var periodStart: Long = 0

    @JvmField
    @ColumnInfo(defaultValue = "0")
    @Expose
    var periodEnd: Long = 0

    @JvmField
    @ColumnInfo(defaultValue = "0xFFFFFFFF")
    @Expose
    var activeDaysOfMonth: Int = -0x1

    @JvmField
    @ColumnInfo(defaultValue = "0")
    @Expose
    var linkedReminderId: Int = 0

    @JvmField
    @ColumnInfo(defaultValue = "0")
    @Expose
    var intervalStart: Long = 0

    @JvmField
    @ColumnInfo(defaultValue = "false")
    @Expose
    var intervalStartsFromProcessed: Boolean = false

    @JvmField
    @ColumnInfo(defaultValue = "false")
    @Expose
    var variableAmount: Boolean = false

    @JvmField
    @ColumnInfo(defaultValue = "false")
    @Expose
    var automaticallyTaken: Boolean = false

    @JvmField
    @Expose
    @ColumnInfo(defaultValue = "480")
    var intervalStartTimeOfDay: Int = 480

    @JvmField
    @Expose
    @ColumnInfo(defaultValue = "1320")
    var intervalEndTimeOfDay: Int = 1320

    @JvmField
    @Expose
    @ColumnInfo(defaultValue = "false")
    var windowedInterval: Boolean = false

    @Expose
    @ColumnInfo(defaultValue = "0.0")
    var stockThreshold: Double = 0.0

    @Expose
    @ColumnInfo(defaultValue = "ONCE")
    var stockReminderType: StockReminderType = StockReminderType.ONCE

    @Expose
    @ColumnInfo(defaultValue = "false")
    var isExpirationReminder: Boolean = false

    @Ignore
    constructor() : this(0)

    val isInterval: Boolean
        get() = this.reminderType == ReminderType.CONTINUOUS_INTERVAL || this.reminderType == ReminderType.WINDOWED_INTERVAL

    val isStockOrExpirationReminder: Boolean
        get() = this.reminderType == ReminderType.OUT_OF_STOCK || this.reminderType == ReminderType.EXPIRATION_DATE

    val usesTimeInMinutes: Boolean
        get() = !isInterval || this.reminderType == ReminderType.EXPIRATION_DATE || this.stockReminderType == StockReminderType.DAILY

    val reminderType: ReminderType
        get() {
            return if (linkedReminderId != 0) {
                ReminderType.LINKED
            } else if (intervalStart != 0L && !windowedInterval) {
                ReminderType.CONTINUOUS_INTERVAL
            } else if (windowedInterval) {
                ReminderType.WINDOWED_INTERVAL
            } else if (stockThreshold > 0.0) {
                ReminderType.OUT_OF_STOCK
            } else if (isExpirationReminder) {
                ReminderType.EXPIRATION_DATE
            } else {
                ReminderType.TIME_BASED
            }
        }

    override fun equals(other: Any?): Boolean {
        if (other == null || javaClass != other.javaClass) return false
        return membersEqual(other as Reminder)
    }

    override fun hashCode(): Int {
        return Objects.hash(
            reminderId,
            medicineRelId,
            timeInMinutes,
            createdTimestamp,
            consecutiveDays,
            pauseDays,
            instructions,
            cycleStartDay,
            amount,
            days,
            active,
            periodStart,
            periodEnd,
            activeDaysOfMonth,
            linkedReminderId,
            intervalStart,
            intervalStartsFromProcessed,
            variableAmount,
            automaticallyTaken,
            intervalStartTimeOfDay,
            intervalEndTimeOfDay,
            windowedInterval,
            stockThreshold,
            stockReminderType,
            isExpirationReminder
        )
    }

    private fun membersEqual(that: Reminder): Boolean {
        return reminderId == that.reminderId && medicineRelId == that.medicineRelId && timeInMinutes == that.timeInMinutes && createdTimestamp == that.createdTimestamp && consecutiveDays == that.consecutiveDays && pauseDays == that.pauseDays &&
                instructions == that.instructions && cycleStartDay == that.cycleStartDay &&
                amount == that.amount &&
                days == that.days && active == that.active && periodStart == that.periodStart && periodEnd == that.periodEnd && activeDaysOfMonth == that.activeDaysOfMonth && linkedReminderId == that.linkedReminderId && intervalStart == that.intervalStart && intervalStartsFromProcessed == that.intervalStartsFromProcessed && variableAmount == that.variableAmount && intervalStartTimeOfDay == that.intervalStartTimeOfDay && intervalEndTimeOfDay == that.intervalEndTimeOfDay && windowedInterval == that.windowedInterval
    }

    enum class ReminderType {
        TIME_BASED,
        CONTINUOUS_INTERVAL,
        LINKED,
        WINDOWED_INTERVAL,
        OUT_OF_STOCK,
        EXPIRATION_DATE
    }

    enum class StockReminderType {
        ONCE,
        ALWAYS,
        DAILY
    }

    companion object {
        const val DEFAULT_TIME: Int = 480
    }
}
