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
import com.adevinta.android.barista.internal.viewaction.ChipViewActions.removeChip
import com.futsch1.medtimer.AndroidTestHelper.createMedicine
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

    private fun addTag(tagName: String) {
        clickOn(R.id.addTag)
        writeTo(android.R.id.input, tagName)
        clickDialogPositiveButton()
    }
}