package com.futsch1.medtimer.robots

import android.app.Activity
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.RootMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import androidx.test.uiautomator.UiDevice
import com.futsch1.medtimer.feature.reminders.alarm.ReminderAlarmActivity
import com.futsch1.medtimer.utilities.pollUntil
import org.hamcrest.Matchers.`is`
import kotlin.test.assertTrue

/** The full-screen alarm. It is MedTimer's own activity, so Espresso drives it once it has resumed. */
class AlarmScreenRobot {

    private val device: UiDevice get() = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    /** The alarm only proves itself by waking a sleeping device, so the test drives the screen too. */
    fun wakeDevice() = device.wakeUp()

    fun sleepDevice() = device.sleep()

    fun awaitShown(timeoutMillis: Long, message: String) {
        assertTrue(pollUntil(timeoutMillis) { alarmActivity() != null }, message)
    }

    /** Taps Taken until the alarm closes: it can resume after the first tap lands. */
    fun take(timeoutMillis: Long, message: String) {
        awaitShown(timeoutMillis, message)
        assertTrue(
            pollUntil(CLOSE_TIMEOUT) {
                clickTaken()
                alarmActivity() == null
            },
            "Alarm screen did not close"
        )
    }

    private fun clickTaken() {
        val activity = alarmActivity() ?: return
        onView(withId(com.futsch1.medtimer.feature.reminders.R.id.takenButton))
            .inRoot(RootMatchers.withDecorView(`is`(activity.window.decorView)))
            .withFailureHandler { _, _ -> }
            .perform(click())
    }

    private fun alarmActivity(): Activity? {
        var activity: Activity? = null
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            activity = ActivityLifecycleMonitorRegistry.getInstance()
                .getActivitiesInStage(Stage.RESUMED)
                .firstOrNull { it is ReminderAlarmActivity }
        }
        return activity
    }

    private companion object {
        const val CLOSE_TIMEOUT = 10_000L
    }
}
