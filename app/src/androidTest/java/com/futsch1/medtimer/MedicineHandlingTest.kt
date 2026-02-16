package com.futsch1.medtimer

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.adevinta.android.barista.assertion.BaristaListAssertions.assertDisplayedAtPosition
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import com.adevinta.android.barista.interaction.BaristaMenuClickInteractions.openMenu
import com.futsch1.medtimer.RecyclerViewDragAction.drag
import org.junit.Test

const val TEST_MED_1 = "Test"
const val TEST_MED_2 = "Test2"
const val TEST_MED_3 = "A test"

class MedicineHandlingTest : BaseTestHelper() {


    @Test
    //@AllowFlaky(attempts = 1)
    fun medicineMoveTest() {
        AndroidTestHelper.createMedicine(TEST_MED_1)
        AndroidTestHelper.createMedicine(TEST_MED_2)

        pressBack()

        assertDisplayedAtPosition(R.id.medicineList, 0, R.id.medicineName, TEST_MED_1)

        onView(withId(R.id.medicineList)).perform(drag(0, 1))
        assertDisplayedAtPosition(R.id.medicineList, 0, R.id.medicineName, TEST_MED_2)

        onView(withId(R.id.medicineList)).perform(drag(1, 0))
        assertDisplayedAtPosition(R.id.medicineList, 0, R.id.medicineName, TEST_MED_1)

        AndroidTestHelper.createMedicine(TEST_MED_3)
        pressBack()

        assertDisplayedAtPosition(R.id.medicineList, 2, R.id.medicineName, TEST_MED_3)

        openMenu()
        clickOn(R.string.sort)
        clickOn(R.string.by_name)
        assertDisplayedAtPosition(R.id.medicineList, 0, R.id.medicineName, TEST_MED_3)
        assertDisplayedAtPosition(R.id.medicineList, 1, R.id.medicineName, TEST_MED_1)
        assertDisplayedAtPosition(R.id.medicineList, 2, R.id.medicineName, TEST_MED_2)
    }
}