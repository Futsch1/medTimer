package com.futsch1.medtimer.preferences

import java.time.LocalTime
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

enum class DismissNotificationAction {
    SKIP, SNOOZE, TAKE
}

data class MedTimerSettings(
    val weekendTime: LocalTime = LocalTime.of(9, 0),
    val weekendMode: Boolean = false,
    val weekendDays: Set<String> = emptySet(),
    val exactReminders: Boolean = false,
    val repeatReminders: Boolean = false,
    val numberOfRepetitions: Int = 3,
    val repeatDelay: Duration = 10.toDuration(DurationUnit.MINUTES),
    val snoozeDuration: Duration = 15.toDuration(DurationUnit.MINUTES),
    val overrideDnd: Boolean = false,
    val stickyOnLockscreen: Boolean = false,
    val bigNotifications: Boolean = false,
    val dismissNotificationAction: DismissNotificationAction = DismissNotificationAction.SKIP,
    val combineNotifications: Boolean = false,
    val hideMedicineName: Boolean = false
)

val DEFAULT_MEDTIMER_SETTINGS = MedTimerSettings()

