package com.futsch1.medtimer

import androidx.test.espresso.Espresso.pressBack
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import com.adevinta.android.barista.interaction.BaristaDialogInteractions.clickDialogPositiveButton
import com.adevinta.android.barista.interaction.BaristaListInteractions.clickListItem
import com.adevinta.android.barista.interaction.BaristaMenuClickInteractions.openMenu
import com.adevinta.android.barista.rule.flaky.AllowFlaky
import org.junit.Test

class ExportBackupTest : BaseTestHelper() {
    @Test
    @AllowFlaky(attempts = 1)
    fun testTriggerExport() {
        openMenu()
        clickOn(R.string.generate_test_data)

        AndroidTestHelper.navigateTo(AndroidTestHelper.MainMenu.MEDICINES)
        clickOn(R.id.tag_filter)
        clickOn("Supplements")
        pressBack()

        openMenu()
        clickOn(R.string.event_data)
        clickOn(R.string.export_csv)
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        device.wait(Until.findObject(By.textContains("Sharing")), 15_000)
        device.pressBack()

        openMenu()
        clickOn(R.string.event_data)
        clickOn(R.string.export_pdf)
        device.wait(Until.findObject(By.textContains("Sharing")), 15_000)
        device.pressBack()

        openMenu()
        clickOn(R.string.medicine_data)
        clickOn(R.string.export_csv)
        device.wait(Until.findObject(By.textContains("Sharing")), 15_000)
        device.pressBack()

        openMenu()
        clickOn(R.string.medicine_data)
        clickOn(R.string.export_pdf)
        device.wait(Until.findObject(By.textContains("Sharing")), 15_000)
        device.pressBack()
    }

    @Test
    fun testTriggerBackup() {
        openMenu()
        clickOn(R.string.generate_test_data)

        openMenu()
        clickOn(R.string.backup)

        clickListItem(-1, 1)
        clickDialogPositiveButton()

        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        device.wait(Until.findObject(By.textContains("Sharing")), 5_000)
        device.pressBack()
    }
}