package com.futsch1.medtimer

import androidx.test.espresso.Espresso.pressBack
import androidx.test.platform.app.InstrumentationRegistry
import com.adevinta.android.barista.assertion.BaristaListAssertions.assertListItemCount
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertContains
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertNotContains
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import com.adevinta.android.barista.interaction.BaristaEditTextInteractions.writeTo
import com.adevinta.android.barista.interaction.BaristaKeyboardInteractions.closeKeyboard
import com.adevinta.android.barista.interaction.BaristaListInteractions.clickListItemChild
import com.adevinta.android.barista.rule.flaky.AllowFlaky
import com.futsch1.medtimer.core.domain.model.StatisticFragment
import com.futsch1.medtimer.core.ui.R
import com.futsch1.medtimer.feature.ui.medicine.advancedReminderPreferences.AdvancedReminderTestTags
import com.futsch1.medtimer.feature.ui.overview.OverviewTestTags
import com.futsch1.medtimer.utilities.clickDialogPositiveButton
import org.junit.Test
import java.text.DateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Calendar
import java.util.Locale


class ReminderTest : MedTimerTestBase() {
    @Test
    @AllowFlaky(attempts = 3)
    fun activeReminderTest() {
        val futureTime = Calendar.getInstance()
        val year = futureTime.get(Calendar.YEAR)
        futureTime.set(year + 1, 1, 1)
        val pastTime = Calendar.getInstance()
        pastTime.set(year - 1, 1, 1)

        medicines.create("Test")
        medicineEditor.addReminder("1", laterToday())

        medicineEditor.openAdvancedSettings()
        clickPreference(R.string.reminder_enabled)

        pressBack()
        pressBack()
        navigation.toOverview()
        overview.assertEventCount(0)

        navigation.toMedicines()
        medicines.clickItem(0)

        medicineEditor.openAdvancedSettings()
        clickPreference(R.string.reminder_enabled)

        pressBack()
        pressBack()
        navigation.toOverview()
        overview.assertEventCount(1)

        navigation.toMedicines()
        medicines.clickItem(0)

        medicineEditor.openAdvancedSettings()
        clickPreference(R.string.reminder_status)
        clickPreference(R.string.period_start)
        clickPreference(R.string.start_date)
        pickers.pickDate(futureTime.getTime())

        pressBack()
        pressBack()
        pressBack()
        navigation.toOverview()
        overview.assertEventCount(0)

        navigation.toMedicines()
        medicines.clickItem(0)
        medicineEditor.openAdvancedSettings()
        clickPreference(R.string.reminder_status)
        clickPreference(R.string.period_end)
        clickPreference(R.string.end_date)
        pickers.pickDate(pastTime.getTime())

        pressBack()
        pressBack()
        pressBack()
        navigation.toOverview()
        overview.assertEventCount(0)

        navigation.toMedicines()
        medicines.clickItem(0)
        medicineEditor.openAdvancedSettings()
        clickPreference(R.string.reminder_status)
        clickPreference(R.string.start_date)
        pickers.pickDate(pastTime.getTime())
        clickPreference(R.string.end_date)
        pickers.pickDate(futureTime.getTime())

        pressBack()
        pressBack()
        pressBack()
        navigation.toOverview()
        overview.assertEventCount(1)
    }

    @Test
    @AllowFlaky(attempts = 3)
    fun activeIntervalReminderTest() {
        val futureTime = Calendar.getInstance()
        val year = futureTime.get(Calendar.YEAR)
        futureTime.set(year + 1, 1, 1)
        val nowTime = Calendar.getInstance()

        medicines.create("Test")
        medicineEditor.addIntervalReminder("1", 180)

        medicineEditor.openAdvancedSettings()
        clickPreference(R.string.interval_start_time)
        pickers.pickDate(futureTime.getTime())
        pickers.pickTime(LocalTime.of(futureTime.get(Calendar.HOUR_OF_DAY), futureTime.get(Calendar.MINUTE)))
        clickPreference(R.string.reminder_enabled)

        pressBack()
        pressBack()

        medicines.clickItem(0)
        openEditMedicineMenu()
        clickMenuItem(R.string.activate_all)

        medicineEditor.openAdvancedSettings()
        assertPreferenceSummary(
            R.string.interval_start_time,
            DateFormat.getDateInstance(DateFormat.SHORT).format(nowTime.getTime())
        )
    }

