package com.futsch1.medtimer.model

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

data class Reminder(
    val id: Int,
    val medicineRelId: Int,
    val time: LocalTime,
    val createdTime: Instant,
    val consecutiveDays: Int,
    val pauseDays: Int,
    val instructions: String?,
    val cycleStartDay: LocalDate,
    val amount: String,
    val days: List<DayOfWeek>,
    val active: Boolean,
    val periodStart: LocalDate,
    val periodEnd: LocalDate,
    val activeDaysOfMonth: List<Int>,
    val linkedReminderId: Int,
    val intervalStart: Instant,
    val intervalStartsFromProcessed: Boolean,
    val variableAmount: Boolean,
    val automaticallyTaken: Boolean,
    val intervalStartTimeOfDay: LocalTime,
    val intervalEndTimeOfDay: LocalTime,
    val windowedInterval: Boolean,
    val outOfStockThreshold: Double,
    val outOfStockReminderType: OutOfStockReminderType,
    val expirationReminderType: ExpirationReminderType
) {
    val reminderType: ReminderType
        get() = when {
            linkedReminderId != 0 -> ReminderType.LINKED
            intervalStart != Instant.EPOCH && !windowedInterval -> ReminderType.CONTINUOUS_INTERVAL
            windowedInterval -> ReminderType.WINDOWED_INTERVAL
            outOfStockReminderType != OutOfStockReminderType.OFF -> ReminderType.OUT_OF_STOCK
            expirationReminderType != ExpirationReminderType.OFF -> ReminderType.EXPIRATION_DATE
            else -> ReminderType.TIME_BASED
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

    val isInterval: Boolean
        get() = this.reminderType == ReminderType.CONTINUOUS_INTERVAL || this.reminderType == ReminderType.WINDOWED_INTERVAL

    val isLinkedOrTimeBased: Boolean
        get() = this.reminderType == ReminderType.LINKED || this.reminderType == ReminderType.TIME_BASED

    val isOutOfStockOrExpirationReminder: Boolean
        get() = this.reminderType == ReminderType.OUT_OF_STOCK || this.reminderType == ReminderType.EXPIRATION_DATE

    val usesTimeInMinutes: Boolean
        get() = isLinkedOrTimeBased || this.reminderType == ReminderType.EXPIRATION_DATE || this.outOfStockReminderType == OutOfStockReminderType.DAILY

    companion object {
        fun default(): Reminder = Reminder(
            id = 0,
            medicineRelId = 0,
            time = LocalTime.of(8, 0),
            createdTime = Instant.EPOCH,
            consecutiveDays = 1,
            pauseDays = 0,
            instructions = "",
            cycleStartDay = LocalDate.EPOCH,
            amount = "?",
            days = emptyList(),
            active = true,
            periodStart = LocalDate.EPOCH,
            periodEnd = LocalDate.EPOCH,
            activeDaysOfMonth = emptyList(),
            linkedReminderId = 0,
            intervalStart = Instant.EPOCH,
            intervalStartsFromProcessed = false,
            variableAmount = false,
            automaticallyTaken = false,
            intervalStartTimeOfDay = LocalTime.of(8, 0),
            intervalEndTimeOfDay = LocalTime.of(23, 0),
            windowedInterval = false,
            outOfStockThreshold = 0.0,
            outOfStockReminderType = OutOfStockReminderType.OFF,
            expirationReminderType = ExpirationReminderType.OFF
        )
    }
}
