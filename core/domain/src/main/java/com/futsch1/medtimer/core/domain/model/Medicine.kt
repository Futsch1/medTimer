package com.futsch1.medtimer.core.domain.model

import android.app.NotificationManager
import android.graphics.Color
import java.time.LocalDate

data class Medicine(
    val name: String,
    val id: Int,
    val color: Int,
    val useColor: Boolean,
    val notificationImportance: NotificationImportance,
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
    val reminders: List<Reminder>,
    val cannotBeSkipped: Boolean,
    val excludeFromPrescriptionRequest: Boolean = false
) {
    enum class NotificationImportance(val value: Int) {
        DEFAULT(NotificationManager.IMPORTANCE_DEFAULT),
        HIGH(NotificationManager.IMPORTANCE_HIGH)
    }

    enum class ReminderChannel(val channelId: String, val importance: Int) {
        // "3" and "4" are legacy channel IDs from when the app used importance.toString() directly.
        // Keeping them for backward compatibility so users don't lose their notification settings.
        DEFAULT("3", NotificationManager.IMPORTANCE_DEFAULT),
        HIGH("4", NotificationManager.IMPORTANCE_HIGH),
        OUT_OF_STOCK("out_of_stock", NotificationManager.IMPORTANCE_DEFAULT)
    }

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
            color = Color.DKGRAY,
            useColor = false,
            notificationImportance = NotificationImportance.DEFAULT,
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
            reminders = emptyList(),
            cannotBeSkipped = false,
            excludeFromPrescriptionRequest = false
        )
    }
}
