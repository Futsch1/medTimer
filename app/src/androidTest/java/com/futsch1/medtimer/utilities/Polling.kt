package com.futsch1.medtimer.utilities

import android.os.SystemClock

private const val DEFAULT_POLL_TIMEOUT = 15_000L
private const val POLL_INTERVAL = 100L

/**
 * Retries [condition] until it holds, for state that lands outside any view hierarchy - a broadcast
 * writing to the database, an activity that has still to resume - where neither Espresso nor Compose
 * has anything to synchronize on. Returns whether it held before [timeoutMillis] ran out.
 */
fun pollUntil(timeoutMillis: Long = DEFAULT_POLL_TIMEOUT, condition: () -> Boolean): Boolean {
    val deadline = SystemClock.uptimeMillis() + timeoutMillis
    while (true) {
        if (condition()) {
            return true
        }
        if (SystemClock.uptimeMillis() >= deadline) {
            return false
        }
        SystemClock.sleep(POLL_INTERVAL)
    }
}
