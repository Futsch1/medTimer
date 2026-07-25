package com.futsch1.medtimer

import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.adevinta.android.barista.assertion.BaristaCheckedAssertions.assertChecked
import com.adevinta.android.barista.assertion.BaristaCheckedAssertions.assertUnchecked
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertContains
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertNotContains
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import com.adevinta.android.barista.interaction.BaristaEditTextInteractions.writeTo
import com.adevinta.android.barista.interaction.BaristaListInteractions
import com.adevinta.android.barista.internal.viewaction.ChipViewActions.removeChip
import com.adevinta.android.barista.rule.flaky.AllowFlaky
import com.futsch1.medtimer.AndroidTestHelper.createIntervalReminder
import com.futsch1.medtimer.AndroidTestHelper.createMedicine
import com.futsch1.medtimer.AndroidTestHelper.navigateTo
import com.futsch1.medtimer.utilities.clickDialogPositiveButton
import org.junit.Test
import com.futsch1.medtimer.feature.ui.AppOptionsTestTags

private const val NEW_TAG = "New tag"

private const val ANOTHER_TAG = "Another tag"

class TagTest : BaseTestHelper() {
    @Test
    @AllowFlaky(attempts = 3)
    fun tagHandling() {
        createMedicine("Test")

        clickMenuItem(com.futsch1.medtimer.core.ui.R.string.tags)

        addTag(NEW_TAG)
        assertContains(NEW_TAG)
        assertChecked(NEW_TAG)

        clickOn(com.futsch1.medtimer.feature.ui.R.id.ok)

        clickMenuItem(com.futsch1.medtimer.core.ui.R.string.tags)
        assertContains(NEW_TAG)
        assertChecked(NEW_TAG)

        addTag(ANOTHER_TAG)
        assertContains(ANOTHER_TAG)
        assertChecked(ANOTHER_TAG)

        clickOn(ANOTHER_TAG)
        assertUnchecked(ANOTHER_TAG)
        clickOn(com.futsch1.medtimer.feature.ui.R.id.ok)

        pressBack()

        AndroidTestHelper.assertTextDisplayed(NEW_TAG)
        AndroidTestHelper.assertTextNotDisplayed(ANOTHER_TAG)

        createMedicine("Test 2")

        clickMenuItem(com.futsch1.medtimer.core.ui.R.string.tags)
        assertUnchecked(NEW_TAG)
        assertUnchecked(ANOTHER_TAG)
        onView(withId(com.futsch1.medtimer.feature.ui.R.id.tags)).perform(
            actionOnItemAtPosition<RecyclerView.ViewHolder>(
                1,
                removeChip()
            )
        )
        clickDialogPositiveButton()
        clickOn(com.futsch1.medtimer.feature.ui.R.id.ok)

        clickMenuItem(com.futsch1.medtimer.core.ui.R.string.tags)
        assertNotContains(ANOTHER_TAG)
        clickOn(NEW_TAG)
        clickOn(com.futsch1.medtimer.feature.ui.R.id.ok)

        openEditMedicineMenu()
        clickMenuItem(com.futsch1.medtimer.core.ui.R.string.duplicate)
        AndroidTestHelper.clickMedicineItem(2)
        clickMenuItem(com.futsch1.medtimer.core.ui.R.string.tags)
        assertChecked(NEW_TAG)
    }

