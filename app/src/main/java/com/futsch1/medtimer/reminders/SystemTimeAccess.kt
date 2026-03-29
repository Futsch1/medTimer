package com.futsch1.medtimer.reminders

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SystemTimeAccess @Inject constructor() : TimeAccess {
    override fun systemZone(): ZoneId = ZoneId.systemDefault()
    override fun localDate(): LocalDate = LocalDate.now(systemZone())
    override fun now(): Instant = Instant.now()
}
