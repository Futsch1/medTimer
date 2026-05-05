package com.futsch1.medtimer.reminders.scheduling

import com.futsch1.medtimer.model.UserPreferences
import com.futsch1.medtimer.preferences.PreferencesDataSource
import com.futsch1.medtimer.reminders.TimeAccess
import java.time.Instant

class WeekendModeSchedulingDecorator(
    private val scheduler: Scheduling,
    private val timeAccess: TimeAccess,
    private val dataSource: PreferencesDataSource
) :
    Scheduling {

    fun adjustInstant(instant: Instant, settings: UserPreferences): Instant {
        var instant = instant
        if (settings.weekendMode) {
            val localDateTime = instant.atZone(timeAccess.systemZone())
            val dayOfWeek = localDateTime.dayOfWeek
            if (settings.weekendDays.contains(dayOfWeek.value.toString())) {
                val currentSeconds = localDateTime.toLocalTime().toSecondOfDay()
                val startSeconds = settings.weekendStartTime.toSecondOfDay()
                val endSeconds = settings.weekendEndTime.toSecondOfDay()
                if (currentSeconds in startSeconds until endSeconds) {
                    instant = instant.plusSeconds((endSeconds - currentSeconds).toLong())
                }
            }
        }
        return instant
    }

    override fun getNextScheduledTime(): Instant? {
        var nextScheduledTime = scheduler.getNextScheduledTime()

        if (nextScheduledTime != null) {
            nextScheduledTime = adjustInstant(nextScheduledTime, dataSource.preferences.value)
        }
        return nextScheduledTime
    }
}
