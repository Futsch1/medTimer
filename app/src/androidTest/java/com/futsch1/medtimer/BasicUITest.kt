package com.futsch1.medtimer

import android.view.View
import android.widget.TextView
import androidx.test.espresso.Espresso
import com.adevinta.android.barista.assertion.BaristaCheckedAssertions.assertChecked
import com.adevinta.android.barista.assertion.BaristaCheckedAssertions.assertUnchecked
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertContains
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertNotContains
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import com.adevinta.android.barista.interaction.BaristaEditTextInteractions.clearText
import com.adevinta.android.barista.interaction.BaristaEditTextInteractions.writeTo
import com.adevinta.android.barista.interaction.BaristaKeyboardInteractions.closeKeyboard
import com.adevinta.android.barista.interaction.BaristaListInteractions.clickListItem
import com.adevinta.android.barista.interaction.BaristaListInteractions.clickListItemChild
import com.adevinta.android.barista.interaction.BaristaMenuClickInteractions.openMenu
import com.adevinta.android.barista.rule.flaky.AllowFlaky
import com.futsch1.medtimer.AndroidTestHelper.MainMenu
import com.futsch1.medtimer.AndroidTestHelper.createIntervalReminder
import com.futsch1.medtimer.AndroidTestHelper.createMedicine
import com.futsch1.medtimer.AndroidTestHelper.createReminder
import com.futsch1.medtimer.AndroidTestHelper.navigateTo
import com.futsch1.medtimer.AndroidTestHelper.setDate
import com.futsch1.medtimer.AndroidTestHelper.setValue
import com.futsch1.medtimer.utilities.clickDialogPositiveButton
import org.junit.Test
import java.text.DateFormat
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.atomic.AtomicReference


class BasicUITest : BaseTestHelper() {
    @Test
    @AllowFlaky(attempts = 3)
    fun basicUITest() {
        createMedicine(" Test ")
        createReminder("1", LocalTime.of(18, 0))

        clickOn(com.futsch1.medtimer.feature.ui.R.id.openAdvancedSettings)

        clickOn(com.futsch1.medtimer.feature.ui.R.string.dosing_instructions)
        clickOn(com.futsch1.medtimer.feature.ui.R.string.sample_instructions)
        clickListItem(position = 0)

        Espresso.pressBack()
        Espresso.pressBack()

        clickOn(com.futsch1.medtimer.feature.ui.R.id.openAdvancedSettings)
        assertContains(com.futsch1.medtimer.feature.ui.R.string.before_meal)
        Espresso.pressBack()

        writeTo(com.futsch1.medtimer.feature.ui.R.id.editAmount, " 2 ")
        Espresso.pressBack()

        clickListItem(com.futsch1.medtimer.feature.ui.R.id.medicineList, 0)
        assertDisplayed(com.futsch1.medtimer.feature.ui.R.id.editAmount, "2")
        clickOn(com.futsch1.medtimer.feature.ui.R.id.openAdvancedSettings)
        openMenu()
        clickOn(com.futsch1.medtimer.feature.ui.R.string.duplicate)
        Espresso.pressBack()

        assertContains(com.futsch1.medtimer.feature.ui.R.id.remindersSummary, ";")

        navigateTo(MainMenu.OVERVIEW)
        assertContains(TEST_2)

        navigateTo(MainMenu.MEDICINES)
        clickListItem(com.futsch1.medtimer.feature.ui.R.id.medicineList, 0)
        writeTo(com.futsch1.medtimer.feature.ui.R.id.editMedicineName, " Test2 ")
        Espresso.pressBack()

        navigateTo(MainMenu.OVERVIEW)
        assertContains("Test2 (2)")
    }

    @Test
    @AllowFlaky(attempts = 3)
    fun menuHandlingTest() {
        createMedicine("Test")
        createReminder("1", LocalTime.of(12, 0))

        clickOn(com.futsch1.medtimer.feature.ui.R.id.openAdvancedSettings)

        val cycleStart = Calendar.getInstance()
        cycleStart.set(2025, 1, 1)
        val cycleStartString =
            DateFormat.getDateInstance(DateFormat.SHORT).format(cycleStart.getTime())
        clickOn(com.futsch1.medtimer.core.ui.R.string.cycle_reminder)
        clickOn(com.futsch1.medtimer.core.ui.R.string.cycle_start_date)
        setDate(cycleStart.getTime())
        clickOn(com.futsch1.medtimer.feature.ui.R.string.cycle_consecutive_days)
        setValue("5")
        clickOn(com.futsch1.medtimer.feature.ui.R.string.cycle_pause_days)
        setValue("6")
        Espresso.pressBack()

        clickOn(com.futsch1.medtimer.feature.ui.R.string.remind_on_weekdays)
        clickOn(com.futsch1.medtimer.feature.ui.R.string.monday)
        clickOn(com.futsch1.medtimer.feature.ui.R.string.tuesday)
        clickDialogPositiveButton()

        clickOn(com.futsch1.medtimer.feature.ui.R.string.remind_on_days_of_month)
        clickOn("1")
        clickOn("3")
        clickDialogPositiveButton()

        Espresso.pressBack()

        clickOn(com.futsch1.medtimer.feature.ui.R.id.openAdvancedSettings)

        clickOn(com.futsch1.medtimer.core.ui.R.string.cycle_reminder)
        assertContains(cycleStartString)
        assertContains("5")
        assertContains("6")
        Espresso.pressBack()

        clickOn(com.futsch1.medtimer.feature.ui.R.string.remind_on_weekdays)
        assertUnchecked(com.futsch1.medtimer.feature.ui.R.string.monday)
        assertUnchecked(com.futsch1.medtimer.feature.ui.R.string.tuesday)
        assertChecked(com.futsch1.medtimer.feature.ui.R.string.wednesday)
        clickDialogPositiveButton()

        clickOn(com.futsch1.medtimer.feature.ui.R.string.remind_on_days_of_month)
        assertChecked("1")
        assertUnchecked("2")
        assertChecked("3")
        clickDialogPositiveButton()
    }

