package com.futsch1.medtimer.core.domain.backup

import com.futsch1.medtimer.core.domain.model.Reminder

class ReminderBackup(
    var reminderId: Int = 0,
    var timeInMinutes: Int = 0,
    var consecutiveDays: Int = 1,
    var pauseDays: Int = 0,
    var instructions: String? = "",
    var cycleStartDay: Long = 0,
    var amount: String = "?",
    var days: MutableList<Boolean> = mutableListOf(true, true, true, true, true, true, true),
    var active: Boolean = true,
    var periodStart: Long = 0,
    var periodEnd: Long = 0,
    var activeDaysOfMonth: Int = -1,
    var linkedReminderId: Int = 0,
    var intervalStart: Long = 0,
    var intervalStartsFromProcessed: Boolean = false,
    var variableAmount: Boolean = false,
    var automaticallyTaken: Boolean = false,
    var intervalStartTimeOfDay: Int = 480,
    var intervalEndTimeOfDay: Int = 1320,
    var windowedInterval: Boolean = false,
    var outOfStockThreshold: Double = 0.0,
    var outOfStockReminderType: Reminder.OutOfStockReminderType = Reminder.OutOfStockReminderType.OFF,
    var expirationReminderType: Reminder.ExpirationReminderType = Reminder.ExpirationReminderType.OFF,
    var notificationImportance: Reminder.NotificationImportance = Reminder.NotificationImportance.SAME_AS_MEDICINE,
)
