package com.futsch1.medtimer

import android.os.Build
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.withResourceName
import androidx.test.espresso.matcher.ViewMatchers.withTagValue
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import com.adevinta.android.barista.assertion.BaristaListAssertions.assertCustomAssertionAtPosition
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertContains
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import com.adevinta.android.barista.interaction.BaristaDialogInteractions.clickDialogPositiveButton
import com.adevinta.android.barista.interaction.BaristaEditTextInteractions.writeTo
import com.adevinta.android.barista.interaction.BaristaListInteractions
import com.adevinta.android.barista.interaction.BaristaMenuClickInteractions.openMenu
import com.adevinta.android.barista.interaction.BaristaSleepInteractions.sleep
import com.futsch1.medtimer.AndroidTestHelper.MainMenu
import com.futsch1.medtimer.AndroidTestHelper.navigateTo
import com.futsch1.medtimer.helpers.TimeHelper
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.equalTo
import org.junit.Assert.assertNotNull
import org.junit.Test


private const val TEST_MED = "Test med"

private const val SECOND_ONE = "second one"

class NotificationTest : BaseTestHelper() {
    @Test
    //@AllowFlaky(attempts = 1)
    fun notificationTest() {
        AndroidTestHelper.createMedicine(TEST_MED)

        // Set color and icon
        clickOn(R.id.enableColor)
        clickOn(R.id.selectColor)
        onView(withResourceName("hexEdit")).perform(
            ViewActions.clearText(),
            ViewActions.typeText("deadbe")
        )
        closeSoftKeyboard()
        clickOn(R.id.confirmSelectColor)

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
        clickOn(R.string.notification_reminder_settings)
        clickOn(R.string.dismiss_notification_action)
        clickOn(R.string.skip_reminder)
        pressBack()
        pressBack()

        navigateTo(MainMenu.MEDICINES)

        AndroidTestHelper.createMedicine(TEST_MED)
        // Interval reminder (amount 1) 2 hours from now
        AndroidTestHelper.createIntervalReminder("1", 120)
        pressBack()

        navigateTo(MainMenu.ANALYSIS)
        waitAndDismissNotification(device)

        // Check overview and next reminders
        navigateTo(MainMenu.OVERVIEW)
        assertCustomAssertionAtPosition(
            R.id.reminders,
            0,
            R.id.stateButton,
            matches(withTagValue(equalTo(R.drawable.x_circle)))
        )

        // Now change to action taken on dismiss
        openMenu()

        // Skip reminder on dismiss
        clickOn(R.string.tab_settings)
        clickOn(R.string.notification_reminder_settings)
        clickOn(R.string.dismiss_notification_action)
        clickOn(R.string.taken)
        pressBack()
        pressBack()

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
            R.id.reminders,
            0,
            R.id.stateButton,
            matches(withTagValue(equalTo(R.drawable.check2_circle)))
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
        clickOn(R.string.notification_reminder_settings)
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
        pressBack()

        navigateTo(MainMenu.MEDICINES)

        AndroidTestHelper.createMedicine(TEST_MED)
        AndroidTestHelper.createIntervalReminder("1", 120)
        pressBack()

        device.openNotification()
        val notification = device.wait(Until.findObject(By.textContains(TEST_MED)), 2000)
        assertNotNull(notification)
        val text = notification.text

        device.wait(Until.gone(By.text(text)), 240_000)

        val nextNotification = device.wait(Until.findObject(By.textContains(TEST_MED)), 2000)
        assertNotNull(nextNotification)
        device.pressBack()
    }

    @Test
    //@AllowFlaky(attempts = 1)
    fun variableAmount() {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        AndroidTestHelper.createMedicine(TEST_MED)
        AndroidTestHelper.createIntervalReminder("1", 2)
        clickOn(R.id.openAdvancedSettings)
        clickOn(R.id.variableAmount)
        pressBack()
        navigateTo(MainMenu.ANALYSIS)
        device.openNotification()
        sleep(2_000)
        device.wait(Until.findObject(By.textContains(TEST_MED)), 2_000)
        clickNotificationButton(device, TEST_MED, getNotificationText(R.string.taken))

        device.pressBack()

        device.openNotification()
        sleep(2_000)
        device.wait(Until.findObject(By.textContains(TEST_MED)), 240_000)
        clickNotificationButton(device, TEST_MED, getNotificationText(R.string.taken))

        device.wait(Until.findObject(By.displayId(android.R.id.input)), 2_000)
        writeTo(android.R.id.input, "Test variable amount")
        clickDialogPositiveButton()
        navigateTo(MainMenu.OVERVIEW)
        assertContains("Test variable amount")

        BaristaListInteractions.clickListItemChild(R.id.reminders, 2, R.id.stateButton)
        clickOn(R.id.takenButton)
        writeTo(android.R.id.input, "Test variable amount again")
        clickDialogPositiveButton()

        assertContains("Test variable amount again")
    }

    private fun clickNotificationButton(device: UiDevice, notificationText: String, buttonText: String) {
        makeNotificationExpanded(device, notificationText)
        val button = device.findObject(By.text(buttonText))
        internalAssert(button != null)
        button.click()
    }

    private fun makeNotificationExpanded(device: UiDevice, notificationText: String) {
        val notificationRow = device.findObject(By.res("com.android.systemui:id/expandableNotificationRow").hasDescendant(By.textContains(notificationText)))
        if (notificationRow != null) {
            val expand = notificationRow.findObject(By.res("android:id/expand_button"))
            if (expand?.contentDescription == "Expand") {
                expand.click()
            }
        }
    }

