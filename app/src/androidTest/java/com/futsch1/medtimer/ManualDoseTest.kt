package com.futsch1.medtimer

import androidx.test.espresso.Espresso.pressBack
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertContains
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertNotContains
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertNotDisplayed
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import com.adevinta.android.barista.interaction.BaristaDialogInteractions
import com.adevinta.android.barista.interaction.BaristaEditTextInteractions.writeTo
import com.adevinta.android.barista.interaction.BaristaListInteractions
import com.adevinta.android.barista.interaction.BaristaListInteractions.clickListItem
import com.adevinta.android.barista.interaction.BaristaMenuClickInteractions.openMenu
import org.junit.Test

private const val TEST_13_ = "Test (13)"

class ManualDoseTest : BaseTestHelper() {
    @Test
    //@AllowFlaky(attempts = 1)
    fun testManualDose() {
        openMenu()
        clickOn(R.string.generate_test_data)

        AndroidTestHelper.navigateTo(AndroidTestHelper.MainMenu.OVERVIEW)

        clickOn(R.id.logManualDose)

        clickListItem(position = 3)
        writeTo(android.R.id.input, "12")
        BaristaDialogInteractions.clickDialogPositiveButton()
        clickOn(com.google.android.material.R.id.material_timepicker_ok_button)

        assertContains(R.id.reminderText, "Ginseng (200mg) (12)")

        clickOn(R.id.logManualDose)
        clickOn(R.string.custom)
        writeTo(android.R.id.input, "Test")
        BaristaDialogInteractions.clickDialogPositiveButton()
        BaristaDialogInteractions.clickDialogPositiveButton()
        clickOn(com.google.android.material.R.id.material_timepicker_ok_button)
        assertContains(R.id.reminderText, "Test")
        assertNotContains(R.id.reminderText, "Test (")

        clickOn(R.id.logManualDose)
        assertContains(R.id.entry_text, "Test")
        assertNotContains(R.id.reminderText, "Test (")
        clickOn("Test")
        writeTo(android.R.id.input, "13")
        BaristaDialogInteractions.clickDialogPositiveButton()
        clickOn(com.google.android.material.R.id.material_timepicker_ok_button)

        assertContains(R.id.reminderText, TEST_13_)

        clickOn(R.id.logManualDose)
        assertContains(R.id.entry_text, TEST_13_)
        clickOn(TEST_13_)
        assertContains("13")
        pressBack()

        AndroidTestHelper.navigateTo(AndroidTestHelper.MainMenu.MEDICINES)

        openMenu()
        clickOn(R.string.deactivate_all)

        AndroidTestHelper.navigateTo(AndroidTestHelper.MainMenu.OVERVIEW)
        clickOn(R.id.logManualDose)
        assertContains(R.id.entry_text, "Selen (200 µg) (1)")
        clickOn("Selen (200 µg) (1)")
        clickOn(com.google.android.material.R.id.material_timepicker_ok_button)
    }

    @Test
    //@AllowFlaky(attempts = 1)
    fun testManualDoseOfDisabledReminder() {
        // Create medication + reminder
        AndroidTestHelper.createMedicine("Test")
        AndroidTestHelper.createReminder("1 pill", null)

        // Disable reminder
        clickOn(R.id.openAdvancedSettings)
        clickOn(R.string.reminder_status)
        clickOn(R.string.active)

        // Create manual dose of the disabled reminder
        AndroidTestHelper.navigateTo(AndroidTestHelper.MainMenu.OVERVIEW)
        clickOn(R.id.logManualDose)
        clickListItem(position = 2)
        clickOn(com.google.android.material.R.id.material_timepicker_ok_button)

        // Check if the event is created properly
        clickOn(R.id.overviewContentContainer)
        assertNotContains(R.id.editEventName, "Test (1 pill)")
        assertContains(R.id.editEventName, "Test")
        assertContains(R.id.editEventAmount, "1 pill")

        pressBack()

        // Check that re-raise is not shown
        BaristaListInteractions.clickListItemChild(R.id.reminders, 0, R.id.stateButton)
        assertNotDisplayed(R.string.re_raise_event)
    }

}
