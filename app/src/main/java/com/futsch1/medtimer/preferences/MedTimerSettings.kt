package com.futsch1.medtimer.preferences

import java.time.LocalTime
import kotlin.time.Duration

enum class DismissNotificationAction {
    SKIP, SNOOZE, TAKE
}

data class MedTimerSettings(
    val weekendTime: LocalTime,
    val weekendMode: Boolean,
    val weekendDays: Set<String>,
    val exactReminders: Boolean,
    val repeatReminders: Boolean,
    val numberOfRepetitions: Int,
    val repeatDelay: Duration,
    val snoozeDuration: Duration,
    val overrideDnd: Boolean,
    val stickyOnLockscreen: Boolean,
    val bigNotifications: Boolean,
    val dismissNotificationAction: DismissNotificationAction
)
