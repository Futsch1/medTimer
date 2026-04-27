package com.futsch1.medtimer.utilities

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import com.adevinta.android.barista.interaction.BaristaDialogInteractions


fun clickDialogPositiveButton(retryIfStillVisible: Boolean = true) {
    val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    device.waitForView(By.res("android:id/button1"), 1_000)
    clickDialogPositiveButtonIfVisible(device)
    device.waitForIdle()
    if (retryIfStillVisible) {
        clickDialogPositiveButtonIfVisible(device)
    }
}

private fun clickDialogPositiveButtonIfVisible(device: UiDevice) {
    device.waitForIdle()
    if (null != device.findObject(By.res("android:id/button1"))) {
        BaristaDialogInteractions.clickDialogPositiveButton()
    }
}

fun openNotification(): AutoCloseable {
    val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    device.openNotification()
    return AutoCloseable { device.closeNotification() }
}