    @Test
    @AllowFlaky(attempts = 3)
    fun medicineVisibility() {
        createMedicine("Test")
        clickMenuItem(com.futsch1.medtimer.core.ui.R.string.tags)
        addTag("Tag1")
        clickOn(com.futsch1.medtimer.feature.ui.R.id.ok)
        pressBack()

        createMedicine("Else")
        clickMenuItem(com.futsch1.medtimer.core.ui.R.string.tags)
        addTag("Tag2")
        clickOn(com.futsch1.medtimer.feature.ui.R.id.ok)
        pressBack()

        AndroidTestHelper.assertMedicineNameContains("Test")
        AndroidTestHelper.assertMedicineNameContains("Else")

        clickTag(AppOptionsTestTags.TAG_FILTER)
        clickOn("Tag1")
        assertChecked("Tag1")
        assertUnchecked("Tag2")
        clickOn(com.futsch1.medtimer.feature.ui.R.id.ok)

        AndroidTestHelper.assertMedicineNameContains("Test")
        AndroidTestHelper.assertMedicineNameNotContains("Else")

        clickTag(AppOptionsTestTags.TAG_FILTER)
        clickOn("Tag1")
        clickOn("Tag2")
        assertUnchecked("Tag1")
        assertChecked("Tag2")
        clickOn(com.futsch1.medtimer.feature.ui.R.id.ok)

        AndroidTestHelper.assertMedicineNameNotContains("Test")
        AndroidTestHelper.assertMedicineNameContains("Else")
    }

    @Test
    @AllowFlaky(attempts = 3)
    fun activateAndOverviewVisibility() {
        createMedicine("Test")
        clickMenuItem(com.futsch1.medtimer.core.ui.R.string.tags)
        addTag("Tag1")
        clickOn(com.futsch1.medtimer.feature.ui.R.id.ok)
        createIntervalReminder("Amount1", 60)
        pressBack()
        pressBack()

        createMedicine("Else")
        clickMenuItem(com.futsch1.medtimer.core.ui.R.string.tags)
        addTag("Tag2")
        clickOn(com.futsch1.medtimer.feature.ui.R.id.ok)
        createIntervalReminder("Amount2", 60)
        pressBack()

        // First, deactivate all of Test
        clickTag(AppOptionsTestTags.TAG_FILTER)
        clickOn("Tag1")
        clickOn(com.futsch1.medtimer.feature.ui.R.id.ok)

        openMedicinesMenu()
        clickMenuItem(com.futsch1.medtimer.core.ui.R.string.deactivate_all)
        AndroidTestHelper.assertTextDisplayed(com.futsch1.medtimer.core.ui.R.string.inactive)

        // Now, check that Else is not deactivated
        clickTag(AppOptionsTestTags.TAG_FILTER)
        clickOn("Tag1")
        clickOn("Tag2")
        clickOn(com.futsch1.medtimer.feature.ui.R.id.ok)

        AndroidTestHelper.assertTextNotDisplayed(com.futsch1.medtimer.core.ui.R.string.inactive)

        clickTag(AppOptionsTestTags.TAG_FILTER)
        clickOn("Tag2")
        clickOn(com.futsch1.medtimer.feature.ui.R.id.ok)

        // And activate Test again
        openMedicinesMenu()
        clickMenuItem(com.futsch1.medtimer.core.ui.R.string.activate_all)

        navigateTo(AndroidTestHelper.MainMenu.OVERVIEW)

        clickOverviewEventState(0)
        clickMenuItem(com.futsch1.medtimer.core.ui.R.string.taken)
        clickOverviewEventState(1)
        clickMenuItem(com.futsch1.medtimer.core.ui.R.string.taken)

        clickTag(AppOptionsTestTags.TAG_FILTER)
        clickOn("Tag1")
        clickOn(com.futsch1.medtimer.feature.ui.R.id.ok)

        assertContains(com.futsch1.medtimer.feature.ui.R.id.reminderText, "Amount1")
        assertNotContains(com.futsch1.medtimer.feature.ui.R.id.reminderText, "Amount2")

        clickTag(AppOptionsTestTags.TAG_FILTER)
        clickOn("Tag1")
        clickOn("Tag2")
        clickOn(com.futsch1.medtimer.feature.ui.R.id.ok)

        assertNotContains(com.futsch1.medtimer.feature.ui.R.id.reminderText, "Amount1")
        assertContains(com.futsch1.medtimer.feature.ui.R.id.reminderText, "Amount2")
    }

    private fun addTag(tagName: String) {
        clickOn(com.futsch1.medtimer.feature.ui.R.id.addTag)
        writeTo(android.R.id.input, tagName)
        clickDialogPositiveButton()
    }
}