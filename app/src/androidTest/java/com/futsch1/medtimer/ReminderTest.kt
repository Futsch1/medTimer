package com.futsch1.medtimer

import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withTagValue
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject
import androidx.test.uiautomator.UiSelector
import com.adevinta.android.barista.assertion.BaristaListAssertions.assertCustomAssertionAtPosition
import com.adevinta.android.barista.assertion.BaristaListAssertions.assertDisplayedAtPosition
import com.adevinta.android.barista.assertion.BaristaListAssertions.assertListItemCount
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertContains
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertNotContains
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import com.adevinta.android.barista.interaction.BaristaEditTextInteractions.writeTo
import com.adevinta.android.barista.interaction.BaristaKeyboardInteractions.closeKeyboard
import com.adevinta.android.barista.interaction.BaristaListInteractions.clickListItem
import com.adevinta.android.barista.interaction.BaristaListInteractions.clickListItemChild
import com.adevinta.android.barista.interaction.BaristaMenuClickInteractions.openMenu
import com.adevinta.android.barista.rule.flaky.AllowFlaky
import com.futsch1.medtimer.AndroidTestHelper.MainMenu
import com.futsch1.medtimer.core.ui.R
import com.futsch1.medtimer.utilities.clickDialogPositiveButton
import org.hamcrest.Matchers.equalTo
import org.junit.Test
import java.text.DateFormat
import java.time.Instant
import java.time.LocalTime
import java.util.Calendar


class ReminderTest : BaseTestHelper() {
    @Test
    @AllowFlaky(attempts = 3)
    fun activeReminderTest() {
        val futureTime = Calendar.getInstance()
        val year = futureTime.get(Calendar.YEAR)
        futureTime.set(year + 1, 1, 1)
        val pastTime = Calendar.getInstance()
        pastTime.set(year - 1, 1, 1)

        AndroidTestHelper.createMedicine("Test")
        AndroidTestHelper.createReminder("1", LocalTime.of(20, 0))

        clickOn(com.futsch1.medtimer.feature.ui.impl.R.id.openAdvancedSettings)
        clickOn(R.string.reminder_enabled)

        pressBack()
        pressBack()
        AndroidTestHelper.navigateTo(MainMenu.OVERVIEW)
        assertListItemCount(com.futsch1.medtimer.feature.ui.impl.R.id.reminders, 0)

        AndroidTestHelper.navigateTo(MainMenu.MEDICINES)
        AndroidTestHelper.clickMedicineItem(0)

        clickOn(com.futsch1.medtimer.feature.ui.impl.R.id.openAdvancedSettings)
        clickOn(R.string.reminder_enabled)

        pressBack()
        pressBack()
        AndroidTestHelper.navigateTo(MainMenu.OVERVIEW)
        assertListItemCount(com.futsch1.medtimer.feature.ui.impl.R.id.reminders, 1)

        AndroidTestHelper.navigateTo(MainMenu.MEDICINES)
        AndroidTestHelper.clickMedicineItem(0)

        clickOn(com.futsch1.medtimer.feature.ui.impl.R.id.openAdvancedSettings)
        clickOn(R.string.reminder_status)
        clickOn(R.string.period_start)
        clickOn(R.string.start_date)
        AndroidTestHelper.setDate(futureTime.getTime())

        pressBack()
        pressBack()
        pressBack()
        AndroidTestHelper.navigateTo(MainMenu.OVERVIEW)
        assertListItemCount(com.futsch1.medtimer.feature.ui.impl.R.id.reminders, 0)

        AndroidTestHelper.navigateTo(MainMenu.MEDICINES)
        AndroidTestHelper.clickMedicineItem(0)
        clickOn(com.futsch1.medtimer.feature.ui.impl.R.id.openAdvancedSettings)
        clickOn(R.string.reminder_status)
        clickOn(R.string.period_end)
        clickOn(R.string.end_date)
        AndroidTestHelper.setDate(pastTime.getTime())

        pressBack()
        pressBack()
        pressBack()
        AndroidTestHelper.navigateTo(MainMenu.OVERVIEW)
        assertListItemCount(com.futsch1.medtimer.feature.ui.impl.R.id.reminders, 0)

        AndroidTestHelper.navigateTo(MainMenu.MEDICINES)
        AndroidTestHelper.clickMedicineItem(0)
        clickOn(com.futsch1.medtimer.feature.ui.impl.R.id.openAdvancedSettings)
        clickOn(R.string.reminder_status)
        clickOn(R.string.start_date)
        AndroidTestHelper.setDate(pastTime.getTime())
        clickOn(R.string.end_date)
        AndroidTestHelper.setDate(futureTime.getTime())

        pressBack()
        pressBack()
        pressBack()
        AndroidTestHelper.navigateTo(MainMenu.OVERVIEW)
        assertListItemCount(com.futsch1.medtimer.feature.ui.impl.R.id.reminders, 1)
    }

