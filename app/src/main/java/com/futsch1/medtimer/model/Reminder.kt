package com.futsch1.medtimer.model

data class Reminder(
    val id: Int = 0,
    val medicineRelId: Int,
    val timeInMinutes: Int = 480,
    val createdTimestamp: Long = 0,
    val consecutiveDays: Int = 1,
    val pauseDays: Int = 0,
    val instructions: String? = "",
    val cycleStartDay: Long = 0,
    val amount: String = "?",
    val days: List<Boolean> = listOf(true, true, true, true, true, true, true),
    val active: Boolean = true,
    val periodStart: Long = 0,
    val periodEnd: Long = 0,
    val activeDaysOfMonth: Int = -0x1,
    val linkedReminderId: Int = 0,
    val intervalStart: Long = 0,
    val intervalStartsFromProcessed: Boolean = false,
    val variableAmount: Boolean = false,
    val automaticallyTaken: Boolean = false,
    val intervalStartTimeOfDay: Int = 480,
    val intervalEndTimeOfDay: Int = 1320,
    val windowedInterval: Boolean = false,
    val outOfStockThreshold: Double = 0.0,
    val outOfStockReminderType: OutOfStockReminderType = OutOfStockReminderType.OFF,
    val expirationReminderType: ExpirationReminderType = ExpirationReminderType.OFF
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
}
