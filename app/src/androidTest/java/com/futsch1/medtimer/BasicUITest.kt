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
import com.adevinta.android.barista.interaction.BaristaDialogInteractions
import com.adevinta.android.barista.interaction.BaristaEditTextInteractions.clearText
import com.adevinta.android.barista.interaction.BaristaEditTextInteractions.writeTo
import com.adevinta.android.barista.interaction.BaristaKeyboardInteractions.closeKeyboard
import com.adevinta.android.barista.interaction.BaristaListInteractions.clickListItem
import com.adevinta.android.barista.interaction.BaristaListInteractions.clickListItemChild
import com.adevinta.android.barista.interaction.BaristaMenuClickInteractions.openMenu
import com.futsch1.medtimer.AndroidTestHelper.MainMenu
import com.futsch1.medtimer.AndroidTestHelper.createIntervalReminder
import com.futsch1.medtimer.AndroidTestHelper.createMedicine
import com.futsch1.medtimer.AndroidTestHelper.createReminder
import com.futsch1.medtimer.AndroidTestHelper.navigateTo
import com.futsch1.medtimer.AndroidTestHelper.setDate
import com.futsch1.medtimer.AndroidTestHelper.setValue
import org.junit.Test
import java.text.DateFormat
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.atomic.AtomicReference


class BasicUITest : BaseTestHelper() {
    @Test //@AllowFlaky(attempts = 1)
    fun basicUITest() {
        createMedicine(" Test ")
        createReminder("1", LocalTime.of(18, 0))

        clickOn(R.id.openAdvancedSettings)

        clickOn(R.string.dosing_instructions)
        clickOn(R.string.sample_instructions)
        clickListItem(position = 0)

        Espresso.pressBack()
        Espresso.pressBack()

        clickOn(R.id.openAdvancedSettings)
        assertContains(R.string.before_meal)
        Espresso.pressBack()

        writeTo(R.id.editAmount, " 2 ")
        Espresso.pressBack()

        clickListItem(R.id.medicineList, 0)
        assertDisplayed(R.id.editAmount, "2")
        clickOn(R.id.openAdvancedSettings)
        openMenu()
        clickOn(R.string.duplicate)
        Espresso.pressBack()

        assertContains(R.id.remindersSummary, ";")

        navigateTo(MainMenu.OVERVIEW)
        assertContains(TEST_2)

        navigateTo(MainMenu.MEDICINES)
        clickListItem(R.id.medicineList, 0)
        writeTo(R.id.editMedicineName, " Test2 ")
        Espresso.pressBack()

        navigateTo(MainMenu.OVERVIEW)
        assertContains("Test2 (2)")
    }

    @Test //@AllowFlaky(attempts = 1)
    fun menuHandlingTest() {
        createMedicine("Test")
        createReminder("1", LocalTime.of(12, 0))

        clickOn(R.id.openAdvancedSettings)

        val cycleStart = Calendar.getInstance()
        cycleStart.set(2025, 1, 1)
        val cycleStartString =
            DateFormat.getDateInstance(DateFormat.SHORT).format(cycleStart.getTime())
        clickOn(R.string.cycle_reminder)
        clickOn(R.string.cycle_start_date)
        setDate(cycleStart.getTime())
        clickOn(R.string.cycle_consecutive_days)
        setValue("5")
        clickOn(R.string.cycle_pause_days)
        setValue("6")
        Espresso.pressBack()

        clickOn(R.string.remind_on_weekdays)
        clickOn(R.string.monday)
        clickOn(R.string.tuesday)
        BaristaDialogInteractions.clickDialogPositiveButton()

        clickOn(R.string.remind_on_days_of_month)
        clickOn("1")
        clickOn("3")
        BaristaDialogInteractions.clickDialogPositiveButton()

        Espresso.pressBack()

        clickOn(R.id.openAdvancedSettings)

        clickOn(R.string.cycle_reminder)
        assertContains(cycleStartString)
        assertContains("5")
        assertContains("6")
        Espresso.pressBack()

        clickOn(R.string.remind_on_weekdays)
        assertUnchecked(R.string.monday)
        assertUnchecked(R.string.tuesday)
        assertChecked(R.string.wednesday)
        BaristaDialogInteractions.clickDialogPositiveButton()

        clickOn(R.string.remind_on_days_of_month)
        assertChecked("1")
        assertUnchecked("2")
        assertChecked("3")
        BaristaDialogInteractions.clickDialogPositiveButton()
    }