    @Test
    @AllowFlaky(attempts = 3)
    fun activeIntervalReminderTest() {
        val futureTime = Calendar.getInstance()
        val year = futureTime.get(Calendar.YEAR)
        futureTime.set(year + 1, 1, 1)
        val nowTime = Calendar.getInstance()

        AndroidTestHelper.createMedicine("Test")
        AndroidTestHelper.createIntervalReminder("1", 180)

        clickOn(com.futsch1.medtimer.feature.ui.impl.R.id.openAdvancedSettings)
        clickOn(R.string.interval_start_time)
        AndroidTestHelper.setDate(futureTime.getTime())
        AndroidTestHelper.setTime(
            futureTime.get(Calendar.HOUR_OF_DAY),
            futureTime.get(Calendar.MINUTE),
            false
        )
        clickOn(R.string.reminder_enabled)

        pressBack()
        pressBack()

        AndroidTestHelper.clickMedicineItem(0)
        openMenu()
        clickOn(R.string.activate_all)

        clickOn(com.futsch1.medtimer.feature.ui.impl.R.id.openAdvancedSettings)
        assertContains(DateFormat.getDateInstance(DateFormat.SHORT).format(nowTime.getTime()))
    }

    @Test
    @AllowFlaky(attempts = 3)
    fun deleteLinkedReminderTest() {
        AndroidTestHelper.createMedicine("Test med")
        AndroidTestHelper.createReminder("1", LocalTime.of(0, 0))

        clickOn(com.futsch1.medtimer.feature.ui.impl.R.id.openAdvancedSettings)

        clickOn(R.string.add_linked_reminder)
        clickDialogPositiveButton()
        AndroidTestHelper.setTime(0, 1, true)

        clickListItemChild(
            com.futsch1.medtimer.feature.ui.impl.R.id.reminderList,
            1,
            com.futsch1.medtimer.feature.ui.impl.R.id.openAdvancedSettings
        )

        clickOn(R.string.add_linked_reminder)
        clickDialogPositiveButton()
        AndroidTestHelper.setTime(0, 2, true)

        clickListItemChild(
            com.futsch1.medtimer.feature.ui.impl.R.id.reminderList,
            0,
            com.futsch1.medtimer.feature.ui.impl.R.id.openAdvancedSettings
        )

        openMenu()
        clickOn(R.string.delete)
        clickDialogPositiveButton()

        // Check that the reminder list is empty
        assertListItemCount(com.futsch1.medtimer.feature.ui.impl.R.id.reminderList, 0)
    }

    @Test
    @AllowFlaky(attempts = 3)
    fun reminderTypeTest() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        AndroidTestHelper.createMedicine("Test")

        // Standard time based reminder (amount 1)
        val reminder1Time = LocalTime.now().plusMinutes(40)
        AndroidTestHelper.createReminder("1", reminder1Time)

        // Linked reminder (amount 2) 30 minutes later
        clickOn(com.futsch1.medtimer.feature.ui.impl.R.id.openAdvancedSettings)
        clickOn(R.string.add_linked_reminder)
        writeTo(android.R.id.input, "2")
        clickDialogPositiveButton()