    @Test
    @AllowFlaky(attempts = 3)
    fun deleteLinkedReminderTest() {
        medicines.create("Test med")
        medicineEditor.addReminder("1", LocalTime.of(0, 0))

        medicineEditor.openAdvancedSettings()

        clickPreference(R.string.add_linked_reminder)
        clickDialogPositiveButton()
        pickers.pickDuration(0, 1)

        clickListItemChild(
            com.futsch1.medtimer.feature.ui.R.id.reminderList,
            1,
            com.futsch1.medtimer.feature.ui.R.id.openAdvancedSettings
        )

        clickPreference(R.string.add_linked_reminder)
        clickDialogPositiveButton()
        pickers.pickDuration(0, 2)

        clickListItemChild(
            com.futsch1.medtimer.feature.ui.R.id.reminderList,
            0,
            com.futsch1.medtimer.feature.ui.R.id.openAdvancedSettings
        )

        clickTag(AdvancedReminderTestTags.DELETE)
        clickDialogPositiveButton()

        // Check that the reminder list is empty
        assertListItemCount(com.futsch1.medtimer.feature.ui.R.id.reminderList, 0)
    }

    @Test
    @AllowFlaky(attempts = 3)
    fun reminderTypeTest() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        medicines.create("Test")

        // Standard time based reminder (amount 1)
        val reminder1Time = LocalTime.now().plusMinutes(40)
        medicineEditor.addReminder("1", reminder1Time)

        // Linked reminder (amount 2) 30 minutes later
        medicineEditor.openAdvancedSettings()
        clickPreference(R.string.add_linked_reminder)
        writeTo(android.R.id.input, "2")
        clickDialogPositiveButton()

        pickers.pickDuration(0, 30)

        // Interval reminder (amount 3) 2 hours from now
        clickOn(com.futsch1.medtimer.feature.ui.R.id.addReminder)
        clickOn(com.futsch1.medtimer.feature.ui.R.id.continuousIntervalCard)
        writeTo(com.futsch1.medtimer.feature.ui.R.id.editAmount, "3")
        clickOn(com.futsch1.medtimer.feature.ui.R.id.intervalHours)
        writeTo(com.futsch1.medtimer.feature.ui.R.id.editIntervalTime, "2")
        closeKeyboard()
        clickOn(com.futsch1.medtimer.feature.ui.R.id.createReminder)

        // Windowed interval reminder (amount 4)
        clickOn(com.futsch1.medtimer.feature.ui.R.id.addReminder)
        clickOn(com.futsch1.medtimer.feature.ui.R.id.windowedIntervalCard)
        writeTo(com.futsch1.medtimer.feature.ui.R.id.editAmount, "4")
        closeKeyboard()

        val windowStart = LocalTime.now().plusMinutes(5)
        clickOn(com.futsch1.medtimer.feature.ui.R.id.editIntervalDailyStartTime)
        pickers.pickTime(windowStart)
        clickOn(com.futsch1.medtimer.feature.ui.R.id.editIntervalDailyEndTime)
        pickers.pickTime(LocalTime.of(23, 59))
        clickOn(com.futsch1.medtimer.feature.ui.R.id.intervalHours)
        writeTo(com.futsch1.medtimer.feature.ui.R.id.editIntervalTime, "3")
        closeKeyboard()
        clickOn(com.futsch1.medtimer.feature.ui.R.id.createReminder)

        // Check calendar view not crashing
        openEditMedicineMenu()
        clickMenuItem(R.string.calendar)
        pressBack()

        assertListItemCount(com.futsch1.medtimer.feature.ui.R.id.reminderList, 4)
        assertReminderListContains(
            context.getString(R.string.every_interval, "2 " + context.resources.getQuantityString(R.plurals.hours, 2))
        )
        assertReminderListContains(timeFormatter().minutesToTimeString(reminder1Time.toSecondOfDay() / 60))
        assertReminderListContains(
            context.getString(R.string.linked_reminder_summary, timeFormatter().toTimeString(reminder1Time))
        )
        assertReminderListContains(
            context.getString(R.string.every_interval, "3 " + context.resources.getQuantityString(R.plurals.hours, 3))
        )

        clickListItemChild(
            com.futsch1.medtimer.feature.ui.R.id.reminderList,
            0,
            com.futsch1.medtimer.feature.ui.R.id.openAdvancedSettings
        )
        pressBack()
        clickListItemChild(
            com.futsch1.medtimer.feature.ui.R.id.reminderList,
            1,
            com.futsch1.medtimer.feature.ui.R.id.openAdvancedSettings
        )
        pressBack()
        clickListItemChild(
            com.futsch1.medtimer.feature.ui.R.id.reminderList,
            2,
            com.futsch1.medtimer.feature.ui.R.id.openAdvancedSettings
        )
        pressBack()
        clickListItemChild(
            com.futsch1.medtimer.feature.ui.R.id.reminderList,
            3,
            com.futsch1.medtimer.feature.ui.R.id.openAdvancedSettings
        )
        pressBack()

