package com.futsch1.medtimer.model

import java.time.Instant

data class ScheduledReminder(
    val medicine: Medicine,
    val reminder: Reminder,
    val timestamp: Instant
)