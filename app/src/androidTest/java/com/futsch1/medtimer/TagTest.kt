package com.futsch1.medtimer

import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import com.adevinta.android.barista.interaction.BaristaEditTextInteractions.writeTo
import com.adevinta.android.barista.internal.viewaction.ChipViewActions.removeChip
import com.adevinta.android.barista.rule.flaky.AllowFlaky
import com.futsch1.medtimer.feature.ui.AppOptionsTestTags
import com.futsch1.medtimer.utilities.clickDialogPositiveButton
import org.junit.Test

private const val NEW_TAG = "New tag"

private const val ANOTHER_TAG = "Another tag"

class TagTest : MedTimerTestBase() {
    @Test
    @AllowFlaky(attempts = 3)
    fun tagHandling() {
        medicines.create("Test")

        openEditMedicineMenu()
        clickMenuItem(com.futsch1.medtimer.core.ui.R.string.tags)

        addTag(NEW_TAG)
        assertTagChipDisplayed(NEW_TAG)
        assertTagChipChecked(NEW_TAG)

        clickOn(com.futsch1.medtimer.feature.ui.R.id.ok)

        openEditMedicineMenu()
        clickMenuItem(com.futsch1.medtimer.core.ui.R.string.tags)
        assertTagChipDisplayed(NEW_TAG)
        assertTagChipChecked(NEW_TAG)

        addTag(ANOTHER_TAG)
        assertTagChipDisplayed(ANOTHER_TAG)
        assertTagChipChecked(ANOTHER_TAG)

        clickTagChip(ANOTHER_TAG)
        assertTagChipNotChecked(ANOTHER_TAG)
        clickOn(com.futsch1.medtimer.feature.ui.R.id.ok)

        pressBack()

        medicines.assertListContains(NEW_TAG)
        medicines.assertListDoesNotContain(ANOTHER_TAG)

        medicines.create("Test 2")

        openEditMedicineMenu()
        clickMenuItem(com.futsch1.medtimer.core.ui.R.string.tags)
        assertTagChipNotChecked(NEW_TAG)
        assertTagChipNotChecked(ANOTHER_TAG)
        onView(withId(com.futsch1.medtimer.feature.ui.R.id.tags)).perform(
            actionOnItemAtPosition<RecyclerView.ViewHolder>(
                1,
                removeChip()
            )
        )
        clickDialogPositiveButton()
        clickOn(com.futsch1.medtimer.feature.ui.R.id.ok)

        openEditMedicineMenu()
        clickMenuItem(com.futsch1.medtimer.core.ui.R.string.tags)
        assertTagChipDoesNotExist(ANOTHER_TAG)
        clickTagChip(NEW_TAG)
        clickOn(com.futsch1.medtimer.feature.ui.R.id.ok)

        openEditMedicineMenu()
        clickMenuItem(com.futsch1.medtimer.core.ui.R.string.duplicate)
        medicines.clickItem(2)
        openEditMedicineMenu()
        clickMenuItem(com.futsch1.medtimer.core.ui.R.string.tags)
        assertTagChipChecked(NEW_TAG)
    }

    @Test
    @AllowFlaky(attempts = 3)
    fun medicineVisibility() {
        medicines.create("Test")
        openEditMedicineMenu()
        clickMenuItem(com.futsch1.medtimer.core.ui.R.string.tags)
        addTag("Tag1")
        clickOn(com.futsch1.medtimer.feature.ui.R.id.ok)
        pressBack()

        medicines.create("Else")
        openEditMedicineMenu()
        clickMenuItem(com.futsch1.medtimer.core.ui.R.string.tags)
        addTag("Tag2")
        clickOn(com.futsch1.medtimer.feature.ui.R.id.ok)
        pressBack()

        medicines.assertNameContains("Test")
        medicines.assertNameContains("Else")

        clickTag(AppOptionsTestTags.TAG_FILTER)
        clickTagChip("Tag1")
        assertTagChipChecked("Tag1")
        assertTagChipNotChecked("Tag2")
        clickOn(com.futsch1.medtimer.feature.ui.R.id.ok)

        medicines.assertNameContains("Test")
        medicines.assertNameNotContains("Else")

        clickTag(AppOptionsTestTags.TAG_FILTER)
        clickTagChip("Tag1")
        clickTagChip("Tag2")
        assertTagChipNotChecked("Tag1")
        assertTagChipChecked("Tag2")
        clickOn(com.futsch1.medtimer.feature.ui.R.id.ok)

        medicines.assertNameNotContains("Test")
        medicines.assertNameContains("Else")
    }

    @Test
    @AllowFlaky(attempts = 3)
    fun activateAndOverviewVisibility() {
        medicines.create("Test")
        openEditMedicineMenu()
        clickMenuItem(com.futsch1.medtimer.core.ui.R.string.tags)
        addTag("Tag1")
        clickOn(com.futsch1.medtimer.feature.ui.R.id.ok)
        medicineEditor.addIntervalReminder("Amount1", 60)
        pressBack()
        pressBack()

        medicines.create("Else")
        openEditMedicineMenu()
        clickMenuItem(com.futsch1.medtimer.core.ui.R.string.tags)
        addTag("Tag2")
        clickOn(com.futsch1.medtimer.feature.ui.R.id.ok)
        medicineEditor.addIntervalReminder("Amount2", 60)
        pressBack()

        // First, deactivate all of Test
        clickTag(AppOptionsTestTags.TAG_FILTER)
        clickTagChip("Tag1")
        clickOn(com.futsch1.medtimer.feature.ui.R.id.ok)

        openMedicinesMenu()
        clickMenuItem(com.futsch1.medtimer.core.ui.R.string.deactivate_all)
        medicines.assertListContains(com.futsch1.medtimer.core.ui.R.string.inactive)

        // Now, check that Else is not deactivated
        clickTag(AppOptionsTestTags.TAG_FILTER)
        clickTagChip("Tag1")
        clickTagChip("Tag2")
        clickOn(com.futsch1.medtimer.feature.ui.R.id.ok)

        medicines.assertListDoesNotContain(com.futsch1.medtimer.core.ui.R.string.inactive)

        clickTag(AppOptionsTestTags.TAG_FILTER)
        clickTagChip("Tag2")
        clickOn(com.futsch1.medtimer.feature.ui.R.id.ok)

        // And activate Test again
        openMedicinesMenu()
        clickMenuItem(com.futsch1.medtimer.core.ui.R.string.activate_all)

        navigation.toOverview()

        overview.clickEventState(0)
        clickMenuItem(com.futsch1.medtimer.core.ui.R.string.taken)
        overview.clickEventState(1)
        clickMenuItem(com.futsch1.medtimer.core.ui.R.string.taken)

        clickTag(AppOptionsTestTags.TAG_FILTER)
        clickTagChip("Tag1")
        clickOn(com.futsch1.medtimer.feature.ui.R.id.ok)

        overview.assertEventContains("Amount1")
        overview.assertNoEventContains("Amount2")

        clickTag(AppOptionsTestTags.TAG_FILTER)
        clickTagChip("Tag1")
        clickTagChip("Tag2")
        clickOn(com.futsch1.medtimer.feature.ui.R.id.ok)

        overview.assertNoEventContains("Amount1")
        overview.assertEventContains("Amount2")
    }

    private fun addTag(tagName: String) {
        clickOn(com.futsch1.medtimer.feature.ui.R.id.addTag)
        writeTo(android.R.id.input, tagName)
        clickDialogPositiveButton()
    }
}