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
import com.adevinta.android.barista.interaction.BaristaDialogInteractions
import com.adevinta.android.barista.interaction.BaristaEditTextInteractions.writeTo
import com.adevinta.android.barista.internal.viewaction.ChipViewActions.removeChip
import com.adevinta.android.barista.rule.flaky.AllowFlaky
import com.futsch1.medtimer.AndroidTestHelper.createMedicine
import org.junit.Test

class TagTest : BaseTestHelper() {
    @Test
    @AllowFlaky(attempts = 1)
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
        clickOn(R.id.ok)

        clickOn(R.id.openTags)
        assertNotContains("Another tag")
    }

    private fun addTag(tagName: String) {
        clickOn(R.id.addTag)
        writeTo(android.R.id.input, tagName)
        BaristaDialogInteractions.clickDialogPositiveButton()
    }
}