        AndroidTestHelper.setTime(0, 30, true)

        // Interval reminder (amount 3) 2 hours from now
        clickOn(com.futsch1.medtimer.feature.ui.impl.R.id.addReminder)
        clickOn(com.futsch1.medtimer.feature.ui.impl.R.id.continuousIntervalCard)
        writeTo(com.futsch1.medtimer.feature.ui.impl.R.id.editAmount, "3")
        clickOn(com.futsch1.medtimer.feature.ui.impl.R.id.intervalHours)
        writeTo(com.futsch1.medtimer.feature.ui.impl.R.id.editIntervalTime, "2")
        closeKeyboard()
        clickOn(com.futsch1.medtimer.feature.ui.impl.R.id.createReminder)

        // Windowed interval reminder (amount 4)
        clickOn(com.futsch1.medtimer.feature.ui.impl.R.id.addReminder)
        clickOn(com.futsch1.medtimer.feature.ui.impl.R.id.windowedIntervalCard)
        writeTo(com.futsch1.medtimer.feature.ui.impl.R.id.editAmount, "4")
        closeKeyboard()
        clickOn(com.futsch1.medtimer.feature.ui.impl.R.id.editIntervalDailyStartTime)
        AndroidTestHelper.setTime(20, 0, false)
        clickOn(com.futsch1.medtimer.feature.ui.impl.R.id.editIntervalDailyEndTime)
        AndroidTestHelper.setTime(23, 30, false)
        clickOn(com.futsch1.medtimer.feature.ui.impl.R.id.intervalHours)
        writeTo(com.futsch1.medtimer.feature.ui.impl.R.id.editIntervalTime, "3")
        closeKeyboard()
        clickOn(com.futsch1.medtimer.feature.ui.impl.R.id.createReminder)

        // Check calendar view not crashing
        clickOn(com.futsch1.medtimer.feature.ui.impl.R.id.openCalendar)
        pressBack()

        var expectedString = context.getString(
            R.string.every_interval,
            "2 " + context.resources.getQuantityString(R.plurals.hours, 2)
        )
        assertCustomAssertionAtPosition(
            com.futsch1.medtimer.feature.ui.impl.R.id.reminderList,
            0,
            com.futsch1.medtimer.feature.ui.impl.R.id.reminderCardLayout,
            matches(ViewMatchers.withChild(ViewMatchers.withSubstring(expectedString)))
        )

        expectedString = timeFormatter().minutesToTimeString(reminder1Time.toSecondOfDay() / 60)
        assertDisplayedAtPosition(
            com.futsch1.medtimer.feature.ui.impl.R.id.reminderList,
            1,
            com.futsch1.medtimer.feature.ui.impl.R.id.editReminderTime,
            expectedString
        )

        expectedString = context.getString(
            R.string.linked_reminder_summary,
            timeFormatter().toTimeString(reminder1Time)
        )
        assertDisplayedAtPosition(
            com.futsch1.medtimer.feature.ui.impl.R.id.reminderList,
            2,
            com.futsch1.medtimer.feature.ui.impl.R.id.reminderCardLayout,
            expectedString
        )

        expectedString = context.getString(
            R.string.every_interval,
            "3 " + context.resources.getQuantityString(R.plurals.hours, 3)
        )
        assertCustomAssertionAtPosition(
            com.futsch1.medtimer.feature.ui.impl.R.id.reminderList,
            3,
            com.futsch1.medtimer.feature.ui.impl.R.id.reminderCardLayout,
            matches(ViewMatchers.withChild(ViewMatchers.withSubstring(expectedString)))
        )

