package com.futsch1.medtimer

import androidx.test.espresso.Espresso.pressBack
import com.adevinta.android.barista.rule.flaky.AllowFlaky
import com.futsch1.medtimer.core.ui.R
import org.junit.Test

const val TEST_MED_1 = "Test"
const val TEST_MED_2 = "Test2"
const val TEST_MED_3 = "A test"

class MedicineHandlingTest : MedTimerTestBase() {

    @Test
    @AllowFlaky(attempts = 3)
    fun medicineMoveTest() {
        medicines.create(TEST_MED_1)
        medicines.create(TEST_MED_2)

        pressBack()

        medicines.assertCount(2)
        medicines.assertAtPosition(0, TEST_MED_1)
        medicines.assertAtPosition(1, TEST_MED_2)

        medicines.dragItem(0, 1)
        medicines.assertAtPosition(0, TEST_MED_2)
        medicines.clickItem(0)
        medicineEditor.rename(TEST_MED_2 + "_")
        pressBack()
        medicines.assertAtPosition(0, TEST_MED_2 + '_')

        medicines.dragItem(1, 0)
        medicines.assertAtPosition(0, TEST_MED_1)
        medicines.dragItem(0, 1)

        medicines.create(TEST_MED_3)
        pressBack()

        medicines.assertCount(3)
        medicines.assertAtPosition(2, TEST_MED_3)

        openMedicinesMenu()
        clickMenuItem(R.string.by_name)
        medicines.assertAtPosition(0, TEST_MED_3)
        medicines.assertAtPosition(1, TEST_MED_1)
        medicines.assertAtPosition(2, TEST_MED_2 + '_')
    }
}
