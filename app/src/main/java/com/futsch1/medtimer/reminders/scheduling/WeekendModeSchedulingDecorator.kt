package com.futsch1.medtimer.reminders.scheduling

import com.futsch1.medtimer.preferences.MedTimerPreferencesDataSource
import com.futsch1.medtimer.preferences.MedTimerSettings
import com.futsch1.medtimer.reminders.TimeAccess
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.time.Instant

class WeekendModeSchedulingDecorator(
    val scheduler: Scheduling,
    val timeAccess: TimeAccess,
    private val dataSource: MedTimerPreferencesDataSource
) :
    Scheduling {

    fun adjustInstant(instant: Instant, settings: MedTimerSettings): Instant {
        var instant = instant
        if (settings.weekendMode) {
            val localDateTime = instant.atZone(timeAccess.systemZone())
            val dayOfWeek = localDateTime.dayOfWeek
            val minutes = localDateTime.minute + localDateTime.hour * 60
            val deltaMinutes = settings.weekendTime - minutes
            if (settings.weekendDays.contains(dayOfWeek.value.toString()) && deltaMinutes > 0) {
                instant = instant.plusSeconds(deltaMinutes * 60L)
            }
        }
        return instant
    }

    override fun getNextScheduledTime(): Instant? {
        var nextScheduledTime = scheduler.getNextScheduledTime()

        if (nextScheduledTime != null) {
            val settings = runBlocking { dataSource.data.first() }
            nextScheduledTime = adjustInstant(nextScheduledTime, settings)
        }
        return nextScheduledTime
    }
}