        // Check overview and next reminders
        navigation.toOverview()

        overview.assertEventContains("Test (1)")
        overview.assertEventContains(timeFormatter().minutesToTimeString(reminder1Time.toSecondOfDay() / 60))

        overview.assertEventContains("Test (3)")

        overview.assertEventContains("Test (4)")

        // If possible, take reminder 1 now and see if reminder 2 appears
        overview.clickEventState(1)
        clickMenuItem(R.string.taken)

        overview.assertEventContains("Test (2)")
    }

    @Test
    @AllowFlaky(attempts = 3)
    fun editReminderTest() {
        medicines.create("Test")

        navigation.toOverview()

        clickTag(OverviewTestTags.LOG_MANUAL_DOSE)

        clickDialogItem("Test")

        writeTo(android.R.id.input, "12")
        clickDialogPositiveButton()
        val now = Instant.now()
        clickOn(com.google.android.material.R.id.material_timepicker_ok_button)

        overview.clickEvent(0)
        assertContains(com.futsch1.medtimer.feature.ui.R.id.editEventName, "Test")
        assertContains(com.futsch1.medtimer.feature.ui.R.id.editEventAmount, "12")
        assertContains(
            com.futsch1.medtimer.feature.ui.R.id.editEventRemindedTimestamp,
            timeFormatter().toTimeString(now)
        )
        assertContains(
            com.futsch1.medtimer.feature.ui.R.id.editEventRemindedDate,
            timeFormatter().toDateString(now)
        )
        assertContains(
            com.futsch1.medtimer.feature.ui.R.id.editEventTakenTimestamp,
            timeFormatter().toTimeString(now)
        )
        assertContains(
            com.futsch1.medtimer.feature.ui.R.id.editEventTakenDate,
            timeFormatter().toDateString(now)
        )
        assertContains(com.futsch1.medtimer.feature.ui.R.id.editEventNotes, "")

        clickOn(com.futsch1.medtimer.feature.ui.R.id.skippedToggleButton)
        writeTo(com.futsch1.medtimer.feature.ui.R.id.editEventNotes, "Test notes")
        pressBack()

        overview.assertEventState(0, R.string.skipped)
        overview.clickEvent(0)
        clickOn(com.futsch1.medtimer.feature.ui.R.id.takenToggleButton)
        assertContains(com.futsch1.medtimer.feature.ui.R.id.editEventNotes, "Test notes")

        val newReminded = now.plusSeconds(60 * 60 * 24 + 120)
        writeTo(
            com.futsch1.medtimer.feature.ui.R.id.editEventRemindedTimestamp,
            timeFormatter().toTimeString(newReminded)
        )
        writeTo(
            com.futsch1.medtimer.feature.ui.R.id.editEventRemindedDate,
            timeFormatter().toDateString(newReminded)
        )

        val newTaken = now.plusSeconds(60 * 60 * 48 + 180)
        writeTo(
            com.futsch1.medtimer.feature.ui.R.id.editEventTakenTimestamp,
            timeFormatter().toTimeString(newTaken)
        )
        writeTo(
            com.futsch1.medtimer.feature.ui.R.id.editEventTakenDate,
            timeFormatter().toDateString(newTaken)
        )

        pressBack()

        navigation.toAnalysis()

        statistics.selectView(StatisticFragment.TABLE)
        statistics.assertTableContains(timeFormatter().toDateTimeString(newReminded))
        statistics.assertTableContains(timeFormatter().toDateTimeString(newTaken))
    }

    @Test
    @AllowFlaky(attempts = 3)
    fun deleteReminderTest() {
        medicines.create("Test")
        medicineEditor.addReminder("1", laterToday())

        navigation.toOverview()

        overview.clickEventState(0)
        clickMenuItem(R.string.taken)

        overview.clickEventState(0)
        clickMenuItem(R.string.delete)
        clickDialogPositiveButton()

        overview.assertEventCount(0)
    }

    @Test
    @AllowFlaky(attempts = 3)
    fun intervalReminderTest() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        medicines.create("Test")
        medicineEditor.addIntervalReminder("1", 10)
        pressBack()

        navigation.toOverview()

        overview.clickEventState(0)
        clickMenuItem(R.string.taken)

        overview.assertNoEventContains(context.getString(R.string.interval_time, "0 min"))

        overview.clickEventState(1)
        clickMenuItem(R.string.taken)

        overview.assertEventContains(context.getString(R.string.interval_time, "0 min"))
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
        medicines.create("Test")

        for (reminder in reminders) {
            // Create reminder
            medicineEditor.addReminder("1", laterToday())

            // Set active and pause days
            medicineEditor.openAdvancedSettings()
            clickPreference(R.string.cycle_reminder)
            preferences.setValue(R.string.cycle_consecutive_days, reminder!!.consecutiveDays.toString())
            preferences.setValue(R.string.cycle_pause_days, reminder.pauseDays.toString())

            // Set cycle start date of the reminder
            val cycleStart = Calendar.getInstance()
            // The month here is 7, not 8, since it is zero-indexed (so January is 0)
            cycleStart.set(2025, 7, 1)
            clickPreference(R.string.cycle_start_date)
            pickers.pickDate(cycleStart.getTime())

            // Leave the cyclic screen, the advanced settings and the editor: a tab tap no longer pops them.
            repeat(3) { pressBack() }

            // Mark event as taken
            navigation.toOverview()
            overview.clickEventState(0)
            clickMenuItem(R.string.taken)

            // Check if cyclic information is present
            overview.clickEvent(0)
            if (reminder.shouldHaveInfo) {
                assertContains(
                    com.futsch1.medtimer.feature.ui.R.id.editEventName,
                    String.format("Test (1/%d)", reminder.consecutiveDays)
                )
            } else {
                assertNotContains(com.futsch1.medtimer.feature.ui.R.id.editEventName, "Test (")
                assertContains(com.futsch1.medtimer.feature.ui.R.id.editEventName, "Test")
            }
            pressBack()

            // Remove event
            overview.clickEventState(0)
            clickMenuItem(R.string.delete)
            clickDialogPositiveButton()

            // Remove reminder
            navigation.toMedicines()
            medicines.clickItem(0)
            medicineEditor.openAdvancedSettings()
            clickTag(AdvancedReminderTestTags.DELETE)
            clickDialogPositiveButton()
        }
    }

    @Test
    @AllowFlaky(attempts = 3)
    fun weekendMode() {
        val windowStart = LocalTime.now().truncatedTo(ChronoUnit.MINUTES)
        val reminderTime = windowStart.plusMinutes(15)
        val windowEnd = windowStart.plusMinutes(30)

        settings.inSection(R.string.weekend_mode) {
            clickPreference(R.string.active)
            clickPreference(R.string.days_string)
            clickDialogItem(LocalDate.now().dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault()))
            clickDialogPositiveButton()
            clickPreference(R.string.weekend_start_time)
            pickers.pickTime(windowStart)
            clickPreference(R.string.weekend_end_time)
            pickers.pickTime(windowEnd)
        }

        medicines.create(TEST_MED)
        medicineEditor.addReminder("1", reminderTime)

        navigation.toOverview()
        overview.assertEventContains(timeFormatter().minutesToTimeString(windowEnd.hour * 60 + windowEnd.minute))
    }

    @Test
    @AllowFlaky(attempts = 3)
    fun reschedule() {
        settings.click(R.string.display_settings, R.string.combine_notifications)

        medicines.create(TEST_MED)
        medicineEditor.addIntervalReminder("1", 60)

        navigation.toOverview()
        overview.assertEventCountAtLeast(2)
        overview.longClickEvent(0)
        overview.clickEvent(1)

        overview.clickSelectionAction(R.string.reschedule_reminder)

        val rescheduleTime = laterToday()
        pickers.pickTime(rescheduleTime)

        val timeString = timeFormatter().minutesToTimeString(rescheduleTime.hour * 60 + rescheduleTime.minute)
        overview.assertEventState(0, R.string.please_wait)
        overview.assertEventTextContains(0, timeString)

        overview.assertEventState(1, R.string.please_wait)
        overview.assertEventTextContains(1, timeString)

        overview.longClickEvent(0)
        overview.assertSelectionCount(2)
        overview.clickSelectionAction(R.string.taken)

        overview.assertEventState(0, R.string.taken)
        overview.assertEventTextContains(0, timeString)

        overview.assertEventState(1, R.string.taken)
        overview.assertEventTextContains(1, timeString)
    }

    private class CyclicReminderInfo(
        var consecutiveDays: Int,
        var pauseDays: Int,
        var shouldHaveInfo: Boolean
    )
}
