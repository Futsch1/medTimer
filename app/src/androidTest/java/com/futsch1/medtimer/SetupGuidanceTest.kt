package com.futsch1.medtimer

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
        help.assertHelpDisplayed()
        help.pressBack()
        medicineEditor.assertReminderSetupGuidanceDisplayed()

        medicineEditor.openAddReminderFromGuidance()
        medicineEditor.assertReminderTypeDisplayed(com.futsch1.medtimer.core.ui.R.string.time_based_reminder)
        medicineEditor.openReminderTypesHelp()
        help.assertHelpDisplayed()
        help.pressBack()
        medicineEditor.assertReminderTypeDisplayed(com.futsch1.medtimer.core.ui.R.string.time_based_reminder)
        medicineEditor.closeReminderTypeChooser()
        medicineEditor.addReminder("1", laterToday())
        medicineEditor.assertReminderSetupGuidanceAbsent()

        medicines.showList()
        medicines.assertAddMedicineGuidanceAbsent()
    }
}
