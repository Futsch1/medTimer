package com.futsch1.medtimer.core.domain.model

data class SimulatedReminder(
    val scheduledReminder: ScheduledReminder,
    val stockBefore: Double,
    val stockAfter: Double
)
