package com.futsch1.medtimer

import androidx.test.espresso.Espresso.pressBack
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import com.adevinta.android.barista.interaction.BaristaEditTextInteractions.writeTo
import com.adevinta.android.barista.interaction.BaristaMenuClickInteractions.openMenu
import com.adevinta.android.barista.rule.flaky.AllowFlaky
import com.futsch1.medtimer.AndroidTestHelper.assertMedicineAtPosition
import com.futsch1.medtimer.AndroidTestHelper.clickMedicineItem
import com.futsch1.medtimer.AndroidTestHelper.dragMedicineItem
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

        assertMedicineAtPosition(0, TEST_MED_1)

        dragMedicineItem(0, 1)
        assertMedicineAtPosition(0, TEST_MED_2)
        clickMedicineItem(0)
        writeTo(com.futsch1.medtimer.feature.ui.impl.R.id.editMedicineName, TEST_MED_2 + "_")
        pressBack()
        assertMedicineAtPosition(0, TEST_MED_2 + '_')

        dragMedicineItem(1, 0)
        assertMedicineAtPosition(0, TEST_MED_1)
        dragMedicineItem(0, 1)

        AndroidTestHelper.createMedicine(TEST_MED_3)
        pressBack()

        assertMedicineAtPosition(2, TEST_MED_3)

        openMenu()
        clickOn(R.string.sort)
        clickOn(R.string.by_name)
        assertMedicineAtPosition(0, TEST_MED_3)
        assertMedicineAtPosition(1, TEST_MED_1)
        assertMedicineAtPosition(2, TEST_MED_2 + '_')
    }
}
