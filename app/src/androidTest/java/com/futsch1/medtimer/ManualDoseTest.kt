package com.futsch1.medtimer

import androidx.test.espresso.Espresso.pressBack
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertContains
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertNotContains
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import com.adevinta.android.barista.interaction.BaristaEditTextInteractions.writeTo
import com.adevinta.android.barista.rule.flaky.AllowFlaky
import com.futsch1.medtimer.core.ui.R
import com.futsch1.medtimer.utilities.clickDialogPositiveButton
import org.junit.Test

private const val TEST_13_ = "Test (13)"
private const val GINSENG = "Ginseng (200mg)"
private const val SELEN_1 = "Selen (200 µg) (1)"
private const val TEST_1_PILL = "Test (1 pill)"

class ManualDoseTest : MedTimerTestBase() {
    @Test
    @AllowFlaky(attempts = 3)
    fun testManualDose() {
        menus.clickAppOption(R.string.generate_test_data)

        navigation.toOverview()

        overview.logManualDose()

        clickDialogItem(GINSENG)
        writeTo(android.R.id.input, "12")
        clickDialogPositiveButton()
        clickOn(com.google.android.material.R.id.material_timepicker_ok_button)

        overview.assertEventContains("Ginseng (200mg) (12)")

        overview.logManualDose()
        clickDialogItem(R.string.custom)
        writeTo(android.R.id.input, "Test")
        clickDialogPositiveButton(false)
        clickDialogPositiveButton()
        clickOn(com.google.android.material.R.id.material_timepicker_ok_button)
        overview.assertEventContains("Test")
        overview.assertNoEventContains("Test (")

        overview.logManualDose()
        assertContains(com.futsch1.medtimer.feature.ui.R.id.entry_text, "Test")
        overview.assertNoEventContains("Test (")
        clickDialogItem("Test")
        writeTo(android.R.id.input, "13")
        clickDialogPositiveButton()
        clickOn(com.google.android.material.R.id.material_timepicker_ok_button)

        overview.assertEventContains(TEST_13_)

        overview.logManualDose()
        assertContains(com.futsch1.medtimer.feature.ui.R.id.entry_text, TEST_13_)
        clickDialogItem(TEST_13_)
        assertContains(android.R.id.input, "13")
        pressBack()

        navigation.toMedicines()

        menus.clickMedicinesOption(R.string.deactivate_all)

        navigation.toOverview()
        overview.logManualDose()
        assertDialogItemDisplayed(SELEN_1)
        clickDialogItem(SELEN_1)
        clickOn(com.google.android.material.R.id.material_timepicker_ok_button)
    }

    @Test
    @AllowFlaky(attempts = 3)
    fun testManualDoseOfDisabledReminder() {
        // Create medication + reminder
        medicines.create("Test")
        medicineEditor.addReminder("1 pill", null)

        // Disable reminder
        medicineEditor.openAdvancedSettings()
        clickPreference(R.string.reminder_enabled)

        // Create manual dose of the disabled reminder
        navigation.toOverview()
        overview.logManualDose()
        clickDialogItem(TEST_1_PILL)
        clickOn(com.google.android.material.R.id.material_timepicker_ok_button)

        // Check if the event is created properly
        overview.clickEvent(0)
        assertNotContains(com.futsch1.medtimer.feature.ui.R.id.editEventName, "Test (1 pill)")
        assertContains(com.futsch1.medtimer.feature.ui.R.id.editEventName, "Test")
        assertContains(com.futsch1.medtimer.feature.ui.R.id.editEventAmount, "1 pill")

        pressBack()

        // Check that re-raise is not shown
        overview.clickEventState(0)
        overview.assertActionAbsent(R.string.re_raise_event)
    }

}
