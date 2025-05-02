package com.futsch1.medtimer

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.adevinta.android.barista.assertion.BaristaListAssertions.assertDisplayedAtPosition
import com.futsch1.medtimer.RecyclerViewDragAction.drag
import org.junit.Test

class MedicineHandlingTest : BaseTestHelper() {
    @Test
    //@AllowFlaky(attempts = 1)
    fun medicineMoveTest() {
        AndroidTestHelper.createMedicine("Test")
        AndroidTestHelper.createMedicine("Test2")

        pressBack()

        assertDisplayedAtPosition(R.id.medicineList, 0, R.id.medicineName, "Test")

        onView(withId(R.id.medicineList)).perform(drag(0, 1))
        assertDisplayedAtPosition(R.id.medicineList, 0, R.id.medicineName, "Test2")

        onView(withId(R.id.medicineList)).perform(drag(1, 0))
        assertDisplayedAtPosition(R.id.medicineList, 0, R.id.medicineName, "Test")

        AndroidTestHelper.createMedicine("Test3")
        pressBack()

        assertDisplayedAtPosition(R.id.medicineList, 2, R.id.medicineName, "Test3")
    }
}