    @Test
    @AllowFlaky(attempts = 3)
    fun notesTest() {
        createMedicine("Test")

        // Test saving notes
        val notes = "Contains catnip\n\nmeow :3"

        clickOn(com.futsch1.medtimer.feature.ui.R.id.openNotes)
        writeTo(com.futsch1.medtimer.feature.ui.R.id.notes, notes)
        closeKeyboard()
        clickOn(com.futsch1.medtimer.feature.ui.R.id.confirmSaveNotes)

        Espresso.pressBack()
        clickListItem(com.futsch1.medtimer.feature.ui.R.id.medicineList, 0)

        // Check if the note is saved
        clickOn(com.futsch1.medtimer.feature.ui.R.id.openNotes)
        assertDisplayed(com.futsch1.medtimer.feature.ui.R.id.notes, notes)

        // Test cancelling saving notes
        clearText(com.futsch1.medtimer.feature.ui.R.id.notes)
        closeKeyboard()
        clickOn(com.futsch1.medtimer.feature.ui.R.id.cancelSaveNotes)

        // Check that the note is unmodified
        clickOn(com.futsch1.medtimer.feature.ui.R.id.openNotes)
        assertDisplayed(com.futsch1.medtimer.feature.ui.R.id.notes, notes)
    }

    @Test
    @AllowFlaky(attempts = 3)
    fun appIntro() {
        openMenu()

        clickOn(com.futsch1.medtimer.feature.ui.R.string.show_intro)

        assertDisplayed(com.futsch1.medtimer.feature.ui.R.string.intro_welcome)
        assertDisplayed(com.futsch1.medtimer.feature.ui.R.string.intro_welcome_description)

        clickOn(com.github.appintro.R.id.skip)

        assertDisplayed(com.futsch1.medtimer.feature.ui.R.string.tab_overview)
    }

    @Test
    @AllowFlaky(attempts = 3)
    fun overviewFilters() {
        createMedicine("Test")
        createIntervalReminder("2", 1000)

        Espresso.pressBack()

        navigateTo(MainMenu.OVERVIEW)
        assertContains(TEST_2)

        clickOn(com.futsch1.medtimer.feature.ui.R.id.filterRaised)
        assertContains(TEST_2)
        clickOn(com.futsch1.medtimer.feature.ui.R.id.filterRaised)

        assertContains(TEST_2)

        clickOn(com.futsch1.medtimer.feature.ui.R.id.filterTaken)
        assertNotContains(TEST_2)
        clickOn(com.futsch1.medtimer.feature.ui.R.id.filterTaken)

        assertContains(TEST_2)

        clickListItemChild(
            com.futsch1.medtimer.feature.ui.R.id.reminders, 0, com.futsch1.medtimer.feature.ui.R.id.stateButton
        )
        clickOn(com.futsch1.medtimer.feature.ui.R.id.takenButton)

        clickOn(com.futsch1.medtimer.feature.ui.R.id.filterTaken)
        assertContains(TEST_2)
        clickOn(com.futsch1.medtimer.feature.ui.R.id.filterTaken)

        assertContains(TEST_2)

        clickOn(com.futsch1.medtimer.feature.ui.R.id.filterRaised)
        assertNotContains(TEST_2)
        clickOn(com.futsch1.medtimer.feature.ui.R.id.filterRaised)

        clickOn(com.futsch1.medtimer.feature.ui.R.id.filterSkipped)
        assertNotContains(TEST_2)
        clickOn(com.futsch1.medtimer.feature.ui.R.id.filterSkipped)

        clickOn(com.futsch1.medtimer.feature.ui.R.id.filterScheduled)
        assertNotContains(TEST_2)
        clickOn(com.futsch1.medtimer.feature.ui.R.id.filterScheduled)

        navigateTo(MainMenu.MEDICINES)
        clickOn("Test")
        createReminder("1", LocalTime.of(20, 0))

        navigateTo(MainMenu.OVERVIEW)

        assertContains("Test (1)")

        clickOn(com.futsch1.medtimer.feature.ui.R.id.filterScheduled)
        assertContains("Test (1)")
    }

    @Test
    @AllowFlaky(attempts = 3)
    // Using internal assert
    fun overviewDaySelection() {
        val secondDay =
            DayOfWeek.SATURDAY.getDisplayName(TextStyle.SHORT, Locale.getDefault()) + "\n2"
        val today = DayOfWeek.FRIDAY.getDisplayName(TextStyle.SHORT, Locale.getDefault()) + "\n1"

        clickOn(secondDay)

        navigateTo(MainMenu.MEDICINES)

        clickOn(com.futsch1.medtimer.feature.ui.R.id.overviewFragment)

        val view = AtomicReference<View>()
        view.set(
            baristaRule.activityTestRule.getActivity().findViewById(com.futsch1.medtimer.feature.ui.R.id.overviewWeek)
        )

        var currentDay = view.get().findViewWithTag<TextView>("selected")
        internalAssert(currentDay.getText() == secondDay)

        clickOn(com.futsch1.medtimer.feature.ui.R.id.overviewFragment)

        navigateTo(MainMenu.OVERVIEW)
        view.set(
            baristaRule.activityTestRule.getActivity().findViewById(com.futsch1.medtimer.feature.ui.R.id.overviewWeek)
        )
        currentDay = view.get().findViewWithTag("selected")
        internalAssert(currentDay.getText() == today)
    }

    companion object {
        const val TEST_2: String = "Test (2)"
    }
}
