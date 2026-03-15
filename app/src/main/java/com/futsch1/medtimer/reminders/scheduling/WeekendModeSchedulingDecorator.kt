package com.futsch1.medtimer.reminders.scheduling

import com.futsch1.medtimer.preferences.MedTimerPreferences
import com.futsch1.medtimer.preferences.MedTimerPreferencesDataSource
import com.futsch1.medtimer.reminders.TimeAccess
import java.time.Instant

class WeekendModeSchedulingDecorator(
    val scheduler: Scheduling,
    val timeAccess: TimeAccess,
    private val dataSource: MedTimerPreferencesDataSource
) :
    Scheduling {

    fun adjustInstant(instant: Instant, settings: MedTimerPreferences): Instant {
        var instant = instant
        if (settings.weekendMode) {
            val localDateTime = instant.atZone(timeAccess.systemZone())
            val dayOfWeek = localDateTime.dayOfWeek
            val deltaSeconds = settings.weekendTime.toSecondOfDay() - localDateTime.toLocalTime().toSecondOfDay()
            if (settings.weekendDays.contains(dayOfWeek.value.toString()) && deltaSeconds > 0) {
                instant = instant.plusSeconds(deltaSeconds.toLong())
            }
        }
        return instant
    }

    override fun getNextScheduledTime(): Instant? {
        var nextScheduledTime = scheduler.getNextScheduledTime()

        if (nextScheduledTime != null) {
            nextScheduledTime = adjustInstant(nextScheduledTime, dataSource.data.value)
        }
        return nextScheduledTime
    }
}
