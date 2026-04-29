package com.futsch1.medtimer.model

import java.time.LocalTime

data class ReminderTime(val minutes: Int, val isDuration: Boolean = false) : Comparable<ReminderTime> {
    val seconds: Long
        get() = minutes * 60L

    constructor(localTime: LocalTime) : this(localTime.hour * 60 + localTime.minute)


    fun getLocalTime(): LocalTime {
        return LocalTime.of(minutes / 60, minutes % 60)
    }

    override fun compareTo(other: ReminderTime): Int {
        return minutes - other.minutes
    }

    companion object {
        const val DEFAULT_TIME: Int = 480
    }
}