package com.futsch1.medtimer.reminders.scheduling

import java.time.Instant

fun interface Scheduling {
    fun getNextScheduledTime(): Instant?
}