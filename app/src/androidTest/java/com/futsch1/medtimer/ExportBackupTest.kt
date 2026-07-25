package com.futsch1.medtimer

import androidx.test.espresso.Espresso.pressBack
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import com.adevinta.android.barista.interaction.BaristaDialogInteractions.clickDialogPositiveButton
import com.adevinta.android.barista.interaction.BaristaListInteractions.clickListItem
import com.adevinta.android.barista.rule.flaky.AllowFlaky
import org.junit.Test
import com.futsch1.medtimer.feature.ui.AppOptionsTestTags

class ExportBackupTest : BaseTestHelper() {
    @Test
    @AllowFlaky(attempts = 3)
    fun testTriggerExport() {
        openAppOptionsMenu()
        clickTag(AppOptionsTestTags.GENERATE_TEST_DATA)

        AndroidTestHelper.navigateTo(AndroidTestHelper.MainMenu.MEDICINES)
        clickTag(AppOptionsTestTags.TAG_FILTER)
        clickOn("Supplements")
        pressBack()

        openAppOptionsMenu()
        clickTag(AppOptionsTestTags.EXPORT_EVENTS_CSV)
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        device.wait(Until.findObject(By.textContains("Sharing")), 15_000)
        device.pressBack()

        openAppOptionsMenu()
        clickTag(AppOptionsTestTags.EXPORT_EVENTS_PDF)
        device.wait(Until.findObject(By.textContains("Sharing")), 15_000)
        device.pressBack()

        openAppOptionsMenu()
        clickTag(AppOptionsTestTags.EXPORT_MEDICINES_CSV)
        device.wait(Until.findObject(By.textContains("Sharing")), 15_000)
        device.pressBack()

        openAppOptionsMenu()
        clickTag(AppOptionsTestTags.EXPORT_MEDICINES_PDF)
        device.wait(Until.findObject(By.textContains("Sharing")), 15_000)
        device.pressBack()
    }

    @Test
    @AllowFlaky(attempts = 3)
    fun testTriggerBackup() {
        openAppOptionsMenu()
        clickTag(AppOptionsTestTags.GENERATE_TEST_DATA)

        openAppOptionsMenu()
        clickTag(AppOptionsTestTags.BACKUP_CREATE)

        clickListItem(-1, 2)
        clickDialogPositiveButton()

        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        device.wait(Until.findObject(By.textContains("Sharing")), 5_000)
        device.pressBack()
    }
}