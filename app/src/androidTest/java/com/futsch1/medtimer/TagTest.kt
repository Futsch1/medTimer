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
import com.adevinta.android.barista.interaction.BaristaDialogInteractions.clickDialogPositiveButton
import com.adevinta.android.barista.interaction.BaristaEditTextInteractions.writeTo
import com.adevinta.android.barista.interaction.BaristaListInteractions.clickListItemChild
import com.adevinta.android.barista.interaction.BaristaMenuClickInteractions.openMenu
import com.adevinta.android.barista.internal.viewaction.ChipViewActions.removeChip
import com.futsch1.medtimer.AndroidTestHelper.createIntervalReminder
import com.futsch1.medtimer.AndroidTestHelper.createMedicine
import com.futsch1.medtimer.AndroidTestHelper.navigateTo
import org.junit.Test

class TagTest : BaseTestHelper() {
    @Test
    //@AllowFlaky(attempts = 1)
    fun tagHandling() {
        createMedicine("Test")

        clickOn(R.id.openTags)

        addTag("New tag")
        assertContains("New tag")
        assertChecked("New tag")

        clickOn(R.id.ok)

        clickOn(R.id.openTags)
        assertContains("New tag")
        assertChecked("New tag")

        addTag("Another tag")
        assertContains("Another tag")
        assertChecked("Another tag")

        clickOn("Another tag")
        assertUnchecked("Another tag")
        clickOn(R.id.ok)

        pressBack()

        assertContains("New tag")
        assertNotContains("Another tag")

        createMedicine("Test 2")

        clickOn(R.id.openTags)
        assertUnchecked("New tag")
        assertUnchecked("Another tag")
        onView(withId(R.id.tags)).perform(
            actionOnItemAtPosition<RecyclerView.ViewHolder>(
                1,
                removeChip()
            )
        )
        clickDialogPositiveButton()
        clickOn(R.id.ok)

        clickOn(R.id.openTags)
        assertNotContains("Another tag")
    }

    @Test
    //@AllowFlaky(attempts = 1)
    fun medicineVisibility() {
        createMedicine("Test")
        clickOn(R.id.openTags)
        addTag("Tag1")
        clickOn(R.id.ok)
        pressBack()

        createMedicine("Else")
        clickOn(R.id.openTags)
        addTag("Tag2")
        clickOn(R.id.ok)
        pressBack()

        assertContains("Test")
        assertContains("Else")

        clickOn(R.id.tag_filter)
        clickOn("Tag1")
        assertChecked("Tag1")
        assertUnchecked("Tag2")
        clickOn(R.id.ok)

        assertContains("Test")
        assertNotContains("Else")

        clickOn(R.id.tag_filter)
        clickOn("Tag1")
        clickOn("Tag2")
        assertUnchecked("Tag1")
        assertChecked("Tag2")
        clickOn(R.id.ok)

        assertNotContains("Test")
        assertContains("Else")
    }

    @Test
    //@AllowFlaky(attempts = 1)
    fun activateAndOverviewVisibility() {
        createMedicine("Test")
        clickOn(R.id.openTags)
        addTag("Tag1")
        clickOn(R.id.ok)
        createIntervalReminder("Amount1", 60)
        pressBack()
        pressBack()

        createMedicine("Else")
        clickOn(R.id.openTags)
        addTag("Tag2")
        clickOn(R.id.ok)
        createIntervalReminder("Amount2", 60)
        pressBack()

        // First, deactivate all of Test
        clickOn(R.id.tag_filter)
        clickOn("Tag1")
        clickOn(R.id.ok)

        openMenu()
        clickOn(R.string.deactivate_all)
        assertContains(R.string.inactive)

        // Now, check that Else is not deactivated
        clickOn(R.id.tag_filter)
        clickOn("Tag1")
        clickOn("Tag2")
        clickOn(R.id.ok)

        assertNotContains(R.string.inactive)

        clickOn(R.id.tag_filter)
        clickOn("Tag2")
        clickOn(R.id.ok)

        // And activate Test again
        openMenu()
        clickOn(R.string.activate_all)

        navigateTo(AndroidTestHelper.MainMenu.OVERVIEW)

        clickListItemChild(R.id.latestReminders, 0, R.id.chipTaken)
        clickListItemChild(R.id.latestReminders, 1, R.id.chipTaken)

        clickOn(R.id.tag_filter)
        clickOn("Tag1")
        clickOn(R.id.ok)

        assertContains(R.id.nextReminderText, "Amount1")
        assertNotContains(R.id.nextReminderText, "Amount2")
        assertContains(R.id.reminderEventText, "Amount1")
        assertNotContains(R.id.reminderEventText, "Amount2")

        clickOn(R.id.tag_filter)
        clickOn("Tag1")
        clickOn("Tag2")
        clickOn(R.id.ok)

        assertNotContains(R.id.nextReminderText, "Amount1")
        assertContains(R.id.nextReminderText, "Amount2")
        assertNotContains(R.id.reminderEventText, "Amount1")
        assertContains(R.id.reminderEventText, "Amount2")
    }

    private fun addTag(tagName: String) {
        clickOn(R.id.addTag)
        writeTo(android.R.id.input, tagName)
        clickDialogPositiveButton()
    }
}