    @Test //@AllowFlaky(attempts = 1)
    fun notesTest() {
        createMedicine("Test")

        // Test saving notes
        val notes = "Contains catnip\n\nmeow :3"

        clickOn(R.id.openNotes)
        writeTo(R.id.notes, notes)
        closeKeyboard()
        clickOn(R.id.confirmSaveNotes)

        Espresso.pressBack()
        clickListItem(R.id.medicineList, 0)

        // Check if the note is saved
        clickOn(R.id.openNotes)
        assertDisplayed(R.id.notes, notes)

        // Test cancelling saving notes
        clearText(R.id.notes)
        closeKeyboard()
        clickOn(R.id.cancelSaveNotes)

        // Check that the note is unmodified
        clickOn(R.id.openNotes)
        assertDisplayed(R.id.notes, notes)
    }

    @Test //@AllowFlaky(attempts = 1)
    fun appIntro() {
        openMenu()

        clickOn(R.string.show_intro)

        assertDisplayed(R.string.intro_welcome)
        assertDisplayed(R.string.intro_welcome_description)

        clickOn(com.github.appintro.R.id.skip)

        assertDisplayed(R.string.tab_overview)
    }

    @Test //@AllowFlaky(attempts = 1)
    fun overviewFilters() {
        createMedicine("Test")
        createIntervalReminder("2", 1000)

        Espresso.pressBack()

        navigateTo(MainMenu.OVERVIEW)
        assertContains(TEST_2)

        clickOn(R.id.filterRaised)
        assertContains(TEST_2)
        clickOn(R.id.filterRaised)

        assertContains(TEST_2)

        clickOn(R.id.filterTaken)
        assertNotContains(TEST_2)
        clickOn(R.id.filterTaken)

        assertContains(TEST_2)

        clickListItemChild(
            R.id.reminders,
            0,
            R.id.stateButton
        )
        clickOn(R.id.takenButton)

        clickOn(R.id.filterTaken)
        assertContains(TEST_2)
        clickOn(R.id.filterTaken)

        assertContains(TEST_2)

        clickOn(R.id.filterRaised)
        assertNotContains(TEST_2)
        clickOn(R.id.filterRaised)

        clickOn(R.id.filterSkipped)
        assertNotContains(TEST_2)
        clickOn(R.id.filterSkipped)

        clickOn(R.id.filterScheduled)
        assertNotContains(TEST_2)
        clickOn(R.id.filterScheduled)

        navigateTo(MainMenu.MEDICINES)
        clickOn("Test")
        createReminder("1", LocalTime.of(20, 0))

        navigateTo(MainMenu.OVERVIEW)

        assertContains("Test (1)")

        clickOn(R.id.filterScheduled)
        assertContains("Test (1)")
    }

    @Test //@AllowFlaky(attempts = 1)
    // Using internal assert
    fun overviewDaySelection() {
        val secondDay =
            DayOfWeek.SATURDAY.getDisplayName(TextStyle.SHORT, Locale.getDefault()) + "\n2"
        val today = DayOfWeek.FRIDAY.getDisplayName(TextStyle.SHORT, Locale.getDefault()) + "\n1"

        clickOn(secondDay)

        navigateTo(MainMenu.MEDICINES)

        clickOn(R.id.overviewFragment)

        val view = AtomicReference<View?>()
        view.set(
            baristaRule.activityTestRule.getActivity()
                .findViewById<View?>(R.id.overviewWeek)
        )

        var currentDay = view.get()!!.findViewWithTag<TextView>("selected")
        internalAssert(currentDay.getText() == secondDay)

        clickOn(R.id.overviewFragment)

        navigateTo(MainMenu.OVERVIEW)
        view.set(
            baristaRule.activityTestRule.getActivity()
                .findViewById<View?>(R.id.overviewWeek)
        )
        currentDay = view.get()!!.findViewWithTag<TextView>("selected")
        internalAssert(currentDay.getText() == today)
    }

    companion object {
        const val TEST_2: String = "Test (2)"
    }
}
