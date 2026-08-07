package com.futsch1.medtimer.harness

import com.futsch1.medtimer.core.common.time.TimeAccess
import java.time.Instant
import java.time.ZoneId

/**
 * Freezes the app's view of time to the instant it was constructed, so a test run that takes
 * tens of minutes never sees a midnight rollover mid-test - construct one per test.
 */
class FakeTimeAccess : TimeAccess {
    private val zone: ZoneId = ZoneId.systemDefault()
    private val instant: Instant = Instant.now()

    override fun systemZone() = zone
    override fun localDate() = instant.atZone(zone).toLocalDate()
    override fun now() = instant
}
