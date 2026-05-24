package com.futsch1.medtimer.core.domain.model

import java.time.Instant

data class PendingSnooze(
    val remindInstant: Instant,
    val reminderIds: List<Int>,
    val reminderEventIds: List<Int>,
    val notificationId: Int
)
