package com.futsch1.medtimer

import com.adevinta.android.barista.assertion.BaristaListAssertions.assertListItemCount
import com.adevinta.android.barista.interaction.BaristaListInteractions.clickListItemChild
import com.adevinta.android.barista.rule.flaky.AllowFlaky
import com.futsch1.medtimer.core.ui.R
import com.futsch1.medtimer.utilities.clickDialogPositiveButton
import org.junit.Test


class DeleteTest : MedTimerTestBase() {
    @Test
    @AllowFlaky(attempts = 3)
    fun testDelete() {
        menus.clickAppOption(R.string.generate_test_data)

        navigation.toMedicines()
        medicines.assertCount(4)

        medicines.clickNamed(OMEGA_3)
        menus.clickEditMedicineOption(R.string.delete)
        clickDialogPositiveButton()

        medicines.assertCount(3)
        medicines.assertNameNotContains(OMEGA_3)

        medicines.clickNamed(SELEN)
        assertListItemCount(com.futsch1.medtimer.feature.ui.R.id.reminderList, 2)

        clickListItemChild(
            com.futsch1.medtimer.feature.ui.R.id.reminderList,
            1,
            com.futsch1.medtimer.feature.ui.R.id.openAdvancedSettings
        )
        medicineEditor.deleteReminder()
        clickDialogPositiveButton()

        assertListItemCount(com.futsch1.medtimer.feature.ui.R.id.reminderList, 1)
    }

    companion object {
        private const val OMEGA_3 = "Omega 3 (EPA/DHA 500mg)"
        private const val SELEN = "Selen (200 µg)"
    }
}
