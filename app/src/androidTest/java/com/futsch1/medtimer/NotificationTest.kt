package com.futsch1.medtimer

import android.os.Build
import android.view.InputDevice
import android.view.MotionEvent
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.GeneralClickAction
import androidx.test.espresso.action.GeneralLocation
import androidx.test.espresso.action.Press
import androidx.test.espresso.action.Tap
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.withResourceName
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.adevinta.android.barista.assertion.BaristaListAssertions.assertCustomAssertionAtPosition
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertContains
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import com.adevinta.android.barista.interaction.BaristaDialogInteractions.clickDialogPositiveButton
import com.adevinta.android.barista.interaction.BaristaEditTextInteractions.writeTo
import com.adevinta.android.barista.interaction.BaristaMenuClickInteractions.openMenu
import com.adevinta.android.barista.rule.flaky.AllowFlaky
import com.futsch1.medtimer.AndroidTestHelper.MainMenu
import com.futsch1.medtimer.AndroidTestHelper.navigateTo
import org.hamcrest.Matchers.allOf
import org.junit.Assert
import org.junit.Test


class NotificationTest : BaseTestHelper() {
    @Test
    fun notificationTest() {
        AndroidTestHelper.createMedicine("Test med")

        // Set color and icon
        clickOn(R.id.enableColor)
        clickOn(R.id.selectColor)
        onView(withResourceName("colorPickerView")).perform(
            GeneralClickAction(
                Tap.SINGLE,
                GeneralLocation.CENTER_LEFT,
                Press.FINGER,
                InputDevice.SOURCE_UNKNOWN,
                MotionEvent.BUTTON_PRIMARY
            )
        )
        clickDialogPositiveButton()

        clickOn(R.id.selectIcon)
        onView(withResourceName("icd_rcv_icon_list")).perform(
            RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(
                1,
                click()
            )
        )

        AndroidTestHelper.createReminder(
            "1",
            AndroidTestHelper.getNextNotificationTime().toLocalTime()
        )

        clickOn(R.id.openAdvancedSettings)

        clickOn(R.id.addLinkedReminder)
        clickDialogPositiveButton()
        AndroidTestHelper.setTime(0, 1, true)

        navigateTo(MainMenu.OVERVIEW)

        baristaRule.activityTestRule.finishActivity()

        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        waitAndDismissNotification(device, 240_000)
        waitAndDismissNotification(device, 180_000)
    }

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

        navigateTo(MainMenu.ANALYSIS)
        waitAndDismissNotification(device)

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

        navigateTo(MainMenu.ANALYSIS)
        waitAndDismissNotification(device)

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

    @Test
    @AllowFlaky(attempts = 1)
    fun variableAmount() {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        AndroidTestHelper.createMedicine("Test med")
        AndroidTestHelper.createIntervalReminder("1", 1)
        clickOn(R.id.openAdvancedSettings)
        clickOn(R.id.variableAmount)
        pressBack()
        navigateTo(MainMenu.ANALYSIS)
        device.openNotification()
        device.wait(Until.findObject(By.textContains("Test med")), 2_000)
        device.findObject(By.text(getNotificationText())).click()
        device.pressBack()
        device.openNotification()
        device.wait(Until.findObject(By.textContains("Test med")), 240_000)
        device.findObject(By.text(getNotificationText())).click()
        device.wait(Until.findObject(By.displayId(android.R.id.input)), 2_000)
        writeTo(android.R.id.input, "Test variable amount")
        clickDialogPositiveButton()
        navigateTo(MainMenu.OVERVIEW)
        assertContains("Test variable amount")

        clickOn(R.id.takenNow)
        writeTo(android.R.id.input, "Test variable amount again")
        clickDialogPositiveButton()

        assertContains("Test variable amount again")
    }

    private fun getNotificationText(): String {
        val s = InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.taken)
        return if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            s.uppercase()
        } else {
            s
        }
    }

    private fun waitAndDismissNotification(device: UiDevice, timeout: Long = 2000) {
        device.openNotification()
        var notification = device.wait(Until.findObject(By.textContains("Test med")), timeout)
        Assert.assertNotNull(notification)
        notification.fling(Direction.RIGHT)
        notification = device.wait(Until.findObject(By.textContains("Test med")), 500)
        notification?.fling(Direction.RIGHT)

        device.pressBack()
    }
}
