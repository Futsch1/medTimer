package com.futsch1.medtimer

import android.view.View
import android.widget.TextView
import androidx.test.espresso.Espresso
import com.adevinta.android.barista.assertion.BaristaCheckedAssertions.assertChecked
import com.adevinta.android.barista.assertion.BaristaCheckedAssertions.assertUnchecked
import com.adevinta.android.barista.assertion.BaristaListAssertions.assertListItemCount
import com.adevinta.android.barista.assertion.BaristaListAssertions.assertListNotEmpty
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertContains
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertNotContains
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import com.adevinta.android.barista.interaction.BaristaEditTextInteractions.clearText
import com.adevinta.android.barista.interaction.BaristaEditTextInteractions.writeTo
import com.adevinta.android.barista.interaction.BaristaKeyboardInteractions.closeKeyboard
import com.adevinta.android.barista.interaction.BaristaListInteractions.clickListItem
import com.adevinta.android.barista.interaction.BaristaListInteractions.clickListItemChild
import com.adevinta.android.barista.rule.flaky.AllowFlaky
import com.futsch1.medtimer.AndroidTestHelper.MainMenu
import com.futsch1.medtimer.AndroidTestHelper.createIntervalReminder
import com.futsch1.medtimer.AndroidTestHelper.createMedicine
import com.futsch1.medtimer.AndroidTestHelper.createReminder
import com.futsch1.medtimer.AndroidTestHelper.navigateTo
import com.futsch1.medtimer.AndroidTestHelper.setDate
import com.futsch1.medtimer.AndroidTestHelper.setValue
import com.futsch1.medtimer.core.ui.R
import com.futsch1.medtimer.utilities.clickDialogPositiveButton
import org.junit.Test
import java.text.DateFormat
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.atomic.AtomicReference
import com.futsch1.medtimer.core.domain.model.OverviewFilter
import com.futsch1.medtimer.feature.ui.AppOptionsTestTags
import com.futsch1.medtimer.feature.ui.overview.OverviewTestTags


class BasicUITest : BaseTestHelper() {
    @Test
    @AllowFlaky(attempts = 3)
    fun basicUITest() {
        createMedicine(" Test ")
        createReminder("1", LocalTime.of(18, 0))

        clickOn(com.futsch1.medtimer.feature.ui.R.id.openAdvancedSettings)

        clickOn(R.string.dosing_instructions)
        clickOn(R.string.sample_instructions)
        clickListItem(position = 0)

        Espresso.pressBack()
        Espresso.pressBack()

        clickOn(com.futsch1.medtimer.feature.ui.R.id.openAdvancedSettings)
        assertContains(R.string.before_meal)
        Espresso.pressBack()

        writeTo(com.futsch1.medtimer.feature.ui.R.id.editAmount, " 2 ")
        Espresso.pressBack()

        AndroidTestHelper.clickMedicineItem(0)
        assertDisplayed(com.futsch1.medtimer.feature.ui.R.id.editAmount, "2")
        clickOn(com.futsch1.medtimer.feature.ui.R.id.openAdvancedSettings)
        openEditMedicineMenu()
        clickMenuItem(R.string.duplicate)

        assertListItemCount(com.futsch1.medtimer.feature.ui.R.id.reminderList, 2)

        navigateTo(MainMenu.OVERVIEW)
        assertContains(TEST_2)

        navigateTo(MainMenu.MEDICINES)
        AndroidTestHelper.clickMedicineItem(0)
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
        clickDialogPositiveButton()

        clickOn(R.string.remind_on_days_of_month)
        clickOn("1")
        clickOn("3")
        clickDialogPositiveButton()

        Espresso.pressBack()

        clickOn(com.futsch1.medtimer.feature.ui.R.id.openAdvancedSettings)

        clickOn(R.string.cycle_reminder)
        assertContains(cycleStartString)
        assertContains("5")
        assertContains("6")
        Espresso.pressBack()

        clickOn(R.string.remind_on_weekdays)
        assertUnchecked(R.string.monday)
        assertUnchecked(R.string.tuesday)
        assertChecked(R.string.wednesday)
        clickDialogPositiveButton()

        clickOn(R.string.remind_on_days_of_month)
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

        clickMenuItem(com.futsch1.medtimer.core.ui.R.string.notes)
        writeTo(com.futsch1.medtimer.feature.ui.R.id.notes, notes)
        closeKeyboard()
        clickOn(com.futsch1.medtimer.feature.ui.R.id.confirmSaveNotes)

        Espresso.pressBack()
        AndroidTestHelper.clickMedicineItem(0)

        // Check if the note is saved
        clickMenuItem(com.futsch1.medtimer.core.ui.R.string.notes)
        assertDisplayed(com.futsch1.medtimer.feature.ui.R.id.notes, notes)

        // Test cancelling saving notes
        clearText(com.futsch1.medtimer.feature.ui.R.id.notes)
        closeKeyboard()
        clickOn(com.futsch1.medtimer.feature.ui.R.id.cancelSaveNotes)

        // Check that the note is unmodified
        clickMenuItem(com.futsch1.medtimer.core.ui.R.string.notes)
        assertDisplayed(com.futsch1.medtimer.feature.ui.R.id.notes, notes)
    }

