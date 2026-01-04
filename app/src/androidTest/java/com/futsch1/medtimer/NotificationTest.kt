package com.futsch1.medtimer

import android.content.Context
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
import androidx.test.uiautomator.StaleObjectException
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import com.adevinta.android.barista.assertion.BaristaListAssertions.assertCustomAssertionAtPosition
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertContains
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import com.adevinta.android.barista.interaction.BaristaDialogInteractions.clickDialogPositiveButton
import com.adevinta.android.barista.interaction.BaristaEditTextInteractions.writeTo
import com.adevinta.android.barista.interaction.BaristaListInteractions.clickListItemChild
import com.adevinta.android.barista.interaction.BaristaMenuClickInteractions.openMenu
import com.adevinta.android.barista.interaction.BaristaSleepInteractions.sleep
import com.adevinta.android.barista.rule.flaky.AllowFlaky
import com.futsch1.medtimer.AndroidTestHelper.MainMenu
import com.futsch1.medtimer.AndroidTestHelper.navigateTo
import com.futsch1.medtimer.reminders.ReminderProcessor
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.equalTo
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.Locale


const val TEST_MED = "Test med"

private const val SECOND_ONE = "second one"
private const val FIRST_REMINDER = "First reminder"
private const val SECOND_REMINDER = "Second reminder"
private const val TEST_VARIABLE_AMOUNT = "Test variable amount"
private const val TEST_ANOTHER_VARIABLE_AMOUNT = "Test another variable amount"


fun clickNotificationButton(device: UiDevice, buttonText: String): Boolean {
    val button = makeNotificationExpanded(device, buttonText)
    button?.click()
    return button != null
}

fun makeNotificationExpanded(device: UiDevice, buttonText: String): UiObject2? {
    var buttonIndex = 0
    var tries = 10
    var button: UiObject2? = null
    while (tries-- > 0 && button == null) {
        val expandButtons = device.findObjects(By.res("android:id/expand_button")) + device.findObjects(By.descContains("Expand"))
        if (expandButtons.size > buttonIndex) {
            expandButtons[buttonIndex++].click()
        }
        button = device.findObject(By.text(buttonText))
    }
    return button
}

