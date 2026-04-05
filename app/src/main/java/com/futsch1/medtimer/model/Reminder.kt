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
    val periodStart: Long,
    val periodEnd: Long,
    val activeDaysOfMonth: List<Int>,
    val linkedReminderId: Int,
    val intervalStart: LocalDate,
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
            days = DayOfWeek.entries,
            active = true,
            periodStart = 0,
            periodEnd = 0,
            activeDaysOfMonth = (1..31).toList(),
            linkedReminderId = 0,
            intervalStart = LocalDate.EPOCH,
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
