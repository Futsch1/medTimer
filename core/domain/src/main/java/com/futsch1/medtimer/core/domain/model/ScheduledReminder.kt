package com.futsch1.medtimer.core.domain.model

import java.time.Instant

data class ScheduledReminder(
    var medicine: Medicine,
    var reminder: Reminder,
    var timestamp: Instant
)