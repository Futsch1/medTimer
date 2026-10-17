package com.futsch1.medtimer.robots

import android.os.SystemClock
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.StaleObjectException
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import com.futsch1.medtimer.core.ui.R
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class HelpRobot(private val menus: MenuRobot) {
    private val device get() = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    fun open() = menus.clickAppOption(R.string.help)

    fun openGettingStarted() {
        findText("Getting started").click()
    }

    fun openNotificationsMissingOrLate() {
        findText("Notifications missing or late").click()
    }

    fun openReminderTypes() {
        findText("learn more about reminder types").click()
    }

    fun assertNotificationsMissingOrLateDisplayed() = assertTopicDisplayed("notifications-missing-or-late")

    fun assertReminderTypesDisplayed() = assertTopicDisplayed("reminder-types")

    fun assertContentsDisplayed() = assertTopicDisplayed("index")

    fun assertGettingStartedDisplayed() = assertTopicDisplayed("getting-started")

    fun pressBack() {
        device.pressBack()
    }

    fun pressToolbarBack() {
        assertNotNull(
            device.wait(Until.findObject(By.desc("Back")), UI_TIMEOUT_MILLIS),
            "Missing help toolbar Back button"
        ).click()
    }

    private fun assertTopicDisplayed(topic: String) {
        val heading = TOPIC_HEADINGS[topic] ?: error("Unknown Help topic: $topic")
        val deadline = SystemClock.uptimeMillis() + UI_TIMEOUT_MILLIS
        var topicDisplayed = false
        while (!topicDisplayed && SystemClock.uptimeMillis() < deadline) {
            topicDisplayed = try {
                val webView = device.findObject(By.clazz("android.webkit.WebView"))
                webView?.findObject(By.text(heading)) != null
            } catch (_: StaleObjectException) {
                false
            }
            if (!topicDisplayed) SystemClock.sleep(POLL_INTERVAL_MILLIS)
        }
        assertTrue(topicDisplayed, "Help topic '$topic' heading '$heading' was not displayed in the WebView")
    }

    private fun findText(text: String): UiObject2 {
        val deadline = SystemClock.uptimeMillis() + UI_TIMEOUT_MILLIS
        while (SystemClock.uptimeMillis() < deadline) {
            try {
                val webView = device.findObject(By.clazz("android.webkit.WebView"))
                val textObject = webView?.findObject(By.textContains(text))
                if (textObject != null) return textObject
                // A non-scrollable WebView may still be loading its accessibility tree; keep polling.
                if (webView?.isScrollable == true) webView.scroll(Direction.DOWN, SCROLL_PERCENT)
            } catch (_: StaleObjectException) {
                // The WebView document can be replaced while its accessibility tree is updating.
            }
            SystemClock.sleep(POLL_INTERVAL_MILLIS)
        }
        throw AssertionError("Missing help text: $text")
    }

    companion object {
        private val TOPIC_HEADINGS = mapOf(
            "index" to "Help",
            "getting-started" to "Getting started",
            "notifications-missing-or-late" to "Notifications missing or late",
            "reminder-types" to "Reminder types"
        )
        private const val UI_TIMEOUT_MILLIS = 10_000L
        private const val POLL_INTERVAL_MILLIS = 100L
        private const val SCROLL_PERCENT = 0.8f
    }
}
