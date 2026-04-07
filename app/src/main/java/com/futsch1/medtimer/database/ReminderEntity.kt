package com.futsch1.medtimer.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.Expose

@Entity(tableName = "Reminder")
class ReminderEntity(
    var medicineRelId: Int = 0,
    @PrimaryKey(autoGenerate = true) @field:Expose var reminderId: Int = 0,
    @field:Expose var timeInMinutes: Int = DEFAULT_TIME,
    @ColumnInfo(defaultValue = "0") var createdTimestamp: Long = 0,
    @ColumnInfo(defaultValue = "1") @field:Expose var consecutiveDays: Int = 1,
    @ColumnInfo(defaultValue = "0") @field:Expose var pauseDays: Int = 0,
    @ColumnInfo(defaultValue = "") @field:Expose var instructions: String? = "",
    @ColumnInfo(defaultValue = "19823") @field:Expose var cycleStartDay: Long = 0,
    @field:Expose var amount: String = "?",
    @ColumnInfo(defaultValue = "[true, true, true, true, true, true, true]") @field:Expose var days: MutableList<Boolean> = mutableListOf(true, true, true, true, true, true, true),
    @ColumnInfo(defaultValue = "true") @field:Expose var active: Boolean = true,
    @ColumnInfo(defaultValue = "0") @field:Expose var periodStart: Long = 0,
    @ColumnInfo(defaultValue = "0") @field:Expose var periodEnd: Long = 0,
    @ColumnInfo(defaultValue = "0xFFFFFFFF") @field:Expose var activeDaysOfMonth: Int = -0x1,
    @ColumnInfo(defaultValue = "0") @field:Expose var linkedReminderId: Int = 0,
    @ColumnInfo(defaultValue = "0") @field:Expose var intervalStart: Long = 0,
    @ColumnInfo(defaultValue = "false") @field:Expose var intervalStartsFromProcessed: Boolean = false,
    @ColumnInfo(defaultValue = "false") @field:Expose var variableAmount: Boolean = false,
    @ColumnInfo(defaultValue = "false") @field:Expose var automaticallyTaken: Boolean = false,
    @field:Expose @ColumnInfo(defaultValue = "480") var intervalStartTimeOfDay: Int = 480,
    @field:Expose @ColumnInfo(defaultValue = "1320") var intervalEndTimeOfDay: Int = 1320,
    @field:Expose @ColumnInfo(defaultValue = "false") var windowedInterval: Boolean = false,
    @field:Expose @ColumnInfo(defaultValue = "0.0") var outOfStockThreshold: Double = 0.0,
    @field:Expose @ColumnInfo(defaultValue = "OFF") var outOfStockReminderType: OutOfStockReminderType = OutOfStockReminderType.OFF,
    @field:Expose @ColumnInfo(defaultValue = "OFF") var expirationReminderType: ExpirationReminderType = ExpirationReminderType.OFF,
) {
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
