package com.futsch1.medtimer.model

import com.futsch1.medtimer.ReminderNotificationChannelManager
import java.time.LocalDate

data class Medicine(
    val name: String,
    val id: Int,
    val color: Int,
    val useColor: Boolean,
    val notificationImportance: ReminderNotificationChannelManager.Importance,
    val iconId: Int,
    val amount: Double,
    val refillSize: Double,
    val unit: String,
    val notes: String,
    val showNotificationAsAlarm: Boolean,
    val productionDate: LocalDate,
    val expirationDate: LocalDate,
    val sortOrder: Double,
    val tags: List<Tag>,
    val reminders: List<Reminder>
) {

    fun hasExpired(): Boolean {
        return expirationDate != LocalDate.EPOCH && expirationDate < LocalDate.now()
    }

    fun isOutOfStock(): Boolean {
        return this.isStockManagementActive() && reminders.any { reminder -> reminder.outOfStockReminderType != Reminder.OutOfStockReminderType.OFF && reminder.outOfStockThreshold >= amount }
    }

    fun isStockManagementActive(): Boolean {
        return (amount != 0.0 || hasStockReminder())
    }

    private fun hasStockReminder(): Boolean {
        return reminders.any { reminder -> reminder.outOfStockReminderType != Reminder.OutOfStockReminderType.OFF }
    }

    companion object {
        fun default(): Medicine = Medicine(
            name = "",
            id = 0,
            color = 0,
            useColor = false,
            notificationImportance = ReminderNotificationChannelManager.Importance.DEFAULT,
            iconId = 0,
            amount = 0.0,
            refillSize = 0.0,
            unit = "",
            notes = "",
            showNotificationAsAlarm = false,
            productionDate = LocalDate.EPOCH,
            expirationDate = LocalDate.EPOCH,
            sortOrder = 1.0,
            tags = emptyList(),
            reminders = emptyList()
        )
    }
}
