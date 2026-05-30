package com.futsch1.medtimer

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.adevinta.android.barista.assertion.BaristaListAssertions.assertDisplayedAtPosition
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import com.adevinta.android.barista.interaction.BaristaEditTextInteractions.writeTo
import com.adevinta.android.barista.interaction.BaristaListInteractions.clickListItem
import com.adevinta.android.barista.interaction.BaristaMenuClickInteractions.openMenu
import com.adevinta.android.barista.rule.flaky.AllowFlaky
import com.futsch1.medtimer.RecyclerViewDragAction.drag
import com.futsch1.medtimer.core.ui.R
import org.junit.Test

const val TEST_MED_1 = "Test"
const val TEST_MED_2 = "Test2"
const val TEST_MED_3 = "A test"

class MedicineHandlingTest : BaseTestHelper() {

    @Test
    @AllowFlaky(attempts = 3)
    fun medicineMoveTest() {
        AndroidTestHelper.createMedicine(TEST_MED_1)
        AndroidTestHelper.createMedicine(TEST_MED_2)

        pressBack()

        assertDisplayedAtPosition(
            com.futsch1.medtimer.feature.ui.R.id.medicineList,
            0,
            com.futsch1.medtimer.feature.ui.R.id.medicineName,
            TEST_MED_1
        )

        onView(withId(com.futsch1.medtimer.feature.ui.R.id.medicineList)).perform(drag(0, 1))
        assertDisplayedAtPosition(
            com.futsch1.medtimer.feature.ui.R.id.medicineList,
            0,
            com.futsch1.medtimer.feature.ui.R.id.medicineName,
            TEST_MED_2
        )
        clickListItem(com.futsch1.medtimer.feature.ui.R.id.medicineList, 0)
        writeTo(com.futsch1.medtimer.feature.ui.R.id.editMedicineName, TEST_MED_2 + "_")
        pressBack()
        assertDisplayedAtPosition(
            com.futsch1.medtimer.feature.ui.R.id.medicineList,
            0,
            com.futsch1.medtimer.feature.ui.R.id.medicineName,
            TEST_MED_2 + '_'
        )

        onView(withId(com.futsch1.medtimer.feature.ui.R.id.medicineList)).perform(drag(1, 0))
        assertDisplayedAtPosition(
            com.futsch1.medtimer.feature.ui.R.id.medicineList,
            0,
            com.futsch1.medtimer.feature.ui.R.id.medicineName,
            TEST_MED_1
        )
        onView(withId(com.futsch1.medtimer.feature.ui.R.id.medicineList)).perform(drag(0, 1))

        AndroidTestHelper.createMedicine(TEST_MED_3)
        pressBack()

        assertDisplayedAtPosition(
            com.futsch1.medtimer.feature.ui.R.id.medicineList,
            2,
            com.futsch1.medtimer.feature.ui.R.id.medicineName,
            TEST_MED_3
        )

        openMenu()
        clickOn(R.string.sort)
        clickOn(R.string.by_name)
        assertDisplayedAtPosition(
            com.futsch1.medtimer.feature.ui.R.id.medicineList,
            0,
            com.futsch1.medtimer.feature.ui.R.id.medicineName,
            TEST_MED_3
        )
        assertDisplayedAtPosition(
            com.futsch1.medtimer.feature.ui.R.id.medicineList,
            1,
            com.futsch1.medtimer.feature.ui.R.id.medicineName,
            TEST_MED_1
        )
        assertDisplayedAtPosition(
            com.futsch1.medtimer.feature.ui.R.id.medicineList,
            2,
            com.futsch1.medtimer.feature.ui.R.id.medicineName,
            TEST_MED_2 + '_'
        )
    }
}