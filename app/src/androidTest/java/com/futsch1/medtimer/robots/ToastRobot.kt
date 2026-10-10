package com.futsch1.medtimer.robots

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Assert.assertTrue

/** Assertions for short-lived system UI such as Android Toasts. */
class ToastRobot {
    private val device get() = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    fun waitUntilGone(text: String, timeoutMillis: Long = DEFAULT_TIMEOUT) {
        assertTrue("Toast '$text' did not disappear", device.wait(Until.gone(By.text(text)), timeoutMillis))
    }

    private companion object {
        const val DEFAULT_TIMEOUT = 5_000L
    }
}
