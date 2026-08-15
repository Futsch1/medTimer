package com.futsch1.medtimer

import com.adevinta.android.barista.rule.flaky.AllowFlaky
import com.futsch1.medtimer.core.ui.R
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Test
import kotlin.time.Duration.Companion.hours

private const val NEW_TAG = "New tag"

private const val ANOTHER_TAG = "Another tag"

@HiltAndroidTest
class TagTest : MedTimerTestBase() {
    @Test
    @AllowFlaky(attempts = 3)
    fun tagHandling() {
        seed.medicine("Test")
        medicines.clickItem(0)

        tags.inMedicineTags {
            add(NEW_TAG)
            assertDisplayed(NEW_TAG)
            assertChecked(NEW_TAG)
        }

        tags.inMedicineTags {
            assertDisplayed(NEW_TAG)
            assertChecked(NEW_TAG)

            add(ANOTHER_TAG)
            assertDisplayed(ANOTHER_TAG)
            assertChecked(ANOTHER_TAG)

            toggle(ANOTHER_TAG)
            assertNotChecked(ANOTHER_TAG)
        }

        medicines.assertListContains(NEW_TAG)
        medicines.assertListDoesNotContain(ANOTHER_TAG)

        seed.medicine("Test 2")
        medicines.clickItem(1)

        tags.inMedicineTags {
            assertNotChecked(NEW_TAG)
            assertNotChecked(ANOTHER_TAG)
            remove(1)
        }

        tags.inMedicineTags {
            assertDoesNotExist(ANOTHER_TAG)
            toggle(NEW_TAG)
        }

        menus.clickEditMedicineOption(R.string.duplicate)
        medicines.clickItem(2)
        tags.inMedicineTags { assertChecked(NEW_TAG) }
    }

    @Test
    @AllowFlaky(attempts = 3)
    fun medicineVisibility() {
        seed.medicine("Test")
        medicines.clickItem(0)
        tags.inMedicineTags { add("Tag1") }

        seed.medicine("Else")
        medicines.clickItem(1)
        tags.inMedicineTags { add("Tag2") }

        medicines.assertNameContains("Test")
        medicines.assertNameContains("Else")

        tags.inFilter {
            toggle("Tag1")
            assertChecked("Tag1")
            assertNotChecked("Tag2")
        }

        medicines.assertNameContains("Test")
        medicines.assertNameNotContains("Else")

        tags.inFilter {
            toggle("Tag1")
            toggle("Tag2")
            assertNotChecked("Tag1")
            assertChecked("Tag2")
        }

        medicines.assertNameNotContains("Test")
        medicines.assertNameContains("Else")
    }

    @Test
    @AllowFlaky(attempts = 3)
    fun activateAndOverviewVisibility() {
        seed.medicine("Test") { intervalReminder("Amount1", 1.hours) }
        medicines.clickItem(0)
        tags.inMedicineTags { add("Tag1") }

        seed.medicine("Else") { intervalReminder("Amount2", 1.hours) }
        medicines.clickItem(1)
        tags.inMedicineTags { add("Tag2") }

        // First, deactivate all of Test
        medicines.showList()
        tags.inFilter { toggle("Tag1") }

        menus.clickMedicinesOption(R.string.deactivate_all)
        medicines.assertListContains(R.string.inactive)

        // Now, check that Else is not deactivated
        tags.inFilter {
            toggle("Tag1")
            toggle("Tag2")
        }

        medicines.assertListDoesNotContain(R.string.inactive)

        tags.inFilter { toggle("Tag2") }

        // And activate Test again
        menus.clickMedicinesOption(R.string.activate_all)

        navigation.toOverview()

        overview.clickEventState(0)
        overview.clickAction(R.string.taken)
        overview.clickEventState(1)
        overview.clickAction(R.string.taken)

        tags.inFilter { toggle("Tag1") }

        overview.assertEventContains("Amount1")
        overview.assertNoEventContains("Amount2")

        tags.inFilter {
            toggle("Tag1")
            toggle("Tag2")
        }

        overview.assertNoEventContains("Amount1")
        overview.assertEventContains("Amount2")
    }
}
