package com.futsch1.medtimer

import androidx.test.espresso.Espresso.pressBack
import com.adevinta.android.barista.rule.flaky.AllowFlaky
import com.futsch1.medtimer.core.ui.R
import com.futsch1.medtimer.feature.ui.AppOptionsTestTags
import com.futsch1.medtimer.utilities.clickDialogPositiveButton
import org.junit.Test

class ExportBackupTest : MedTimerTestBase() {
    @Test
    @AllowFlaky(attempts = 3)
    fun testTriggerExport() {
        openAppOptionsMenu()
        clickTag(AppOptionsTestTags.GENERATE_TEST_DATA)

        navigation.toMedicines()
        clickTag(AppOptionsTestTags.TAG_FILTER)
        clickTagChip("Supplements")
        pressBack()

        exportViaAppOptions(AppOptionsTestTags.EXPORT_EVENTS_CSV)
        exportViaAppOptions(AppOptionsTestTags.EXPORT_EVENTS_PDF)
        exportViaAppOptions(AppOptionsTestTags.EXPORT_MEDICINES_CSV)
        exportViaAppOptions(AppOptionsTestTags.EXPORT_MEDICINES_PDF)
    }

    @Test
    @AllowFlaky(attempts = 3)
    fun testTriggerBackup() {
        openAppOptionsMenu()
        clickTag(AppOptionsTestTags.GENERATE_TEST_DATA)

        openAppOptionsMenu()
        clickTag(AppOptionsTestTags.BACKUP_CREATE)

        assertDialogItemChecked(R.string.medicine_data)
        assertDialogItemChecked(R.string.event_data)
        clickDialogPositiveButton()

        assertShareSheetShown()
    }
}
