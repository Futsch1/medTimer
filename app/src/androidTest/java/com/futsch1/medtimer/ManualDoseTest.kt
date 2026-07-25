package com.futsch1.medtimer

import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertContains
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertNotContains
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertNotDisplayed
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import com.adevinta.android.barista.interaction.BaristaEditTextInteractions.writeTo
import com.adevinta.android.barista.interaction.BaristaListInteractions
import com.adevinta.android.barista.interaction.BaristaListInteractions.clickListItem
import com.adevinta.android.barista.rule.flaky.AllowFlaky
import com.futsch1.medtimer.utilities.clickDialogPositiveButton
import org.hamcrest.Matchers.hasToString
import org.junit.Test
import com.futsch1.medtimer.core.ui.R
import com.futsch1.medtimer.feature.ui.AppOptionsTestTags
import com.futsch1.medtimer.feature.ui.overview.OverviewTestTags

private const val TEST_13_ = "Test (13)"

class ManualDoseTest : BaseTestHelper() {
    @Test
    @AllowFlaky(attempts = 3)
    fun testManualDose() {
        openAppOptionsMenu()
        clickTag(AppOptionsTestTags.GENERATE_TEST_DATA)

        AndroidTestHelper.navigateTo(AndroidTestHelper.MainMenu.OVERVIEW)

        clickTag(OverviewTestTags.LOG_MANUAL_DOSE)

        clickListItem(position = 3)
        writeTo(android.R.id.input, "12")
        clickDialogPositiveButton()
        clickOn(com.google.android.material.R.id.material_timepicker_ok_button)

        assertContains(com.futsch1.medtimer.feature.ui.R.id.reminderText, "Ginseng (200mg) (12)")

        clickTag(OverviewTestTags.LOG_MANUAL_DOSE)
        clickOn(R.string.custom)
        writeTo(android.R.id.input, "Test")
        clickDialogPositiveButton(false)
        clickDialogPositiveButton()
        clickOn(com.google.android.material.R.id.material_timepicker_ok_button)
        assertContains(com.futsch1.medtimer.feature.ui.R.id.reminderText, "Test")
        assertNotContains(com.futsch1.medtimer.feature.ui.R.id.reminderText, "Test (")

        clickTag(OverviewTestTags.LOG_MANUAL_DOSE)
        assertContains(com.futsch1.medtimer.feature.ui.R.id.entry_text, "Test")
        assertNotContains(com.futsch1.medtimer.feature.ui.R.id.reminderText, "Test (")
        clickOn("Test")
        writeTo(android.R.id.input, "13")
        clickDialogPositiveButton()
        clickOn(com.google.android.material.R.id.material_timepicker_ok_button)

        assertContains(com.futsch1.medtimer.feature.ui.R.id.reminderText, TEST_13_)

        clickTag(OverviewTestTags.LOG_MANUAL_DOSE)
        assertContains(com.futsch1.medtimer.feature.ui.R.id.entry_text, TEST_13_)
        clickOn(TEST_13_)
        assertContains("13")
        pressBack()

        AndroidTestHelper.navigateTo(AndroidTestHelper.MainMenu.MEDICINES)

        openMedicinesMenu()
        clickMenuItem(R.string.deactivate_all)

        AndroidTestHelper.navigateTo(AndroidTestHelper.MainMenu.OVERVIEW)
        clickTag(OverviewTestTags.LOG_MANUAL_DOSE)
        onData(hasToString("Selen (200 µg) (1)")).check(matches(isDisplayed()))
        onData(hasToString("Selen (200 µg) (1)")).perform(click())
        clickOn(com.google.android.material.R.id.material_timepicker_ok_button)
    }

    @Test
    @AllowFlaky(attempts = 3)
    fun testManualDoseOfDisabledReminder() {
        // Create medication + reminder
        AndroidTestHelper.createMedicine("Test")
        AndroidTestHelper.createReminder("1 pill", null)

        // Disable reminder
        clickOn(com.futsch1.medtimer.feature.ui.R.id.openAdvancedSettings)
        clickOn(R.string.reminder_enabled)

        // Create manual dose of the disabled reminder
        AndroidTestHelper.navigateTo(AndroidTestHelper.MainMenu.OVERVIEW)
        clickTag(OverviewTestTags.LOG_MANUAL_DOSE)
        clickListItem(position = 2)
        clickOn(com.google.android.material.R.id.material_timepicker_ok_button)

        // Check if the event is created properly
        clickOverviewEvent(0)
        assertNotContains(com.futsch1.medtimer.feature.ui.R.id.editEventName, "Test (1 pill)")
        assertContains(com.futsch1.medtimer.feature.ui.R.id.editEventName, "Test")
        assertContains(com.futsch1.medtimer.feature.ui.R.id.editEventAmount, "1 pill")

        pressBack()

        // Check that re-raise is not shown
        clickOverviewEventState(0)
        assertNotDisplayed(R.string.re_raise_event)
    }

}
