package com.futsch1.medtimer

import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withResourceName
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.futsch1.medtimer.AndroidTestHelper.MainMenu
import com.futsch1.medtimer.AndroidTestHelper.navigateTo
import org.hamcrest.Matchers.allOf
import org.junit.Assert
import org.junit.Rule
import org.junit.Test

@LargeTest
class SettingsTest : BaseTestHelper() {

    @JvmField
    @Rule
    var mActivityScenarioRule: ActivityScenarioRule<MainActivity> = ActivityScenarioRule(
        MainActivity::class.java
    )

    @Test
    fun actionOnDismissedNotification() {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        device.wait(Until.findObject(By.desc("More options")), 2000)
        Espresso.openActionBarOverflowOrOptionsMenu(InstrumentationRegistry.getInstrumentation().targetContext)

        // Skip reminder on dismiss
        onView(withText(R.string.tab_settings)).perform(click())
        onView(withText(R.string.dismiss_notification_action)).perform(click())
        onView(withText(R.string.skip_reminder)).perform(click())
        pressBack()

        navigateTo(MainMenu.MEDICINES)

        AndroidTestHelper.createMedicine("Test med")
        // Interval reminder (amount 1) 2 hours from now
        AndroidTestHelper.createIntervalReminder("1", 120)
        pressBack()

        // Now dismiss notification
        // We navigate to analysis first to not accidentally grab another UI object with text "Test med"
        navigateTo(MainMenu.ANALYSIS)
        device.openNotification()
        val notification = device.wait(Until.findObject(By.textContains("Test med")), 2000)
        Assert.assertNotNull(notification)
        notification.fling(Direction.RIGHT)
        device.pressBack()

        // Check overview and next reminders
        navigateTo(MainMenu.OVERVIEW)
        onView(
            RecyclerViewMatcher(R.id.latestReminders).atPositionOnView(
                0,
                R.id.chipSkipped
            )
        ).check(matches(isChecked()))

        // Now change to action taken on dismiss
        Espresso.openActionBarOverflowOrOptionsMenu(InstrumentationRegistry.getInstrumentation().targetContext)

        // Skip reminder on dismiss
        onView(withText(R.string.tab_settings)).perform(click())
        onView(withText(R.string.dismiss_notification_action)).perform(click())
        onView(withText(R.string.taken)).perform(click())
        device.pressBack()

        // Clear event data (causes reminder to be re-raised)
        device.wait(Until.findObject(By.desc("More options")), 2000)
        Espresso.openActionBarOverflowOrOptionsMenu(InstrumentationRegistry.getInstrumentation().targetContext)
        onView(withText(R.string.event_data)).perform(click())
        onView(withText(R.string.clear_events)).perform(click())
        onView(withId(android.R.id.button1)).perform(click())

        // Now dismiss notification
        // We navigate to analysis first to not accidentally grab another UI object with text "Test med"
        navigateTo(MainMenu.ANALYSIS)
        device.openNotification()
        val newNotification = device.wait(Until.findObject(By.textContains("Test med")), 2000)
        Assert.assertNotNull(newNotification)
        newNotification.fling(Direction.RIGHT)
        device.pressBack()

        // Check overview and next reminders
        navigateTo(MainMenu.OVERVIEW)
        onView(
            RecyclerViewMatcher(R.id.latestReminders).atPositionOnView(
                0,
                R.id.chipTaken
            )
        ).check(matches(isChecked()))
    }

    @Test
    fun repeatingReminders() {
        // Use an interval reminder an check if the timestamp changes
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        device.wait(Until.findObject(By.desc("More options")), 2000)
        Espresso.openActionBarOverflowOrOptionsMenu(InstrumentationRegistry.getInstrumentation().targetContext)

        // Repeat reminder every minute
        onView(withText(R.string.tab_settings)).perform(click())
        onView(withText(R.string.repeat_reminders)).perform(click())
        onView(
            allOf(
                withText(R.string.repeat_reminders),
                withResourceName("title")
            )
        ).perform(click())
        onView(withText(R.string.time_between_repetitions)).perform(click())
        onView(withText(R.string.minutes_1)).perform(click())
        pressBack()

        navigateTo(MainMenu.MEDICINES)

        AndroidTestHelper.createMedicine("Test med")
        AndroidTestHelper.createIntervalReminder("1", 120)
        pressBack()

        device.openNotification()
        val notification = device.wait(Until.findObject(By.textContains("Test med")), 2000)
        Assert.assertNotNull(notification)

        device.wait(Until.gone(By.text(notification.text)), 240_000)

        val nextNotification = device.wait(Until.findObject(By.textContains("Test med")), 2000)
        Assert.assertNotNull(nextNotification)
    }
}