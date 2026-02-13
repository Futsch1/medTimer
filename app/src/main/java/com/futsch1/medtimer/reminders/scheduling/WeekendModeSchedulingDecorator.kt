package com.futsch1.medtimer.reminders.scheduling

import android.content.SharedPreferences
import android.util.ArraySet
import com.futsch1.medtimer.preferences.PreferencesNames
import com.futsch1.medtimer.reminders.TimeAccess
import java.time.Instant

class WeekendModeSchedulingDecorator(val scheduler: Scheduling, val timeAccess: TimeAccess, private val preferences: SharedPreferences) :
    Scheduling {
    fun adjustInstant(instant: Instant): Instant {
        var instant = instant
        if (this.isWeekendModeEnabled) {
            val weekendTime = preferences.getInt(PreferencesNames.WEEKEND_TIME, 540)
            val weekendDays: Set<String?> = preferences.getStringSet(PreferencesNames.WEEKEND_DAYS, ArraySet())!!
            val localDateTime = instant.atZone(timeAccess.systemZone())
            val dayOfWeek = localDateTime.dayOfWeek
            val minutes = localDateTime.minute + localDateTime.hour * 60
            val deltaMinutes = weekendTime - minutes
            if (weekendDays.contains(dayOfWeek.value.toString()) && deltaMinutes > 0) {
                instant = instant.plusSeconds(deltaMinutes * 60L)
            }
        }
        return instant
    }

    private val isWeekendModeEnabled: Boolean
        get() = preferences.getBoolean(PreferencesNames.WEEKEND_MODE, false)

    override fun getNextScheduledTime(): Instant? {
        var nextScheduledTime = scheduler.getNextScheduledTime()
        if (nextScheduledTime != null) {
            nextScheduledTime = adjustInstant(nextScheduledTime)
        }
        return nextScheduledTime
    }
}