    @Test
    @AllowFlaky(attempts = 3)
    fun appIntro() {
        openAppOptionsMenu()

        clickTag(AppOptionsTestTags.SHOW_INTRO)

        assertDisplayed(R.string.intro_welcome)
        assertDisplayed(R.string.intro_welcome_description)

        clickOn(com.github.appintro.R.id.skip)

        assertContains(R.string.tab_overview)
    }

    @Test
    @AllowFlaky(attempts = 3)
    fun overviewFilters() {
        createMedicine("Test")
        createIntervalReminder("2", 1000)

        Espresso.pressBack()

        navigateTo(MainMenu.OVERVIEW)
        assertContains(TEST_2)

        toggleOverviewFilter(OverviewFilter.RAISED)
        assertContains(TEST_2)
        toggleOverviewFilter(OverviewFilter.RAISED)

        assertContains(TEST_2)

        toggleOverviewFilter(OverviewFilter.TAKEN)
        assertNotContains(TEST_2)
        toggleOverviewFilter(OverviewFilter.TAKEN)

        assertContains(TEST_2)

        clickOverviewEventState(0)
        clickMenuItem(com.futsch1.medtimer.core.ui.R.string.taken)

        toggleOverviewFilter(OverviewFilter.TAKEN)
        assertContains(TEST_2)
        toggleOverviewFilter(OverviewFilter.TAKEN)

        assertContains(TEST_2)

        toggleOverviewFilter(OverviewFilter.RAISED)
        assertNotContains(TEST_2)
        toggleOverviewFilter(OverviewFilter.RAISED)

        toggleOverviewFilter(OverviewFilter.SKIPPED)
        assertNotContains(TEST_2)
        toggleOverviewFilter(OverviewFilter.SKIPPED)

        toggleOverviewFilter(OverviewFilter.SCHEDULED)
        assertNotContains(TEST_2)
        toggleOverviewFilter(OverviewFilter.SCHEDULED)

        navigateTo(MainMenu.MEDICINES)
        AndroidTestHelper.clickMedicineItem(0)
        createReminder("1", LocalTime.of(20, 0))

        navigateTo(MainMenu.OVERVIEW)

        assertContains("Test (1)")

        toggleOverviewFilter(OverviewFilter.SCHEDULED)
        assertContains("Test (1)")
    }

    @Test
    @AllowFlaky(attempts = 3)
    // Using internal assert
    fun overviewDaySelection() {
        openAppOptionsMenu()
        clickTag(AppOptionsTestTags.GENERATE_TEST_DATA_AND_EVENTS)

        val secondDay =
            DayOfWeek.SATURDAY.getDisplayName(TextStyle.SHORT, Locale.getDefault()) + "\n2"
        val today = DayOfWeek.FRIDAY.getDisplayName(TextStyle.SHORT, Locale.getDefault()) + "\n1"

        clickOn(secondDay)
        internalAssert(overviewEventCount() > 0)

        navigateTo(MainMenu.MEDICINES)

        navigateTo(MainMenu.OVERVIEW)

        internalAssert(selectedOverviewDay() == secondDay)

        navigateTo(MainMenu.OVERVIEW)

        navigateTo(MainMenu.OVERVIEW)
        internalAssert(selectedOverviewDay() == today)
        internalAssert(overviewEventCount() > 0)

        clickTag(OverviewTestTags.NEXT_WEEK)
        internalAssert(overviewEventCount() > 0)

        clickTag(OverviewTestTags.PREV_WEEK)
        clickTag(OverviewTestTags.PREV_WEEK)
        internalAssert(overviewEventCount() > 0)
    }

    companion object {
        const val TEST_2: String = "Test (2)"
    }
}
