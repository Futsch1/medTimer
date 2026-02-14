package com.futsch1.medtimer

import android.widget.TextView
import androidx.test.espresso.Espresso
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.platform.app.InstrumentationRegistry
import com.adevinta.android.barista.assertion.BaristaListAssertions.assertCustomAssertionAtPosition
import com.adevinta.android.barista.assertion.BaristaListAssertions.assertDisplayedAtPosition
import com.adevinta.android.barista.assertion.BaristaListAssertions.assertListItemCount
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertContains
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertNotContains
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import com.adevinta.android.barista.interaction.BaristaDialogInteractions
import com.adevinta.android.barista.interaction.BaristaEditTextInteractions.writeTo
import com.adevinta.android.barista.interaction.BaristaKeyboardInteractions.closeKeyboard
import com.adevinta.android.barista.interaction.BaristaListInteractions.clickListItem
import com.adevinta.android.barista.interaction.BaristaListInteractions.clickListItemChild
import com.adevinta.android.barista.interaction.BaristaMenuClickInteractions.openMenu
import com.adevinta.android.barista.interaction.BaristaSleepInteractions
import com.adevinta.android.barista.rule.flaky.AllowFlaky
import com.evrencoskun.tableview.TableView
import com.futsch1.medtimer.AndroidTestHelper.MainMenu
import com.futsch1.medtimer.helpers.TimeHelper
import junit.framework.TestCase
import org.junit.Test
import java.text.DateFormat
import java.time.Instant
import java.time.LocalTime
import java.util.Calendar
import java.util.concurrent.atomic.AtomicReference


class ReminderTest : BaseTestHelper() {
    @Test //@AllowFlaky(attempts = 1)
    fun activeReminderTest() {
        val futureTime = Calendar.getInstance()
        val year = futureTime.get(Calendar.YEAR)
        futureTime.set(year + 1, 1, 1)
        val pastTime = Calendar.getInstance()
        pastTime.set(year - 1, 1, 1)

        AndroidTestHelper.createMedicine("Test")
        AndroidTestHelper.createReminder("1", LocalTime.of(20, 0))

        clickOn(R.id.openAdvancedSettings)
        clickOn(R.string.reminder_status)
        clickOn(R.string.active)

        Espresso.pressBack()
        Espresso.pressBack()
        Espresso.pressBack()
        AndroidTestHelper.navigateTo(MainMenu.OVERVIEW)
        assertListItemCount(R.id.reminders, 0)

        AndroidTestHelper.navigateTo(MainMenu.MEDICINES)
        clickListItem(R.id.medicineList, 0)

        clickOn(R.id.openAdvancedSettings)
        clickOn(R.string.reminder_status)
        clickOn(R.string.active)

        Espresso.pressBack()
        Espresso.pressBack()
        Espresso.pressBack()
        AndroidTestHelper.navigateTo(MainMenu.OVERVIEW)
        assertListItemCount(R.id.reminders, 1)

        AndroidTestHelper.navigateTo(MainMenu.MEDICINES)
        clickListItem(R.id.medicineList, 0)

        clickOn(R.id.openAdvancedSettings)
        clickOn(R.string.reminder_status)
        clickOn(R.string.period_start)
        clickOn(R.string.start_date)
        AndroidTestHelper.setDate(futureTime.getTime())

        Espresso.pressBack()
        Espresso.pressBack()
        Espresso.pressBack()
        AndroidTestHelper.navigateTo(MainMenu.OVERVIEW)
        assertListItemCount(R.id.reminders, 0)

        AndroidTestHelper.navigateTo(MainMenu.MEDICINES)
        clickListItem(R.id.medicineList, 0)
        clickOn(R.id.openAdvancedSettings)
        clickOn(R.string.reminder_status)
        clickOn(R.string.period_end)
        clickOn(R.string.end_date)
        AndroidTestHelper.setDate(pastTime.getTime())

        Espresso.pressBack()
        Espresso.pressBack()
        Espresso.pressBack()
        AndroidTestHelper.navigateTo(MainMenu.OVERVIEW)
        assertListItemCount(R.id.reminders, 0)

        AndroidTestHelper.navigateTo(MainMenu.MEDICINES)
        clickListItem(R.id.medicineList, 0)
        clickOn(R.id.openAdvancedSettings)
        clickOn(R.string.reminder_status)
        clickOn(R.string.start_date)
        AndroidTestHelper.setDate(pastTime.getTime())
        clickOn(R.string.end_date)
        AndroidTestHelper.setDate(futureTime.getTime())

        Espresso.pressBack()
        Espresso.pressBack()
        Espresso.pressBack()
        AndroidTestHelper.navigateTo(MainMenu.OVERVIEW)
        assertListItemCount(R.id.reminders, 1)
    }

