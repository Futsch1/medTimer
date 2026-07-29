package com.futsch1.medtimer

import com.adevinta.android.barista.rule.flaky.AllowFlaky
import com.futsch1.medtimer.core.ui.R
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Test

@HiltAndroidTest
class ExportBackupTest : MedTimerTestBase() {
    @Test
    @AllowFlaky(attempts = 3)
    fun testTriggerExport() {
        menus.clickAppOption(R.string.generate_test_data)

        navigation.toMedicines()
        tags.inFilter(confirming = false) { toggle("Supplements") }

        export.export(R.string.export_events_csv)
        export.export(R.string.export_events_pdf)
        export.export(R.string.export_medicines_csv)
        export.export(R.string.export_medicines_pdf)
    }

    @Test
    @AllowFlaky(attempts = 3)
    fun testTriggerBackup() {
        menus.clickAppOption(R.string.generate_test_data)

        menus.clickAppOption(R.string.backup)

        dialogs.assertItemChecked(R.string.medicine_data)
        dialogs.assertItemChecked(R.string.event_data)
        dialogs.confirm()

        shareSheet.assertShownAndDismiss()
    }
}
