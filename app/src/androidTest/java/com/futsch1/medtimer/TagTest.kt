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
import com.adevinta.android.barista.interaction.BaristaMenuClickInteractions.openMenu
import com.adevinta.android.barista.internal.viewaction.ChipViewActions.removeChip
import com.adevinta.android.barista.rule.flaky.AllowFlaky
import com.futsch1.medtimer.AndroidTestHelper.createIntervalReminder
import com.futsch1.medtimer.AndroidTestHelper.createMedicine
import com.futsch1.medtimer.AndroidTestHelper.navigateTo
import com.futsch1.medtimer.utilities.clickDialogPositiveButton
import org.junit.Test

private const val NEW_TAG = "New tag"

private const val ANOTHER_TAG = "Another tag"

class TagTest : BaseTestHelper() {
    @Test
    @AllowFlaky(attempts = 3)
    fun tagHandling() {
        createMedicine("Test")

        clickOn(com.futsch1.medtimer.feature.ui.impl.R.id.openTags)

        addTag(NEW_TAG)
        assertContains(NEW_TAG)
        assertChecked(NEW_TAG)

        clickOn(com.futsch1.medtimer.feature.ui.impl.R.id.ok)

        clickOn(com.futsch1.medtimer.feature.ui.impl.R.id.openTags)
        assertContains(NEW_TAG)
        assertChecked(NEW_TAG)

        addTag(ANOTHER_TAG)
        assertContains(ANOTHER_TAG)
        assertChecked(ANOTHER_TAG)

        clickOn(ANOTHER_TAG)
        assertUnchecked(ANOTHER_TAG)
        clickOn(com.futsch1.medtimer.feature.ui.impl.R.id.ok)

        pressBack()

        AndroidTestHelper.assertTextDisplayed(NEW_TAG)
        AndroidTestHelper.assertTextNotDisplayed(ANOTHER_TAG)

        createMedicine("Test 2")

        clickOn(com.futsch1.medtimer.feature.ui.impl.R.id.openTags)
        assertUnchecked(NEW_TAG)
        assertUnchecked(ANOTHER_TAG)
        onView(withId(com.futsch1.medtimer.feature.ui.impl.R.id.tags)).perform(
            actionOnItemAtPosition<RecyclerView.ViewHolder>(
                1,
                removeChip()
            )
        )
        clickDialogPositiveButton()
        clickOn(com.futsch1.medtimer.feature.ui.impl.R.id.ok)

        clickOn(com.futsch1.medtimer.feature.ui.impl.R.id.openTags)
        assertNotContains(ANOTHER_TAG)
        clickOn(NEW_TAG)
        clickOn(com.futsch1.medtimer.feature.ui.impl.R.id.ok)

        openMenu()
        clickOn(com.futsch1.medtimer.core.ui.R.string.duplicate)
        AndroidTestHelper.clickMedicineItem(2)
        clickOn(com.futsch1.medtimer.feature.ui.impl.R.id.openTags)
        assertChecked(NEW_TAG)
    }

    @Test
    @AllowFlaky(attempts = 3)
    fun medicineVisibility() {
        createMedicine("Test")
        clickOn(com.futsch1.medtimer.feature.ui.impl.R.id.openTags)
        addTag("Tag1")
        clickOn(com.futsch1.medtimer.feature.ui.impl.R.id.ok)
        pressBack()

        createMedicine("Else")
        clickOn(com.futsch1.medtimer.feature.ui.impl.R.id.openTags)
        addTag("Tag2")
        clickOn(com.futsch1.medtimer.feature.ui.impl.R.id.ok)
        pressBack()

        AndroidTestHelper.assertMedicineNameContains("Test")
        AndroidTestHelper.assertMedicineNameContains("Else")

        clickOn(R.id.tag_filter)
        clickOn("Tag1")
        assertChecked("Tag1")
        assertUnchecked("Tag2")
        clickOn(com.futsch1.medtimer.feature.ui.impl.R.id.ok)

        AndroidTestHelper.assertMedicineNameContains("Test")
        AndroidTestHelper.assertMedicineNameNotContains("Else")

        clickOn(R.id.tag_filter)
        clickOn("Tag1")
        clickOn("Tag2")
        assertUnchecked("Tag1")
        assertChecked("Tag2")
        clickOn(com.futsch1.medtimer.feature.ui.impl.R.id.ok)

        AndroidTestHelper.assertMedicineNameNotContains("Test")
        AndroidTestHelper.assertMedicineNameContains("Else")
    }

