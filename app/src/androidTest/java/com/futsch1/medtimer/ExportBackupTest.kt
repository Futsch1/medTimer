package com.futsch1.medtimer

import androidx.test.espresso.Espresso.pressBack
import com.adevinta.android.barista.rule.flaky.AllowFlaky
import com.futsch1.medtimer.core.ui.R
import com.futsch1.medtimer.utilities.clickDialogPositiveButton
import org.junit.Test

class ExportBackupTest : MedTimerTestBase() {
    @Test
    @AllowFlaky(attempts = 3)
    fun testTriggerExport() {
        menus.clickAppOption(R.string.generate_test_data)

        navigation.toMedicines()
        menus.openTagFilter()
        clickTagChip("Supplements")
        pressBack()

        exportViaAppOptions(R.string.export_events_csv)
        exportViaAppOptions(R.string.export_events_pdf)
        exportViaAppOptions(R.string.export_medicines_csv)
        exportViaAppOptions(R.string.export_medicines_pdf)
    }

    @Test
    @AllowFlaky(attempts = 3)
    fun testTriggerBackup() {
        menus.clickAppOption(R.string.generate_test_data)

        menus.clickAppOption(R.string.backup)

        assertDialogItemChecked(R.string.medicine_data)
        assertDialogItemChecked(R.string.event_data)
        clickDialogPositiveButton()

        assertShareSheetShown()
    }
}
