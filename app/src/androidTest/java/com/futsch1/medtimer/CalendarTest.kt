package com.futsch1.medtimer

import androidx.test.espresso.Espresso
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertContains
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertNotContains
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import com.adevinta.android.barista.interaction.BaristaEditTextInteractions.writeTo
import com.adevinta.android.barista.interaction.BaristaListInteractions.clickListItem
import com.adevinta.android.barista.interaction.BaristaListInteractions.clickListItemChild
import com.adevinta.android.barista.interaction.BaristaMenuClickInteractions.openMenu
import com.adevinta.android.barista.rule.flaky.AllowFlaky
import com.futsch1.medtimer.AndroidTestHelper.navigateTo
import com.futsch1.medtimer.utilities.clickDialogPositiveButton
import org.junit.Test


class CalendarTest : BaseTestHelper() {
    @Test
    @AllowFlaky(attempts = 3)
    fun calendarTest() {
        openMenu()
        clickOn(com.futsch1.medtimer.feature.ui.R.string.generate_test_data)

        clickListItemChild(com.futsch1.medtimer.feature.ui.R.id.reminders, 0, com.futsch1.medtimer.feature.ui.R.id.stateButton)
        clickOn(com.futsch1.medtimer.feature.ui.R.id.takenButton)
        clickListItemChild(com.futsch1.medtimer.feature.ui.R.id.reminders, 2, com.futsch1.medtimer.feature.ui.R.id.stateButton)
        clickOn(com.futsch1.medtimer.feature.ui.R.id.takenButton)
        clickListItemChild(com.futsch1.medtimer.feature.ui.R.id.reminders, 3, com.futsch1.medtimer.feature.ui.R.id.stateButton)
        clickOn(com.futsch1.medtimer.feature.ui.R.id.takenButton)
        clickListItemChild(com.futsch1.medtimer.feature.ui.R.id.reminders, 4, com.futsch1.medtimer.feature.ui.R.id.stateButton)
        clickOn(com.futsch1.medtimer.feature.ui.R.id.takenButton)

        navigateTo(AndroidTestHelper.MainMenu.MEDICINES)

        clickListItem(com.futsch1.medtimer.feature.ui.R.id.medicineList, 0)
        clickOn(com.futsch1.medtimer.feature.ui.R.id.openCalendar)
        assertContains(com.futsch1.medtimer.feature.ui.R.id.currentDayEvents, "Omega 3 (EPA/DHA 500mg)")
        Espresso.pressBack()

        navigateTo(AndroidTestHelper.MainMenu.ANALYSIS)

        clickOn(com.futsch1.medtimer.feature.ui.R.id.calendarChip)
        assertContains(com.futsch1.medtimer.feature.ui.R.id.currentDayEvents, "Selen (200 µg)")
    }

    @Test
    @AllowFlaky(attempts = 3)
    fun testDeletedEventNotInCalendarView() {
        // Create event
        clickOn(com.futsch1.medtimer.feature.ui.R.id.logManualDose)
        clickOn(com.futsch1.medtimer.feature.ui.R.string.custom)
        writeTo(android.R.id.input, "Test")
        clickDialogPositiveButton(false)
        clickDialogPositiveButton()
        clickOn(com.google.android.material.R.id.material_timepicker_ok_button)

        // Delete event
        clickListItemChild(
            com.futsch1.medtimer.feature.ui.R.id.reminders, 0, com.futsch1.medtimer.feature.ui.R.id.stateButton
        )
        clickOn(com.futsch1.medtimer.feature.ui.R.id.deleteButton)
        clickDialogPositiveButton()

        // Check that the event is not listed in the calendar view
        navigateTo(AndroidTestHelper.MainMenu.ANALYSIS)
        clickOn(com.futsch1.medtimer.feature.ui.R.id.calendarChip)
        assertNotContains(com.futsch1.medtimer.feature.ui.R.id.currentDayEvents, "Test")
    }
}