        clickListItemChild(
            com.futsch1.medtimer.feature.ui.impl.R.id.reminderList,
            0,
            com.futsch1.medtimer.feature.ui.impl.R.id.openAdvancedSettings
        )
        pressBack()
        clickListItemChild(
            com.futsch1.medtimer.feature.ui.impl.R.id.reminderList,
            1,
            com.futsch1.medtimer.feature.ui.impl.R.id.openAdvancedSettings
        )
        pressBack()
        clickListItemChild(
            com.futsch1.medtimer.feature.ui.impl.R.id.reminderList,
            2,
            com.futsch1.medtimer.feature.ui.impl.R.id.openAdvancedSettings
        )
        pressBack()
        clickListItemChild(
            com.futsch1.medtimer.feature.ui.impl.R.id.reminderList,
            3,
            com.futsch1.medtimer.feature.ui.impl.R.id.openAdvancedSettings
        )
        pressBack()

        // Check overview and next reminders
        AndroidTestHelper.navigateTo(MainMenu.OVERVIEW)

        assertContains(com.futsch1.medtimer.feature.ui.impl.R.id.reminderText, "Test (1)")
        expectedString = timeFormatter().minutesToTimeString(reminder1Time.toSecondOfDay() / 60)
        assertContains(com.futsch1.medtimer.feature.ui.impl.R.id.reminderText, expectedString)

        assertContains(com.futsch1.medtimer.feature.ui.impl.R.id.reminderText, "Test (3)")

        assertContains(com.futsch1.medtimer.feature.ui.impl.R.id.reminderText, "Test (4)")

        // If possible, take reminder 1 now and see if reminder 2 appears
        clickListItemChild(
            com.futsch1.medtimer.feature.ui.impl.R.id.reminders,
            1,
            com.futsch1.medtimer.feature.ui.impl.R.id.stateButton
        )
        clickOn(com.futsch1.medtimer.feature.ui.impl.R.id.takenButton)

