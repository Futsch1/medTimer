package com.futsch1.medtimer.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.futsch1.medtimer.core.domain.model.ReminderTime

@Entity(tableName = "Reminder")
class ReminderEntity(
    var medicineRelId: Int = 0,
    @PrimaryKey(autoGenerate = true) var reminderId: Int = 0,
    var timeInMinutes: Int = ReminderTime.DEFAULT_TIME,
    @ColumnInfo(defaultValue = "0") var createdTimestamp: Long = 0,
    @ColumnInfo(defaultValue = "1") var consecutiveDays: Int = 1,
    @ColumnInfo(defaultValue = "0") var pauseDays: Int = 0,
    @ColumnInfo(defaultValue = "") var instructions: String? = "",
    @ColumnInfo(defaultValue = "19823") var cycleStartDay: Long = 0,
    var amount: String = "?",
    @ColumnInfo(defaultValue = "[true, true, true, true, true, true, true]") var days: MutableList<Boolean> = mutableListOf(
        true,
        true,
        true,
        true,
        true,
        true,
        true
    ),
    @ColumnInfo(defaultValue = "true") var active: Boolean = true,
    @ColumnInfo(defaultValue = "0") var periodStart: Long = 0,
    @ColumnInfo(defaultValue = "0") var periodEnd: Long = 0,
    @ColumnInfo(defaultValue = "0xFFFFFFFF") var activeDaysOfMonth: Int = -0x1,
    @ColumnInfo(defaultValue = "0") var linkedReminderId: Int = 0,
    @ColumnInfo(defaultValue = "0") var intervalStart: Long = 0,
    @ColumnInfo(defaultValue = "false") var intervalStartsFromProcessed: Boolean = false,
    @ColumnInfo(defaultValue = "false") var variableAmount: Boolean = false,
    @ColumnInfo(defaultValue = "false") var automaticallyTaken: Boolean = false,
    @ColumnInfo(defaultValue = "480") var intervalStartTimeOfDay: Int = 480,
    @ColumnInfo(defaultValue = "1320") var intervalEndTimeOfDay: Int = 1320,
    @ColumnInfo(defaultValue = "false") var windowedInterval: Boolean = false,
    @ColumnInfo(defaultValue = "0.0") var outOfStockThreshold: Double = 0.0,
    @ColumnInfo(defaultValue = "OFF") var outOfStockReminderType: OutOfStockReminderType = OutOfStockReminderType.OFF,
    @ColumnInfo(defaultValue = "OFF") var expirationReminderType: ExpirationReminderType = ExpirationReminderType.OFF,
    @ColumnInfo(defaultValue = "SAME_AS_MEDICINE") var notificationImportance: NotificationImportance = NotificationImportance.SAME_AS_MEDICINE,
) {
    enum class NotificationImportance {
        SAME_AS_MEDICINE,
        DEFAULT,
        HIGH,
        HIGH_AND_ALARM
    }

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
}
