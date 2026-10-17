package com.futsch1.medtimer.robots

import android.os.SystemClock

/** Assertions for short-lived system UI such as Android Toasts. */
class ToastRobot {
    /** System toasts are not consistently exposed to UiAutomator, so wait past Android's long-toast duration. */
    fun waitForToastToExpire() = SystemClock.sleep(LONG_TOAST_DURATION_MILLIS)

    private companion object {
        const val LONG_TOAST_DURATION_MILLIS = 4_000L
    }
}