        assertContains("Test (2)")
    }

    @Test
    @AllowFlaky(attempts = 3)
    fun editReminderTest() {
        AndroidTestHelper.createMedicine("Test")

        AndroidTestHelper.navigateTo(MainMenu.OVERVIEW)

        clickOn(com.futsch1.medtimer.feature.ui.impl.R.id.logManualDose)

        clickListItem(position = 1)

        writeTo(android.R.id.input, "12")
        clickDialogPositiveButton()
        val now = Instant.now()
        clickOn(com.google.android.material.R.id.material_timepicker_ok_button)

        clickListItemChild(
            com.futsch1.medtimer.feature.ui.impl.R.id.reminders,
            0,
            com.futsch1.medtimer.feature.ui.impl.R.id.overviewContentContainer
        )
        assertContains(com.futsch1.medtimer.feature.ui.impl.R.id.editEventName, "Test")
        assertContains(com.futsch1.medtimer.feature.ui.impl.R.id.editEventAmount, "12")
        assertContains(
            com.futsch1.medtimer.feature.ui.impl.R.id.editEventRemindedTimestamp,
            timeFormatter().toTimeString(now)
        )
        assertContains(
            com.futsch1.medtimer.feature.ui.impl.R.id.editEventRemindedDate,
            timeFormatter().toDateString(now)
        )
        assertContains(
            com.futsch1.medtimer.feature.ui.impl.R.id.editEventTakenTimestamp,
            timeFormatter().toTimeString(now)
        )
        assertContains(
            com.futsch1.medtimer.feature.ui.impl.R.id.editEventTakenDate,
            timeFormatter().toDateString(now)
        )
        assertContains(com.futsch1.medtimer.feature.ui.impl.R.id.editEventNotes, "")

        clickOn(R.string.skipped)
        writeTo(com.futsch1.medtimer.feature.ui.impl.R.id.editEventNotes, "Test notes")
        pressBack()

        assertCustomAssertionAtPosition(
            com.futsch1.medtimer.feature.ui.impl.R.id.reminders,
            0,
            com.futsch1.medtimer.feature.ui.impl.R.id.stateButton,
            matches(withTagValue(equalTo(R.drawable.x_circle)))
        )
        clickListItemChild(
            com.futsch1.medtimer.feature.ui.impl.R.id.reminders,
            0,
            com.futsch1.medtimer.feature.ui.impl.R.id.overviewContentContainer
        )
        clickOn(R.string.taken)
        assertContains(com.futsch1.medtimer.feature.ui.impl.R.id.editEventNotes, "Test notes")

        val newReminded = now.plusSeconds(60 * 60 * 24 + 120)
        writeTo(
            com.futsch1.medtimer.feature.ui.impl.R.id.editEventRemindedTimestamp,
            timeFormatter().toTimeString(newReminded)
        )
        writeTo(
            com.futsch1.medtimer.feature.ui.impl.R.id.editEventRemindedDate,
            timeFormatter().toDateString(newReminded)
        )

        val newTaken = now.plusSeconds(60 * 60 * 48 + 180)
        writeTo(
            com.futsch1.medtimer.feature.ui.impl.R.id.editEventTakenTimestamp,
            timeFormatter().toTimeString(newTaken)
        )
        writeTo(
            com.futsch1.medtimer.feature.ui.impl.R.id.editEventTakenDate,
            timeFormatter().toDateString(newTaken)
        )

        pressBack()

        AndroidTestHelper.navigateTo(MainMenu.ANALYSIS)

        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        // The Analysis view chips are icon-only; their labels are exposed as content descriptions.
        device.findObject(By.desc(context.getString(R.string.tabular_view)))?.click()
        AndroidTestHelper.waitForIdle(1_000)

        internalAssert(
            device.findObject(
                By.textContains(
                    timeFormatter().toDateTimeString(
                        newReminded
                    )
                )
            ) != null
        )
        internalAssert(device.findObject(By.textContains(timeFormatter().toDateTimeString(newTaken))) != null)
    }

    @Test
    @AllowFlaky(attempts = 3)
    fun deleteReminderTest() {
        AndroidTestHelper.createMedicine("Test")
        AndroidTestHelper.createReminder("1", LocalTime.of(20, 0))

        AndroidTestHelper.navigateTo(MainMenu.OVERVIEW)

        clickListItemChild(
            com.futsch1.medtimer.feature.ui.impl.R.id.reminders,
            0,
            com.futsch1.medtimer.feature.ui.impl.R.id.stateButton
        )
        clickOn(com.futsch1.medtimer.feature.ui.impl.R.id.takenButton)

        clickListItemChild(
            com.futsch1.medtimer.feature.ui.impl.R.id.reminders,
            0,
            com.futsch1.medtimer.feature.ui.impl.R.id.stateButton
        )
        clickOn(com.futsch1.medtimer.feature.ui.impl.R.id.deleteButton)
        clickDialogPositiveButton()

        assertListItemCount(com.futsch1.medtimer.feature.ui.impl.R.id.reminders, 0)
    }

    @Test
    @AllowFlaky(attempts = 3)
    fun intervalReminderTest() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        AndroidTestHelper.createMedicine("Test")
        AndroidTestHelper.createIntervalReminder("1", 10)
        pressBack()

        AndroidTestHelper.navigateTo(MainMenu.OVERVIEW)

        clickListItemChild(
            com.futsch1.medtimer.feature.ui.impl.R.id.reminders,
            0,
            com.futsch1.medtimer.feature.ui.impl.R.id.stateButton
        )
        clickOn(com.futsch1.medtimer.feature.ui.impl.R.id.takenButton)

        assertNotContains(context.getString(R.string.interval_time, "0 min"))

        clickListItemChild(
            com.futsch1.medtimer.feature.ui.impl.R.id.reminders,
            1,
            com.futsch1.medtimer.feature.ui.impl.R.id.stateButton
        )
        clickOn(com.futsch1.medtimer.feature.ui.impl.R.id.takenButton)

        AndroidTestHelper.waitForText(context.getString(R.string.interval_time, "0 min"))
        assertContains(context.getString(R.string.interval_time, "0 min"))
    }

    @Test
    @AllowFlaky(attempts = 3)
    fun cyclicReminderTest() {
        val reminders: Array<CyclicReminderInfo?> = arrayOf(
            CyclicReminderInfo(1, 0, false),
            CyclicReminderInfo(1, 1, false),
            CyclicReminderInfo(1, 2, false),
            CyclicReminderInfo(2, 0, false),
            CyclicReminderInfo(2, 1, true),
        )

        // Create medicine
        AndroidTestHelper.createMedicine("Test")

        for (reminder in reminders) {
            // Create reminder
            AndroidTestHelper.createReminder("1", LocalTime.of(20, 0))

            // Set active and pause days
            clickOn(com.futsch1.medtimer.feature.ui.impl.R.id.openAdvancedSettings)
            clickOn(R.string.cycle_reminder)
            clickOn(R.string.cycle_consecutive_days)
            AndroidTestHelper.setValue(reminder!!.consecutiveDays.toString())
            clickOn(R.string.cycle_pause_days)
            AndroidTestHelper.setValue(reminder.pauseDays.toString())

            // Set cycle start date of the reminder
            val cycleStart = Calendar.getInstance()
            // The month here is 7, not 8, since it is zero-indexed (so January is 0)
            cycleStart.set(2025, 7, 1)
            clickOn(R.string.cycle_start_date)
            AndroidTestHelper.setDate(cycleStart.getTime())

            // Go back to medicines list
            AndroidTestHelper.navigateTo(MainMenu.MEDICINES)

            // Mark event as taken
            AndroidTestHelper.navigateTo(MainMenu.OVERVIEW)
            clickListItemChild(
                com.futsch1.medtimer.feature.ui.impl.R.id.reminders,
                0,
                com.futsch1.medtimer.feature.ui.impl.R.id.stateButton
            )
            clickOn(com.futsch1.medtimer.feature.ui.impl.R.id.takenButton)

            // Check if cyclic information is present
            clickListItemChild(
                com.futsch1.medtimer.feature.ui.impl.R.id.reminders,
                0,
                com.futsch1.medtimer.feature.ui.impl.R.id.overviewContentContainer
            )
            if (reminder.shouldHaveInfo) {
                assertContains(
                    com.futsch1.medtimer.feature.ui.impl.R.id.editEventName,
                    String.format("Test (1/%d)", reminder.consecutiveDays)
                )
            } else {
                assertNotContains(com.futsch1.medtimer.feature.ui.impl.R.id.editEventName, "Test (")
                assertContains(com.futsch1.medtimer.feature.ui.impl.R.id.editEventName, "Test")
            }
            pressBack()

            // Remove event
            clickListItemChild(
                com.futsch1.medtimer.feature.ui.impl.R.id.reminders,
                0,
                com.futsch1.medtimer.feature.ui.impl.R.id.stateButton
            )
            clickOn(com.futsch1.medtimer.feature.ui.impl.R.id.deleteButton)
            clickDialogPositiveButton()

            // Remove reminder
            AndroidTestHelper.navigateTo(MainMenu.MEDICINES)
            AndroidTestHelper.clickMedicineItem(0)
            clickOn(com.futsch1.medtimer.feature.ui.impl.R.id.openAdvancedSettings)
            openMenu()
            clickOn(R.string.delete)
            clickDialogPositiveButton()
        }
    }

    @Test
    @AllowFlaky(attempts = 3)
    fun weekendMode() {
        openMenu()
        clickOn(R.string.tab_settings)
        clickOn(R.string.weekend_mode)
        clickOn(R.string.active)
        clickOn(R.string.days_string)
        clickOn(R.string.friday)
        clickDialogPositiveButton()
        clickOn(R.string.weekend_start_time)
        AndroidTestHelper.setTime(19, 0, false)
        clickOn(R.string.weekend_end_time)
        AndroidTestHelper.setTime(21, 0, false)

        AndroidTestHelper.createMedicine(TEST_MED)
        AndroidTestHelper.createReminder("1", LocalTime.of(20, 0))

        AndroidTestHelper.navigateTo(MainMenu.OVERVIEW)
        assertContains(timeFormatter().minutesToTimeString(21 * 60))
    }

    @Test
    @AllowFlaky(attempts = 3)
    fun reschedule() {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        openMenu()
        clickOn(R.string.tab_settings)
        clickOn(R.string.display_settings)
        clickOn(R.string.combine_notifications)
        pressBack()
        pressBack()

        AndroidTestHelper.createMedicine(TEST_MED)
        AndroidTestHelper.createIntervalReminder("1", 60)

        AndroidTestHelper.navigateTo(MainMenu.OVERVIEW)
        AndroidTestHelper.waitForIdle(2_000)
        AndroidTestHelper.longClickListItem(com.futsch1.medtimer.feature.ui.impl.R.id.reminders, 0)
        clickListItem(com.futsch1.medtimer.feature.ui.impl.R.id.reminders, 1)

        try {
            clickOn(com.futsch1.medtimer.feature.ui.impl.R.id.rescheduleButton)
        } catch (_: Exception) {
            val menuButton: UiObject =
                device.findObject(UiSelector().description("More options"))
            menuButton.click()
            clickOn(R.string.reschedule_reminder)
        }

        AndroidTestHelper.setTime(19, 0, false)

        val timeString = timeFormatter().minutesToTimeString(19 * 60)
        assertCustomAssertionAtPosition(
            com.futsch1.medtimer.feature.ui.impl.R.id.reminders,
            0,
            com.futsch1.medtimer.feature.ui.impl.R.id.stateButton,
            matches(withTagValue(equalTo(R.drawable.alarm)))
        )
        assertCustomAssertionAtPosition(
            com.futsch1.medtimer.feature.ui.impl.R.id.reminders,
            0,
            com.futsch1.medtimer.feature.ui.impl.R.id.reminderText,
            matches(ViewMatchers.withSubstring(timeString))
        )

        assertCustomAssertionAtPosition(
            com.futsch1.medtimer.feature.ui.impl.R.id.reminders,
            1,
            com.futsch1.medtimer.feature.ui.impl.R.id.stateButton,
            matches(withTagValue(equalTo(R.drawable.alarm)))
        )
        assertCustomAssertionAtPosition(
            com.futsch1.medtimer.feature.ui.impl.R.id.reminders,
            1,
            com.futsch1.medtimer.feature.ui.impl.R.id.reminderText,
            matches(ViewMatchers.withSubstring(timeString))
        )

        AndroidTestHelper.longClickListItem(com.futsch1.medtimer.feature.ui.impl.R.id.reminders, 0)
        assertDisplayed("2")
        clickOn(com.futsch1.medtimer.feature.ui.impl.R.id.takenButton)

        assertCustomAssertionAtPosition(
            com.futsch1.medtimer.feature.ui.impl.R.id.reminders,
            0,
            com.futsch1.medtimer.feature.ui.impl.R.id.stateButton,
            matches(withTagValue(equalTo(R.drawable.check2_circle)))
        )
        assertCustomAssertionAtPosition(
            com.futsch1.medtimer.feature.ui.impl.R.id.reminders,
            0,
            com.futsch1.medtimer.feature.ui.impl.R.id.reminderText,
            matches(ViewMatchers.withSubstring(timeString))
        )

        assertCustomAssertionAtPosition(
            com.futsch1.medtimer.feature.ui.impl.R.id.reminders,
            1,
            com.futsch1.medtimer.feature.ui.impl.R.id.stateButton,
            matches(withTagValue(equalTo(R.drawable.check2_circle)))
        )
        assertCustomAssertionAtPosition(
            com.futsch1.medtimer.feature.ui.impl.R.id.reminders,
            1,
            com.futsch1.medtimer.feature.ui.impl.R.id.reminderText,
            matches(ViewMatchers.withSubstring(timeString))
        )
    }

    private class CyclicReminderInfo(
        var consecutiveDays: Int,
        var pauseDays: Int,
        var shouldHaveInfo: Boolean
    )
}
