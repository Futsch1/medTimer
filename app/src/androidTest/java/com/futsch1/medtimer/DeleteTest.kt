package com.futsch1.medtimer

import com.adevinta.android.barista.rule.flaky.AllowFlaky
import com.futsch1.medtimer.core.ui.R
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Test


@HiltAndroidTest
class DeleteTest : MedTimerTestBase() {
    @Test
    @AllowFlaky(attempts = 3)
    fun testDelete() {
        menus.clickAppOption(R.string.generate_test_data)

        navigation.toMedicines()
        medicines.assertCount(4)

        medicines.clickNamed(OMEGA_3)
        menus.clickEditMedicineOption(R.string.delete)
        dialogs.confirm()

        medicines.assertCount(3)
        medicines.assertNameNotContains(OMEGA_3)

        medicines.clickNamed(SELEN)
        reminders.assertCount(2)

        reminders.delete(1)

        reminders.assertCount(1)
    }

    companion object {
        private const val OMEGA_3 = "Omega 3 (EPA/DHA 500mg)"
        private const val SELEN = "Selen (200 µg)"
    }
}