    @Test //@AllowFlaky(attempts = 1)
    fun activeIntervalReminderTest() {
        val futureTime = Calendar.getInstance()
        val year = futureTime.get(Calendar.YEAR)
        futureTime.set(year + 1, 1, 1)
        val nowTime = Calendar.getInstance()

        AndroidTestHelper.createMedicine("Test")
        AndroidTestHelper.createIntervalReminder("1", 180)

        clickOn(R.id.openAdvancedSettings)
        clickOn(R.string.interval_start_time)
        AndroidTestHelper.setDate(futureTime.getTime())
        AndroidTestHelper.setTime(futureTime.get(Calendar.HOUR_OF_DAY), futureTime.get(Calendar.MINUTE), false)
        clickOn(R.string.reminder_status)
        clickOn(R.string.active)

        Espresso.pressBack()
        Espresso.pressBack()
        Espresso.pressBack()

        clickListItem(R.id.medicineList, 0)
        openMenu()
        clickOn(R.string.activate_all)

        clickOn(R.id.openAdvancedSettings)
        assertContains(DateFormat.getDateInstance(DateFormat.SHORT).format(nowTime.getTime()))
    }

    @Test //@AllowFlaky(attempts = 1)
    fun deleteLinkedReminderTest() {
        AndroidTestHelper.createMedicine("Test med")
        AndroidTestHelper.createReminder("1", LocalTime.of(0, 0))

        clickOn(R.id.openAdvancedSettings)

        clickOn(R.string.add_linked_reminder)
        BaristaDialogInteractions.clickDialogPositiveButton()
        AndroidTestHelper.setTime(0, 1, true)

        clickListItemChild(R.id.reminderList, 1, R.id.openAdvancedSettings)

        clickOn(R.string.add_linked_reminder)
        BaristaDialogInteractions.clickDialogPositiveButton()
        AndroidTestHelper.setTime(0, 2, true)

        clickListItemChild(R.id.reminderList, 0, R.id.openAdvancedSettings)

        openMenu()
        clickOn(R.string.delete)
        BaristaDialogInteractions.clickDialogPositiveButton()

        // Check that the reminder list is empty
        assertListItemCount(R.id.reminderList, 0)
    }

    @Test //@AllowFlaky(attempts = 1)
    fun reminderTypeTest() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        AndroidTestHelper.createMedicine("Test")

        // Standard time based reminder (amount 1)
        val reminder1Time = LocalTime.now().plusMinutes(40)
        AndroidTestHelper.createReminder("1", reminder1Time)

        // Linked reminder (amount 2) 30 minutes later
        clickOn(R.id.openAdvancedSettings)
        clickOn(R.string.add_linked_reminder)
        writeTo(android.R.id.input, "2")
        BaristaDialogInteractions.clickDialogPositiveButton()

        AndroidTestHelper.setTime(0, 30, true)

        // Interval reminder (amount 3) 2 hours from now
        clickOn(R.id.addReminder)
        clickOn(R.id.continuousIntervalCard)
        writeTo(R.id.editAmount, "3")
        clickOn(R.id.intervalHours)
        writeTo(R.id.editIntervalTime, "2")
        closeKeyboard()
        clickOn(R.id.createReminder)

        // Windowed interval reminder (amount 4)
        clickOn(R.id.addReminder)
        clickOn(R.id.windowedIntervalCard)
        writeTo(R.id.editAmount, "4")
        closeKeyboard()
        clickOn(R.id.editIntervalDailyStartTime)
        AndroidTestHelper.setTime(20, 0, false)
        clickOn(R.id.editIntervalDailyEndTime)
        AndroidTestHelper.setTime(23, 30, false)
        clickOn(R.id.intervalHours)
        writeTo(R.id.editIntervalTime, "3")
        closeKeyboard()
        clickOn(R.id.createReminder)

        // Check calendar view not crashing
        clickOn(R.id.openCalendar)
        Espresso.pressBack()

