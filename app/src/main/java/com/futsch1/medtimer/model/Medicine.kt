package com.futsch1.medtimer.model

import com.futsch1.medtimer.ReminderNotificationChannelManager
import java.time.LocalDate

data class Medicine(
    val name: String,
    val color: Int = 0,
    val useColor: Boolean = false,
    val notificationImportance: ReminderNotificationChannelManager.Importance = ReminderNotificationChannelManager.Importance.DEFAULT,
    val iconId: Int = 0,
    val amount: Double = 0.0,
    val refillSizes: List<Double> = emptyList(),
    val unit: String = "",
    val sortOrder: Double = 1.0,
    val notes: String = "",
    val showNotificationAsAlarm: Boolean = false,
    val productionDate: LocalDate = LocalDate.EPOCH,
    val expirationDate: LocalDate = LocalDate.EPOCH,
    val tags: List<Tag> = emptyList(),
    val reminders: List<Reminder> = emptyList()
)
