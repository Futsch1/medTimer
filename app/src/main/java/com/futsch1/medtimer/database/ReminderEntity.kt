package com.futsch1.medtimer.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.google.gson.annotations.Expose

@Entity(tableName = "Reminder")
class ReminderEntity(var medicineRelId: Int) {
    @PrimaryKey(autoGenerate = true)
    @Expose
    var reminderId: Int = 0

    @Expose
    var timeInMinutes: Int = DEFAULT_TIME

    @ColumnInfo(defaultValue = "0")
    var createdTimestamp: Long = 0

    @ColumnInfo(defaultValue = "1")
    @Expose
    var consecutiveDays: Int = 1

    @ColumnInfo(defaultValue = "0")
    @Expose
    var pauseDays: Int = 0

    @ColumnInfo(defaultValue = "")
    @Expose
    var instructions: String? = ""

    @ColumnInfo(defaultValue = "19823") // 10.4.24
    @Expose
    var cycleStartDay: Long = 0

    @Expose
    var amount: String = "?"

    @ColumnInfo(defaultValue = "[true, true, true, true, true, true, true]")
    @Expose
    var days: MutableList<Boolean> = mutableListOf(true, true, true, true, true, true, true)

    @ColumnInfo(defaultValue = "true")
    @Expose
    var active: Boolean = true

    @ColumnInfo(defaultValue = "0")
    @Expose
    var periodStart: Long = 0

    @ColumnInfo(defaultValue = "0")
    @Expose
    var periodEnd: Long = 0

    @ColumnInfo(defaultValue = "0xFFFFFFFF")
    @Expose
    var activeDaysOfMonth: Int = -0x1

    @ColumnInfo(defaultValue = "0")
    @Expose
    var linkedReminderId: Int = 0

    @ColumnInfo(defaultValue = "0")
    @Expose
    var intervalStart: Long = 0

    @ColumnInfo(defaultValue = "false")
    @Expose
    var intervalStartsFromProcessed: Boolean = false

    @ColumnInfo(defaultValue = "false")
    @Expose
    var variableAmount: Boolean = false

    @ColumnInfo(defaultValue = "false")
    @Expose
    var automaticallyTaken: Boolean = false

    @Expose
    @ColumnInfo(defaultValue = "480")
    var intervalStartTimeOfDay: Int = 480

    @Expose
    @ColumnInfo(defaultValue = "1320")
    var intervalEndTimeOfDay: Int = 1320

    @Expose
    @ColumnInfo(defaultValue = "false")
    var windowedInterval: Boolean = false

    @Expose
    @ColumnInfo(defaultValue = "0.0")
    var outOfStockThreshold: Double = 0.0

    @Expose
    @ColumnInfo(defaultValue = "OFF")
    var outOfStockReminderType: OutOfStockReminderType = OutOfStockReminderType.OFF

    @Expose
    @ColumnInfo(defaultValue = "OFF")
    var expirationReminderType: ExpirationReminderType = ExpirationReminderType.OFF

    @Ignore
    constructor() : this(0)

    enum class OutOfStockReminderType {
        ONCE,
        ALWAYS,
        DAILY,
        OFF
    }

    enum class ExpirationReminderType {
        ONCE,
        DAILY,
        OFF
    }

    companion object {
        const val DEFAULT_TIME: Int = 480
    }
}
