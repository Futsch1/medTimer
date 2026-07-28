package com.futsch1.medtimer

import androidx.test.espresso.Espresso.pressBack
import com.adevinta.android.barista.assertion.BaristaListAssertions.assertListItemCount
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import com.adevinta.android.barista.interaction.BaristaEditTextInteractions.clearText
import com.adevinta.android.barista.interaction.BaristaEditTextInteractions.writeTo
import com.adevinta.android.barista.interaction.BaristaKeyboardInteractions.closeKeyboard
import com.adevinta.android.barista.rule.flaky.AllowFlaky
import com.futsch1.medtimer.core.domain.model.OverviewFilter
import com.futsch1.medtimer.core.ui.R
import com.futsch1.medtimer.utilities.clickDialogPositiveButton
import org.junit.Test
import java.text.DateFormat
import java.time.LocalDate
import java.time.LocalTime
import java.util.Calendar
import kotlin.test.assertTrue


class BasicUITest : MedTimerTestBase() {
    @Test
    @AllowFlaky(attempts = 3)
    fun basicUITest() {
        medicines.create(" Test ")
        medicineEditor.addReminder("1", laterToday())

        medicineEditor.openAdvancedSettings()

        clickPreference(R.string.dosing_instructions)
        clickPreference(R.string.sample_instructions)
        clickDialogItem(R.string.before_meal)

        pressBack()
        pressBack()

        medicineEditor.openAdvancedSettings()
        assertPreferenceSummary(R.string.dosing_instructions, getString(R.string.before_meal))
        pressBack()

        writeTo(com.futsch1.medtimer.feature.ui.R.id.editAmount, " 2 ")
        pressBack()

        medicines.clickItem(0)
        assertDisplayed(com.futsch1.medtimer.feature.ui.R.id.editAmount, "2")
        medicineEditor.openAdvancedSettings()
        medicineEditor.duplicateReminder()

        assertListItemCount(com.futsch1.medtimer.feature.ui.R.id.reminderList, 2)

        pressBack()

        navigation.toOverview()
        overview.assertEventContains(TEST_2)

        navigation.toMedicines()
        medicines.clickItem(0)
        medicineEditor.rename(" Test2 ")
        pressBack()

        navigation.toOverview()
        overview.assertEventContains("Test2 (2)")
    }

    @Test
    @AllowFlaky(attempts = 3)
    fun menuHandlingTest() {
        medicines.create("Test")
        medicineEditor.addReminder("1", LocalTime.of(12, 0))

        medicineEditor.openAdvancedSettings()

        val cycleStart = Calendar.getInstance()
        cycleStart.set(2025, 1, 1)
        val cycleStartString =
            DateFormat.getDateInstance(DateFormat.SHORT).format(cycleStart.getTime())
        clickPreference(R.string.cycle_reminder)
        clickPreference(R.string.cycle_start_date)
        pickers.pickDate(cycleStart.getTime())
        preferences.setValue(R.string.cycle_consecutive_days, "5")
        preferences.setValue(R.string.cycle_pause_days, "6")
        pressBack()

        clickPreference(R.string.remind_on_weekdays)
        clickDialogItem(R.string.monday)
        clickDialogItem(R.string.tuesday)
        clickDialogPositiveButton()

        clickPreference(R.string.remind_on_days_of_month)
        clickDialogItem("1")
        clickDialogItem("3")
        clickDialogPositiveButton()

        pressBack()

        medicineEditor.openAdvancedSettings()

        clickPreference(R.string.cycle_reminder)
        assertPreferenceSummary(R.string.cycle_start_date, cycleStartString)
        assertPreferenceSummary(R.string.cycle_consecutive_days, "5")
        assertPreferenceSummary(R.string.cycle_pause_days, "6")
        pressBack()

        clickPreference(R.string.remind_on_weekdays)
        assertDialogItemNotChecked(R.string.monday)
        assertDialogItemNotChecked(R.string.tuesday)
        assertDialogItemChecked(R.string.wednesday)
        clickDialogPositiveButton()

        clickPreference(R.string.remind_on_days_of_month)
        assertDialogItemChecked("1")
        assertDialogItemNotChecked("2")
        assertDialogItemChecked("3")
        clickDialogPositiveButton()
    }

