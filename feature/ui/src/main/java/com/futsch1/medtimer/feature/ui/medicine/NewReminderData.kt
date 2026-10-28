package com.futsch1.medtimer.feature.ui.medicine

import com.futsch1.medtimer.core.domain.model.ReminderType
import java.time.Instant
import java.time.LocalTime

enum class NewReminderStockFrequency { ONCE, ALWAYS, DAILY }
enum class NewReminderExpirationFrequency { ONCE, DAILY }

data class NewReminderData(
    val medicineId: Int,
    val type: ReminderType,
    val amount: String,
    val timeMinutes: Int,
    val intervalMinutes: Int? = null,
    val intervalStart: Instant? = null,
    val intervalStartsFromProcessed: Boolean = false,
    val dailyStartTime: LocalTime? = null,
    val dailyEndTime: LocalTime? = null,
    val stockThreshold: Double? = null,
    val stockFrequency: NewReminderStockFrequency? = null,
    val expirationDaysBefore: Long? = null,
    val expirationFrequency: NewReminderExpirationFrequency? = null,
)
