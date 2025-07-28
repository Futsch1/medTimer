package com.futsch1.medtimer

import androidx.test.espresso.Espresso.pressBack
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertContains
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertNotContains
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import com.adevinta.android.barista.interaction.BaristaDialogInteractions
import com.adevinta.android.barista.interaction.BaristaEditTextInteractions.writeTo
import com.adevinta.android.barista.interaction.BaristaListInteractions.clickListItem
import com.adevinta.android.barista.interaction.BaristaMenuClickInteractions.openMenu
import org.junit.Test

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

        assertContains(R.id.reminderText, "Test (13)")

        clickOn(R.id.logManualDose)
        assertContains(R.id.entry_text, "Test (13)")
        clickOn("Test (13)")
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

}