    @Test
    @AllowFlaky(attempts = 3)
    fun notesTest() {
        medicines.create("Test")

        val notes = "Contains catnip\n\nmeow :3"

        menus.clickEditMedicineOption(R.string.notes)
        writeTo(com.futsch1.medtimer.feature.ui.R.id.notes, notes)
        closeKeyboard()
        clickOn(com.futsch1.medtimer.feature.ui.R.id.confirmSaveNotes)

        pressBack()
        medicines.clickItem(0)

        menus.clickEditMedicineOption(R.string.notes)
        assertDisplayed(com.futsch1.medtimer.feature.ui.R.id.notes, notes)

        clearText(com.futsch1.medtimer.feature.ui.R.id.notes)
        closeKeyboard()
        clickOn(com.futsch1.medtimer.feature.ui.R.id.cancelSaveNotes)

        menus.clickEditMedicineOption(R.string.notes)
        assertDisplayed(com.futsch1.medtimer.feature.ui.R.id.notes, notes)
    }

    @Test
    @AllowFlaky(attempts = 3)
    fun appIntro() {
        menus.clickAppOption(R.string.show_intro)

        assertDisplayed(com.github.appintro.R.id.title, getString(R.string.intro_welcome))
        assertDisplayed(com.github.appintro.R.id.description, getString(R.string.intro_welcome_description))

        clickOn(com.github.appintro.R.id.skip)

        navigation.assertOverviewShown()
    }

    @Test
    @AllowFlaky(attempts = 3)
    fun overviewFilters() {
        medicines.create("Test")
        medicineEditor.addIntervalReminder("2", 1000)

        pressBack()

        navigation.toOverview()
        overview.assertEventContains(TEST_2)

        overview.toggleFilter(OverviewFilter.RAISED)
        overview.assertEventContains(TEST_2)
        overview.toggleFilter(OverviewFilter.RAISED)

        overview.assertEventContains(TEST_2)

        overview.toggleFilter(OverviewFilter.TAKEN)
        overview.assertEventCount(0)
        overview.toggleFilter(OverviewFilter.TAKEN)

        overview.assertEventContains(TEST_2)

        overview.clickEventState(0)
        overview.clickAction(R.string.taken)

        overview.toggleFilter(OverviewFilter.TAKEN)
        overview.assertEventContains(TEST_2)
        overview.toggleFilter(OverviewFilter.TAKEN)

        overview.assertEventContains(TEST_2)

        overview.toggleFilter(OverviewFilter.RAISED)
        overview.assertEventCount(0)
        overview.toggleFilter(OverviewFilter.RAISED)

        overview.toggleFilter(OverviewFilter.SKIPPED)
        overview.assertEventCount(0)
        overview.toggleFilter(OverviewFilter.SKIPPED)

        overview.toggleFilter(OverviewFilter.SCHEDULED)
        overview.assertEventCount(0)
        overview.toggleFilter(OverviewFilter.SCHEDULED)

        navigation.toMedicines()
        medicines.clickItem(0)
        medicineEditor.addReminder("1", laterToday())

        navigation.toOverview()

        overview.assertEventContains("Test (1)")

        overview.toggleFilter(OverviewFilter.SCHEDULED)
        overview.assertEventContains("Test (1)")
    }

    @Test
    @AllowFlaky(attempts = 3)
    fun overviewDaySelection() {
        menus.clickAppOption(R.string.generate_test_data_and_events)

        val today = LocalDate.now()
        val secondDay = today.plusDays(1)

        overview.clickDay(secondDay)
        assertTrue(overview.eventCount() > 0)

        navigation.toOverview()
        overview.assertDaySelected(secondDay)

        navigation.toMedicines()
        navigation.toOverview()
        overview.assertDaySelected(today)
        assertTrue(overview.eventCount() > 0)

        overview.nextWeek()
        assertTrue(overview.eventCount() > 0)

        overview.previousWeek()
        overview.assertDaySelected(today)
        assertTrue(overview.eventCount() > 0)

        overview.previousWeek()
        assertTrue(overview.eventCount() > 0)
    }

    companion object {
        const val TEST_2: String = "Test (2)"
    }
}
