package com.futsch1.medtimer.robots

import android.os.SystemClock
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.StaleObjectException
import androidx.test.uiautomator.UiDevice
import com.futsch1.medtimer.core.ui.R

class HelpRobot(private val menus: MenuRobot) {
    private val device get() = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    fun open() = menus.clickAppOption(R.string.help)

    fun assertHelpDisplayed() {
        val deadline = SystemClock.uptimeMillis() + UI_TIMEOUT_MILLIS
        while (SystemClock.uptimeMillis() < deadline) {
            try {
                val webView = device.findObject(By.clazz("android.webkit.WebView"))
                if (webView != null) return
            } catch (_: StaleObjectException) {
                // The WebView document can be replaced while its accessibility tree is updating.
            }
            SystemClock.sleep(POLL_INTERVAL_MILLIS)
        }
        throw AssertionError("Help not displayed")
    }

    fun pressBack() {
        device.pressBack()
    }

    companion object {
        private const val UI_TIMEOUT_MILLIS = 20_000L
        private const val POLL_INTERVAL_MILLIS = 100L
    }
}