        var expectedString = context.getString(
            R.string.every_interval,
            "2 " + context.resources.getQuantityString(R.plurals.hours, 2)
        )
        assertCustomAssertionAtPosition(
            R.id.reminderList,
            0,
            R.id.reminderCardLayout,
            ViewAssertions.matches(ViewMatchers.withChild(ViewMatchers.withSubstring(expectedString)))
        )

        expectedString = TimeHelper.minutesToTimeString(context, (reminder1Time.toSecondOfDay() / 60).toLong())
        assertDisplayedAtPosition(R.id.reminderList, 1, R.id.editReminderTime, expectedString)

        expectedString = context.getString(
            R.string.linked_reminder_summary,
            TimeHelper.minutesToTimeString(context, (reminder1Time.toSecondOfDay() / 60).toLong())
        )
        assertDisplayedAtPosition(R.id.reminderList, 2, R.id.reminderCardLayout, expectedString)

        expectedString = context.getString(
            R.string.every_interval,
            "3 " + context.resources.getQuantityString(R.plurals.hours, 3)
        )
        assertCustomAssertionAtPosition(
            R.id.reminderList,
            3,
            R.id.reminderCardLayout,
            ViewAssertions.matches(ViewMatchers.withChild(ViewMatchers.withSubstring(expectedString)))
        )

        clickListItemChild(R.id.reminderList, 0, R.id.openAdvancedSettings)
        Espresso.pressBack()
        clickListItemChild(R.id.reminderList, 1, R.id.openAdvancedSettings)
        Espresso.pressBack()
        clickListItemChild(R.id.reminderList, 2, R.id.openAdvancedSettings)
        Espresso.pressBack()
        clickListItemChild(R.id.reminderList, 3, R.id.openAdvancedSettings)
        Espresso.pressBack()

        // Check overview and next reminders
        AndroidTestHelper.navigateTo(MainMenu.OVERVIEW)

        assertContains(R.id.reminderText, "Test (1)")
        expectedString = TimeHelper.minutesToTimeString(context, (reminder1Time.toSecondOfDay() / 60).toLong())
        assertContains(R.id.reminderText, expectedString)

        assertContains(R.id.reminderText, "Test (3)")

        assertContains(R.id.reminderText, "Test (4)")

        // If possible, take reminder 1 now and see if reminder 2 appears
        clickListItemChild(R.id.reminders, 1, R.id.stateButton)
        clickOn(R.id.takenButton)

