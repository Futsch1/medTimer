package com.futsch1.medtimer.core.common.time

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

interface TimeAccess {
    fun systemZone(): ZoneId

    fun localDate(): LocalDate

    fun now(): Instant
}