    @Test
    @AllowFlaky(attempts = 3)
    fun activateAndOverviewVisibility() {
        createMedicine("Test")
        clickOn(com.futsch1.medtimer.feature.ui.impl.R.id.openTags)
        addTag("Tag1")
        clickOn(com.futsch1.medtimer.feature.ui.impl.R.id.ok)
        createIntervalReminder("Amount1", 60)
        pressBack()
        pressBack()

        createMedicine("Else")
        clickOn(com.futsch1.medtimer.feature.ui.impl.R.id.openTags)
        addTag("Tag2")
        clickOn(com.futsch1.medtimer.feature.ui.impl.R.id.ok)
        createIntervalReminder("Amount2", 60)
        pressBack()

        // First, deactivate all of Test
        clickOn(R.id.tag_filter)
        clickOn("Tag1")
        clickOn(com.futsch1.medtimer.feature.ui.impl.R.id.ok)

        openMenu()
        clickOn(com.futsch1.medtimer.core.ui.R.string.deactivate_all)
        AndroidTestHelper.assertTextDisplayed(com.futsch1.medtimer.core.ui.R.string.inactive)

        // Now, check that Else is not deactivated
        clickOn(R.id.tag_filter)
        clickOn("Tag1")
        clickOn("Tag2")
        clickOn(com.futsch1.medtimer.feature.ui.impl.R.id.ok)

        AndroidTestHelper.assertTextNotDisplayed(com.futsch1.medtimer.core.ui.R.string.inactive)

        clickOn(R.id.tag_filter)
        clickOn("Tag2")
        clickOn(com.futsch1.medtimer.feature.ui.impl.R.id.ok)

        // And activate Test again
        openMenu()
        clickOn(com.futsch1.medtimer.core.ui.R.string.activate_all)

        navigateTo(AndroidTestHelper.MainMenu.OVERVIEW)

        BaristaListInteractions.clickListItemChild(com.futsch1.medtimer.feature.ui.impl.R.id.reminders, 0, com.futsch1.medtimer.feature.ui.impl.R.id.stateButton)
        clickOn(com.futsch1.medtimer.feature.ui.impl.R.id.takenButton)
        BaristaListInteractions.clickListItemChild(com.futsch1.medtimer.feature.ui.impl.R.id.reminders, 1, com.futsch1.medtimer.feature.ui.impl.R.id.stateButton)
        clickOn(com.futsch1.medtimer.feature.ui.impl.R.id.takenButton)

        clickOn(R.id.tag_filter)
        clickOn("Tag1")
        clickOn(com.futsch1.medtimer.feature.ui.impl.R.id.ok)

        assertContains(com.futsch1.medtimer.feature.ui.impl.R.id.reminderText, "Amount1")
        assertNotContains(com.futsch1.medtimer.feature.ui.impl.R.id.reminderText, "Amount2")

        clickOn(R.id.tag_filter)
        clickOn("Tag1")
        clickOn("Tag2")
        clickOn(com.futsch1.medtimer.feature.ui.impl.R.id.ok)

        assertNotContains(com.futsch1.medtimer.feature.ui.impl.R.id.reminderText, "Amount1")
        assertContains(com.futsch1.medtimer.feature.ui.impl.R.id.reminderText, "Amount2")
    }

    private fun addTag(tagName: String) {
        clickOn(com.futsch1.medtimer.feature.ui.impl.R.id.addTag)
        writeTo(android.R.id.input, tagName)
        clickDialogPositiveButton()
    }
}