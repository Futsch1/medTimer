package com.futsch1.medtimer

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.withResourceName
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.adevinta.android.barista.assertion.BaristaListAssertions.assertCustomAssertionAtPosition
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import com.adevinta.android.barista.interaction.BaristaDialogInteractions.clickDialogPositiveButton
import com.adevinta.android.barista.interaction.BaristaMenuClickInteractions.openMenu
import com.futsch1.medtimer.AndroidTestHelper.MainMenu
import com.futsch1.medtimer.AndroidTestHelper.navigateTo
import org.hamcrest.Matchers.allOf
import org.junit.Assert
import org.junit.Test

class SettingsTest : BaseTestHelper() {

    @Test
    //@AllowFlaky(attempts = 1)
    fun actionOnDismissedNotification() {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        openMenu()

        // Skip reminder on dismiss
        clickOn(R.string.tab_settings)
        clickOn(R.string.dismiss_notification_action)
        clickOn(R.string.skip_reminder)
        pressBack()

        navigateTo(MainMenu.MEDICINES)

        AndroidTestHelper.createMedicine("Test med")
        // Interval reminder (amount 1) 2 hours from now
        AndroidTestHelper.createIntervalReminder("1", 120)
        pressBack()

        dismissNotification(device)

        // Check overview and next reminders
        navigateTo(MainMenu.OVERVIEW)
        assertCustomAssertionAtPosition(
            R.id.latestReminders,
            0,
            R.id.chipSkipped,
            matches(isChecked())
        )

        // Now change to action taken on dismiss
        openMenu()

        // Skip reminder on dismiss
        clickOn(R.string.tab_settings)
        clickOn(R.string.dismiss_notification_action)
        clickOn(R.string.taken)
        device.pressBack()

        // Clear event data (causes reminder to be re-raised)
        openMenu()
        clickOn(R.string.event_data)
        clickOn(R.string.clear_events)
        clickDialogPositiveButton()

        dismissNotification(device)

        // Check overview and next reminders
        navigateTo(MainMenu.OVERVIEW)
        assertCustomAssertionAtPosition(
            R.id.latestReminders,
            0,
            R.id.chipTaken,
            matches(isChecked())
        )
    }

    @Test
    //@AllowFlaky(attempts = 1)
    fun repeatingReminders() {
        // Use an interval reminder an check if the timestamp changes
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        device.openNotification()
        device.pressBack()
        openMenu()

        // Repeat reminder every minute
        clickOn(R.string.tab_settings)
        clickOn(R.string.repeat_reminders)
        onView(
            allOf(
                withText(R.string.repeat_reminders),
                withResourceName("title")
            )
        ).perform(click())
        clickOn(R.string.time_between_repetitions)
        clickOn(R.string.minutes_1)
        pressBack()

        navigateTo(MainMenu.MEDICINES)

        AndroidTestHelper.createMedicine("Test med")
        AndroidTestHelper.createIntervalReminder("1", 120)
        pressBack()

        device.openNotification()
        val notification = device.wait(Until.findObject(By.textContains("Test med")), 2000)
        Assert.assertNotNull(notification)
        val text = notification.text

        device.wait(Until.gone(By.text(text)), 240_000)

        val nextNotification = device.wait(Until.findObject(By.textContains("Test med")), 2000)
        Assert.assertNotNull(nextNotification)
        device.pressBack()
    }

    private fun dismissNotification(device: UiDevice) {
        // Now dismiss notification
        // We navigate to analysis first to not accidentally grab another UI object with text "Test med"
        navigateTo(MainMenu.ANALYSIS)
        device.openNotification()
        var notification = device.wait(Until.findObject(By.textContains("Test med")), 2000)
        Assert.assertNotNull(notification)
        notification.fling(Direction.RIGHT)
        notification = device.wait(Until.findObject(By.textContains("Test med")), 500)
        notification?.fling(Direction.RIGHT)

        device.pressBack()
    }
}