package com.futsch1.medtimer.robots

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import kotlin.test.assertTrue

/** The system share sheet, which is another app's window and so out of Espresso's reach. */
class ShareSheetRobot {

    fun assertShownAndDismiss() {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val appPackage = InstrumentationRegistry.getInstrumentation().targetContext.packageName
        assertTrue(device.wait(Until.gone(By.pkg(appPackage).depth(0)), TIMEOUT), "Share sheet did not open")
        device.pressBack()
        device.wait(Until.hasObject(By.pkg(appPackage).depth(0)), TIMEOUT)
    }

    private companion object {
        const val TIMEOUT = 15_000L
    }
}
