package com.futsch1.medtimer.robots

import android.os.SystemClock
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withTagKey
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.futsch1.medtimer.core.ui.R
import org.hamcrest.Matchers.equalTo
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
        val deadline = SystemClock.uptimeMillis() + UI_TIMEOUT_MILLIS
        var topicDisplayed = false
        while (!topicDisplayed && SystemClock.uptimeMillis() < deadline) {
            try {
                onView(withTagKey(R.id.help_reader_topic, equalTo(topic))).check(matches(isDisplayed()))
                topicDisplayed = true
            } catch (_: NoMatchingViewException) {
                SystemClock.sleep(POLL_INTERVAL_MILLIS)
            }
        }
        assertTrue(topicDisplayed, "Help topic '$topic' did not become selected")
    }

    private fun findText(text: String) =
        assertNotNull(
            device.wait(Until.findObject(By.textContains(text)), UI_TIMEOUT_MILLIS),
            "Missing help text: $text"
        )

    companion object {
        private const val UI_TIMEOUT_MILLIS = 5_000L
        private const val POLL_INTERVAL_MILLIS = 100L
    }
}
