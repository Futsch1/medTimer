package com.futsch1.medtimer

import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import com.adevinta.android.barista.rule.flaky.AllowFlaky
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Test

@HiltAndroidTest
class SetupGuidanceTest : MedTimerTestBase() {
    @Test
    @AllowFlaky(attempts = 3)
    fun setupGuidanceFollowsSavedMedicineAndReminderState() {
        medicines.assertAddMedicineGuidanceDisplayed()
        medicines.createFromGuidance("Medicine A")

        medicineEditor.assertReminderSetupGuidanceDisplayed()
        medicines.showList()
        medicines.assertAddMedicineGuidanceAbsent()
        medicines.clickNamed("Medicine A")
        medicineEditor.assertReminderSetupGuidanceDisplayed()

        medicineEditor.openGettingStartedFromGuidance()
        help.assertGettingStartedDisplayed()
        help.pressBack()
        medicineEditor.assertReminderSetupGuidanceDisplayed()

        medicineEditor.openAddReminderFromGuidance()
        assertDisplayed(com.futsch1.medtimer.feature.ui.R.id.timeBasedCard)
        medicineEditor.closeReminderTypeChooser()
        medicineEditor.addReminder("1", laterToday())
        medicineEditor.assertReminderSetupGuidanceAbsent()

        medicines.showList()
        medicines.assertAddMedicineGuidanceAbsent()
    }
}
