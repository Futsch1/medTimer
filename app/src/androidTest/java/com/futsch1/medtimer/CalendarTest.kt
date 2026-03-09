package com.futsch1.medtimer

import androidx.test.espresso.Espresso
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertContains
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertNotContains
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import com.adevinta.android.barista.interaction.BaristaDialogInteractions
import com.adevinta.android.barista.interaction.BaristaEditTextInteractions.writeTo
import com.adevinta.android.barista.interaction.BaristaListInteractions.clickListItem
import com.adevinta.android.barista.interaction.BaristaListInteractions.clickListItemChild
import com.adevinta.android.barista.interaction.BaristaMenuClickInteractions.openMenu
import com.futsch1.medtimer.AndroidTestHelper.navigateTo
import org.junit.Test


class CalendarTest : BaseTestHelper() {
    //@AllowFlaky(attempts = 1)
    @Test
    fun calendarTest() {
        openMenu()
        clickOn(R.string.generate_test_data)

        clickListItemChild(R.id.reminders, 0, R.id.stateButton)
        clickOn(R.id.takenButton)
        clickListItemChild(R.id.reminders, 2, R.id.stateButton)
        clickOn(R.id.takenButton)
        clickListItemChild(R.id.reminders, 3, R.id.stateButton)
        clickOn(R.id.takenButton)
        clickListItemChild(R.id.reminders, 4, R.id.stateButton)
        clickOn(R.id.takenButton)

        navigateTo(AndroidTestHelper.MainMenu.MEDICINES)

        clickListItem(R.id.medicineList, 0)
        clickOn(R.id.openCalendar)
        assertContains(R.id.currentDayEvents, "Omega 3 (EPA/DHA 500mg)")
        Espresso.pressBack()

        navigateTo(AndroidTestHelper.MainMenu.ANALYSIS)

        clickOn(R.id.calendarChip)
        assertContains(R.id.currentDayEvents, "Selen (200 µg)")
    }

    @Test //@AllowFlaky(attempts = 1)
    fun testDeletedEventNotInCalendarView() {
        // Create event
        clickOn(R.id.logManualDose)
        clickOn(R.string.custom)
        writeTo(android.R.id.input, "Test")
        BaristaDialogInteractions.clickDialogPositiveButton()
        BaristaDialogInteractions.clickDialogPositiveButton()
        clickOn(com.google.android.material.R.id.material_timepicker_ok_button)

        // Delete event
        clickListItemChild(
            R.id.reminders, 0, R.id.stateButton
        )
        clickOn(R.id.deleteButton)
        BaristaDialogInteractions.clickDialogPositiveButton()

        // Check that the event is not listed in the calendar view
        navigateTo(AndroidTestHelper.MainMenu.ANALYSIS)
        clickOn(R.id.calendarChip)
        assertNotContains(R.id.currentDayEvents, "Test")
    }
}
