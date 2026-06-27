package com.futsch1.medtimer.core.domain.model

data class ProcessedReminder(
    val scheduledReminder: ScheduledReminder,
    val stockBefore: Double,
    val stockAfter: Double
)