fun getNotificationText(stringId: Int): String {
    val s = InstrumentationRegistry.getInstrumentation().targetContext.getString(stringId)
    return if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
        s.uppercase()
    } else {
        s
    }
}


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

        clickOn(R.string.add_linked_reminder)
        clickDialogPositiveButton()
        AndroidTestHelper.setTime(0, 1, true)

        navigateTo(MainMenu.OVERVIEW)

        baristaRule.activityTestRule.finishActivity()

        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        ReminderProcessor.requestRescheduleNowForTests(InstrumentationRegistry.getInstrumentation().context)
        waitAndDismissNotification(device, 2_000)
        ReminderProcessor.requestRescheduleNowForTests(InstrumentationRegistry.getInstrumentation().context)
        waitAndDismissNotification(device, 2_000)
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

        clickOn(R.string.display_settings)
        clickOn(R.string.combine_notifications)
        pressBack()
        pressBack()

        AndroidTestHelper.createMedicine(TEST_MED)
        AndroidTestHelper.createReminder(FIRST_REMINDER, LocalTime.of(22, 0))
        AndroidTestHelper.createReminder(SECOND_REMINDER, LocalTime.of(22, 0))
        pressBack()

        ReminderProcessor.requestRescheduleNowForTests(InstrumentationRegistry.getInstrumentation().context, 0)

        device.openNotification()
        var notification = device.wait(Until.findObject(By.textContains(TEST_MED)), 2_000)
        assertNotNull(notification)
        notification = device.wait(Until.findObject(By.textContains(FIRST_REMINDER)), 1_000)
        assertNotNull(notification)
        notification = device.wait(Until.findObject(By.textContains(SECOND_REMINDER)), 1_000)
        assertNotNull(notification)
        device.pressBack()
        navigateTo(MainMenu.OVERVIEW)

        clickListItemChild(R.id.reminders, 0, R.id.stateButton)
        clickOn(R.id.takenButton)
        navigateTo(MainMenu.ANALYSIS)

        device.openNotification()
        notification = device.wait(Until.findObject(By.textContains(FIRST_REMINDER)), 1_000)
        assertNull(notification)
        notification = device.wait(Until.findObject(By.textContains(SECOND_REMINDER)), 1_000)
        assertNotNull(notification)
        val text = notification.text

        device.wait(Until.gone(By.text(text)), 120_000)
        val nextNotification = device.wait(Until.findObject(By.textContains(TEST_MED)), 2_000)
        assertNotNull(nextNotification)
        notification = device.wait(Until.findObject(By.textContains(FIRST_REMINDER)), 1_000)
        assertNull(notification)
        notification = device.wait(Until.findObject(By.textContains(SECOND_REMINDER)), 1_000)
        assertNotNull(notification)

        device.pressBack()
    }

    @Test
    @AllowFlaky(attempts = 1)
    fun variableAmount() {
        openMenu()
        clickOn(R.string.tab_settings)
        clickOn(R.string.display_settings)
        clickOn(R.string.combine_notifications)
        pressBack()

        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        AndroidTestHelper.createMedicine(TEST_MED)
        AndroidTestHelper.createReminder("1", LocalTime.of(20, 0))
        clickOn(R.id.openAdvancedSettings)
        clickOn(R.string.variable_amount)
        pressBack()

        AndroidTestHelper.createMedicine(SECOND_ONE)
        AndroidTestHelper.createReminder("1", LocalTime.of(20, 0))
        clickOn(R.id.openAdvancedSettings)
        clickOn(R.string.variable_amount)
        pressBack()

        device.openNotification()
        ReminderProcessor.requestRescheduleNowForTests(InstrumentationRegistry.getInstrumentation().context, 0)
        device.wait(Until.findObject(By.textContains(TEST_MED)), 2_000)
        clickNotificationButton(device, getNotificationText(R.string.taken))

        var input = device.wait(Until.findObject(By.hintContains(TEST_MED)), 2_000)
        input.text = TEST_VARIABLE_AMOUNT
        device.findObject(By.res("com.android.systemui:id/remote_input_send")).click()
        device.wait(Until.gone(By.textContains(TEST_MED)), 2_000)

        clickNotificationButton(device, getNotificationText(R.string.taken))
        input = device.wait(Until.findObject(By.hintContains(SECOND_ONE)), 2_000)
        input.text = TEST_ANOTHER_VARIABLE_AMOUNT
        device.findObject(By.res("com.android.systemui:id/remote_input_send")).click()
        device.wait(Until.gone(By.textContains(SECOND_ONE)), 2_000)

        AndroidTestHelper.closeNotifications(device)

        navigateTo(MainMenu.OVERVIEW)
        assertContains(TEST_VARIABLE_AMOUNT)
        assertContains(TEST_ANOTHER_VARIABLE_AMOUNT)
    }

    @Test
    //@AllowFlaky(attempts = 1)
    fun variableAmountBigButton() {
        openMenu()
        clickOn(R.string.tab_settings)
        clickOn(R.string.display_settings)
        clickOn(R.string.big_notifications)
        clickOn(R.string.combine_notifications)
        pressBack()

        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        AndroidTestHelper.createMedicine(TEST_MED)
        AndroidTestHelper.createReminder("1", LocalTime.of(20, 0))
        clickOn(R.id.openAdvancedSettings)
        clickOn(R.string.variable_amount)
        pressBack()
        AndroidTestHelper.createReminder("Not variable", LocalTime.of(20, 0))

        AndroidTestHelper.createMedicine(SECOND_ONE)
        AndroidTestHelper.createReminder("1", LocalTime.of(20, 0))
        clickOn(R.id.openAdvancedSettings)
        clickOn(R.string.variable_amount)
        pressBack()

        navigateTo(MainMenu.ANALYSIS)

        device.openNotification()
        ReminderProcessor.requestRescheduleNowForTests(InstrumentationRegistry.getInstrumentation().context, 0)
        device.wait(Until.findObject(By.textContains(TEST_MED)), 2_000)
        clickNotificationButton(device, InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.taken))

        device.wait(Until.findObject(By.displayId(android.R.id.input)), 2_000)
        assertContains(TEST_MED)
        writeTo(android.R.id.input, TEST_VARIABLE_AMOUNT)
        clickDialogPositiveButton()

        device.wait(Until.findObject(By.displayId(android.R.id.input)), 2_000)
        assertContains(SECOND_ONE)
        writeTo(android.R.id.input, TEST_ANOTHER_VARIABLE_AMOUNT)
        clickDialogPositiveButton()

        navigateTo(MainMenu.OVERVIEW)
        assertContains(TEST_VARIABLE_AMOUNT)
        assertContains(TEST_ANOTHER_VARIABLE_AMOUNT)
        assertContains("Not variable")

        val nextDay = DayOfWeek.SATURDAY.getDisplayName(TextStyle.SHORT, Locale.getDefault()) + "\n2"
        clickOn(nextDay)
        clickListItemChild(R.id.reminders, 0, R.id.stateButton)
        clickOn(R.id.takenButton)
        writeTo(android.R.id.input, "Test variable amount again")
        clickDialogPositiveButton()

        assertContains("Test variable amount again")
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
        ReminderProcessor.requestRescheduleNowForTests(InstrumentationRegistry.getInstrumentation().context)
        val notification = device.wait(Until.findObject(By.textContains(TEST_MED)), 2_000)
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
    }

    @Test
    //@AllowFlaky(attempts = 1)
    fun sameTimeReminders() {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        openMenu()
        clickOn(R.string.tab_settings)
        clickOn(R.string.display_settings)
        clickOn(R.string.combine_notifications)

        AndroidTestHelper.createMedicine(TEST_MED)
        val notificationTime = AndroidTestHelper.getNextNotificationTime().toLocalTime()

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
        ReminderProcessor.requestRescheduleNowForTests(InstrumentationRegistry.getInstrumentation().context, 0, 0)
        device.wait(Until.findObject(By.textContains(TEST_MED)), 2_000)
        val notification = device.wait(Until.findObject(By.textContains(SECOND_ONE)), 2_000)
        assertNotNull(notification)

        internalAssert(clickNotificationButton(device, getNotificationText(R.string.taken)))
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
        AndroidTestHelper.scrollDown()
        clickOn(R.string.automatically_taken)
        pressBack()

        navigateTo(MainMenu.OVERVIEW)
        assertCustomAssertionAtPosition(
            R.id.reminders,
            0,
            R.id.stateButton,
            matches(withTagValue(equalTo(R.drawable.alarm)))
        )

        ReminderProcessor.requestRescheduleNowForTests(InstrumentationRegistry.getInstrumentation().context)
        device.wait(Until.findObject(By.desc(InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.taken))), 2_000)

        device.openNotification()
        internalAssert(device.findObject(By.textContains(getNotificationText(R.string.taken))) == null)
        device.pressBack()
    }

    @Test
    //@AllowFlaky(attempts = 1)
    fun alarmTest() {
        val timeToNotify = 10_000L
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        device.wakeUp()

        AndroidTestHelper.createMedicine(TEST_MED)
        clickOn(R.id.notificationImportance)
        clickOn(R.string.high_and_alarm)
        AndroidTestHelper.createIntervalReminder("1", 2)

        navigateTo(MainMenu.OVERVIEW)
        clickListItemChild(R.id.reminders, 0, R.id.stateButton)
        clickOn(R.id.takenButton)

        sleep(2_000)

        device.sleep()

        ReminderProcessor.requestRescheduleNowForTests(context, timeToNotify, 0)

        var o = device.wait(Until.findObject(By.text(context.getString(R.string.snooze))), timeToNotify * 4)
        internalAssert(o != null)
        clickTakenOnAlarmScreen(device, context)

        assertCustomAssertionAtPosition(
            R.id.reminders,
            1,
            R.id.stateButton,
            matches(withTagValue(equalTo(R.drawable.check2_circle)))
        )

        sleep(2_000)

        device.sleep()

        ReminderProcessor.requestRescheduleNowForTests(context, timeToNotify, 0)
        o = device.wait(Until.findObject(By.text(context.getString(R.string.snooze))), timeToNotify * 4)
        internalAssert(o != null)
        ReminderProcessor.requestRescheduleNowForTests(context)
        clickTakenOnAlarmScreen(device, context)

        assertCustomAssertionAtPosition(
            R.id.reminders,
            2,
            R.id.stateButton,
            matches(withTagValue(equalTo(R.drawable.check2_circle)))
        )
        assertCustomAssertionAtPosition(
            R.id.reminders,
            3,
            R.id.stateButton,
            matches(withTagValue(equalTo(R.drawable.bell)))
        )
    }

    @Test
    //@AllowFlaky(attempts = 1)
    fun scheduleReminderTest() {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        AndroidTestHelper.createMedicine(TEST_MED)

        AndroidTestHelper.createReminder("1", LocalTime.of(22, 0))

        navigateTo(MainMenu.OVERVIEW)

        clickListItemChild(R.id.reminders, 0, R.id.stateButton)
        clickOn(R.string.reschedule_reminder)

        AndroidTestHelper.setTime(4, 0, false)

        device.openNotification()
        assertContains(TEST_MED)
        device.pressBack()
    }

    private fun clickTakenOnAlarmScreen(
        device: UiDevice,
        context: Context
    ) {
        val o1 = device.findObject(By.desc(context.getString(R.string.taken)))
        while (device.findObject(By.text(context.getString(R.string.snooze))) != null) {
            try {
                o1.click()
            } catch (_: StaleObjectException) {
                // Ignore
            }
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
