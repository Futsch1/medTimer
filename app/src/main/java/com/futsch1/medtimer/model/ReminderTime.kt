package com.futsch1.medtimer.model

import java.time.LocalTime

class ReminderTime : Comparable<ReminderTime> {
    val minutes: Int
    val seconds: Long
        get() = minutes * 60L

    val isDuration: Boolean

    constructor(minutes: Int, isDuration: Boolean = false) {
        this.minutes = minutes
        this.isDuration = isDuration
    }

    constructor(localTime: LocalTime) {
        this.minutes = localTime.hour * 60 + localTime.minute
        this.isDuration = false
    }

    fun getLocalTime(): LocalTime {
        return LocalTime.of(minutes / 60, minutes % 60)
    }

    override fun compareTo(other: ReminderTime): Int {
        return minutes - other.minutes
    }
}