        assertContains("Test (2)")
    }

    @Test //@AllowFlaky(attempts = 1)
    fun editReminderTest() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        AndroidTestHelper.createMedicine("Test")

        AndroidTestHelper.navigateTo(MainMenu.OVERVIEW)

        clickOn(R.id.logManualDose)

        clickListItem(position = 1)

        writeTo(android.R.id.input, "12")
        BaristaDialogInteractions.clickDialogPositiveButton()
        val now = Instant.now().epochSecond
        clickOn(com.google.android.material.R.id.material_timepicker_ok_button)

        clickListItemChild(R.id.reminders, 0, R.id.overviewContentContainer)
        assertContains(R.id.editEventName, "Test")
        assertContains(R.id.editEventAmount, "12")
        assertContains(R.id.editEventRemindedTimestamp, TimeHelper.secondsSinceEpochToTimeString(context, now))
        assertContains(R.id.editEventRemindedDate, TimeHelper.secondSinceEpochToDateString(context, now))
        assertContains(R.id.editEventTakenTimestamp, TimeHelper.secondsSinceEpochToTimeString(context, now))
        assertContains(R.id.editEventTakenDate, TimeHelper.secondSinceEpochToDateString(context, now))
        assertContains(R.id.editEventNotes, "")

        writeTo(R.id.editEventNotes, "Test notes")
        Espresso.pressBack()

        clickListItemChild(R.id.reminders, 0, R.id.overviewContentContainer)
        assertContains(R.id.editEventNotes, "Test notes")

        val newReminded = now + 60 * 60 * 24 + 120
        writeTo(R.id.editEventRemindedTimestamp, TimeHelper.secondsSinceEpochToTimeString(context, newReminded))
        writeTo(R.id.editEventRemindedDate, TimeHelper.secondSinceEpochToDateString(context, newReminded))

        val newTaken = now + 60 * 60 * 48 + 180
        writeTo(R.id.editEventTakenTimestamp, TimeHelper.secondsSinceEpochToTimeString(context, newTaken))
        writeTo(R.id.editEventTakenDate, TimeHelper.secondSinceEpochToDateString(context, newTaken))

        Espresso.pressBack()

        AndroidTestHelper.navigateTo(MainMenu.ANALYSIS)

        clickOn(R.id.tableChip)

        val tableView = AtomicReference<TableView?>()
        tableView.set(baristaRule.activityTestRule.getActivity().findViewById(R.id.reminder_table))

        var view = tableView.get()!!.cellRecyclerView.findViewWithTag<TextView>("time")
        TestCase.assertEquals(TimeHelper.secondsSinceEpochToDateTimeString(context, newReminded), view.getText())
        view = tableView.get()!!.cellRecyclerView.findViewWithTag("taken")
        TestCase.assertEquals(TimeHelper.secondsSinceEpochToDateTimeString(context, newTaken), view.getText())
    }

    @Test //@AllowFlaky(attempts = 1)
    fun deleteReminderTest() {
        AndroidTestHelper.createMedicine("Test")
        AndroidTestHelper.createReminder("1", LocalTime.of(20, 0))

        AndroidTestHelper.navigateTo(MainMenu.OVERVIEW)

        clickListItemChild(R.id.reminders, 0, R.id.stateButton)
        clickOn(R.id.takenButton)

        clickListItemChild(R.id.reminders, 0, R.id.stateButton)
        clickOn(R.id.deleteButton)
        BaristaDialogInteractions.clickDialogPositiveButton()

        assertListItemCount(R.id.reminders, 0)
    }

    @Test //@AllowFlaky(attempts = 1)
    fun intervalReminderTest() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        AndroidTestHelper.createMedicine("Test")
        AndroidTestHelper.createIntervalReminder("1", 10)
        Espresso.pressBack()

        AndroidTestHelper.navigateTo(MainMenu.OVERVIEW)

        clickListItemChild(R.id.reminders, 0, R.id.stateButton)
        clickOn(R.id.takenButton)

        assertNotContains(context.getString(R.string.interval_time, "0 min"))

        clickListItemChild(R.id.reminders, 1, R.id.stateButton)
        clickOn(R.id.takenButton)

        BaristaSleepInteractions.sleep(1000)
        assertContains(context.getString(R.string.interval_time, "0 min"))
    }

    @Test
    //@AllowFlaky(attempts = 1)
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
            clickOn(R.id.openAdvancedSettings)
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
            clickOn(R.id.stateButton)
            clickOn(R.id.takenButton)

            // Check if cyclic information is present
            clickOn(R.id.overviewContentContainer)
            if (reminder.shouldHaveInfo) {
                assertContains(R.id.editEventName, String.format("Test (1/%d)", reminder.consecutiveDays))
            } else {
                assertNotContains(R.id.editEventName, "Test (")
                assertContains(R.id.editEventName, "Test")
            }
            Espresso.pressBack()

            // Remove event
            clickOn(R.id.stateButton)
            clickOn(R.id.deleteButton)
            BaristaDialogInteractions.clickDialogPositiveButton()

            // Remove reminder
            AndroidTestHelper.navigateTo(MainMenu.MEDICINES)
            clickListItem(R.id.medicineList, 0)
            clickOn(R.id.openAdvancedSettings)
            openMenu()
            clickOn(R.string.delete)
            BaristaDialogInteractions.clickDialogPositiveButton()
        }
    }

    @Test
    @AllowFlaky(attempts = 1)
    fun weekendMode() {
        openMenu()
        clickOn(R.string.tab_settings)
        clickOn(R.string.weekend_mode)
        clickOn(R.string.active)
        clickOn(R.string.days_string)
        clickOn(R.string.friday)
        BaristaDialogInteractions.clickDialogPositiveButton()
        clickOn(R.string.time)
        AndroidTestHelper.setTime(21, 0, false)

        AndroidTestHelper.createMedicine("Test")
        AndroidTestHelper.createReminder("1", LocalTime.of(20, 0))

        AndroidTestHelper.navigateTo(MainMenu.OVERVIEW)
        assertContains(TimeHelper.minutesToTimeString(InstrumentationRegistry.getInstrumentation().targetContext, 21 * 60))
    }

    private class CyclicReminderInfo(var consecutiveDays: Int, var pauseDays: Int, var shouldHaveInfo: Boolean)
}