    @Test
    //@AllowFlaky(attempts = 1)
    fun customSnooze() {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        openMenu()
        clickOn(R.string.tab_settings)
        clickOn(R.string.notification_reminder_settings)
        clickOn(R.string.dismiss_notification_action)
        clickOn(R.string.snooze)
        clickOn(R.string.snooze_duration)
        clickOn(R.string.custom)
        pressBack()
        pressBack()

        AndroidTestHelper.createMedicine(TEST_MED)
        AndroidTestHelper.createIntervalReminder("1", 120)

        navigateTo(MainMenu.ANALYSIS)
        waitAndDismissNotification(device, 2_000)

        device.wait(Until.findObject(By.displayId(android.R.id.input)), 2_000)
        writeTo(android.R.id.input, "1")
        clickDialogPositiveButton()

        device.openNotification()
        sleep(2_000)
        val notification = device.wait(Until.findObject(By.textContains(TEST_MED)), 240_000)
        internalAssert(notification != null)
        makeNotificationExpanded(device, TEST_MED)
        internalAssert(device.findObject(By.text(getNotificationText(R.string.taken))) != null)
        internalAssert(device.findObject(By.text(getNotificationText(R.string.skipped))) != null)
        internalAssert(device.findObject(By.text(getNotificationText(R.string.snooze))) == null)
    }

    @Test
    //@AllowFlaky(attempts = 1)
    fun hiddenMedicineName() {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        openMenu()
        clickOn(R.string.tab_settings)
        clickOn(R.string.privacy_settings)
        clickOn(R.string.hide_med_name)

        pressBack()

        AndroidTestHelper.createMedicine(TEST_MED)
        AndroidTestHelper.createIntervalReminder("1", 120)
        pressBack()
        assertContains(TEST_MED)

        device.openNotification()
        val notification = device.wait(Until.findObject(By.textContains("T*******")), 2000)
        assertNotNull(notification)
    }

    @Test
    //@AllowFlaky(attempts = 1)
    fun bigButtons() {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val packageName = device.currentPackageName

        openMenu()
        clickOn(R.string.tab_settings)
        clickOn(R.string.display_settings)
        clickOn(R.string.big_notifications)

        pressBack()

        AndroidTestHelper.createMedicine(TEST_MED)
        AndroidTestHelper.createIntervalReminder("1", 120)
        pressBack()

        device.openNotification()
        device.wait(Until.findObject(By.textContains(TEST_MED)), 2_000)
        makeNotificationExpanded(device, TEST_MED)
        internalAssert(
            device.wait(
                Until.hasObject(By.res(packageName, "takenButton")),
                2000
            )
        )
        internalAssert(
            device.wait(
                Until.hasObject(By.res(packageName, "skippedButton")),
                2000
            )
        )
        internalAssert(
            device.wait(
                Until.hasObject(By.res(packageName, "snoozeButton")),
                2000
            )
        )
        internalAssert(
            device.findObject(By.text(getNotificationText(R.string.all_taken, "$$").replace("($$)", ""))) == null
        )
    }

    @Test
    //@AllowFlaky(attempts = 1)
    fun sameTimeReminders() {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        AndroidTestHelper.createMedicine(TEST_MED)
        val notificationTime = AndroidTestHelper.getNextNotificationTime().toLocalTime()
        val notificationTimeString =
            TimeHelper.minutesToTimeString(InstrumentationRegistry.getInstrumentation().targetContext, notificationTime.hour * 60L + notificationTime.minute)

        AndroidTestHelper.createReminder(
            "1",
            notificationTime
        )
        AndroidTestHelper.createReminder(
            SECOND_ONE,
            notificationTime
        )

        device.openNotification()
        sleep(2_000)
        val notification = device.wait(Until.findObject(By.textContains(SECOND_ONE)), 240_000)
        assertNotNull(notification)

        clickNotificationButton(device, SECOND_ONE, getNotificationText(R.string.all_taken, notificationTimeString))
        device.pressBack()

        navigateTo(MainMenu.OVERVIEW)
        assertCustomAssertionAtPosition(
            R.id.reminders,
            0,
            R.id.stateButton,
            matches(withTagValue(equalTo(R.drawable.check2_circle)))
        )
        assertCustomAssertionAtPosition(
            R.id.reminders,
            1,
            R.id.stateButton,
            matches(withTagValue(equalTo(R.drawable.check2_circle)))
        )
    }

    @Test
    //@AllowFlaky(attempts = 1)
    fun automaticallyTakenTest() {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        AndroidTestHelper.createMedicine(TEST_MED)
        val notificationTime = AndroidTestHelper.getNextNotificationTime().toLocalTime()

        AndroidTestHelper.createReminder("1", notificationTime)
        clickOn(R.id.openAdvancedSettings)
        clickOn(R.id.automaticallyTaken)
        pressBack()

        navigateTo(MainMenu.OVERVIEW)
        assertCustomAssertionAtPosition(
            R.id.reminders,
            0,
            R.id.stateButton,
            matches(withTagValue(equalTo(R.drawable.alarm)))
        )

        device.wait(Until.findObject(By.desc(InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.taken))), 180_000)
    }


    private fun getNotificationText(stringId: Int, vararg args: Any): String {
        val s = InstrumentationRegistry.getInstrumentation().targetContext.getString(stringId, *args)
        return if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            s.uppercase()
        } else {
            s
        }
    }

    private fun waitAndDismissNotification(device: UiDevice, timeout: Long = 2000) {
        device.openNotification()
        val notification = device.wait(Until.findObject(By.textContains(TEST_MED)), timeout)
        assertNotNull(notification)
        dismissNotification(notification, device)

        device.pressBack()
    }

    private fun dismissNotification(notification: UiObject2, device: UiDevice) {
        notification.fling(Direction.RIGHT)
        val notification = device.wait(Until.findObject(By.textContains(TEST_MED)), 500)
        notification?.fling(Direction.RIGHT)
    }
}
