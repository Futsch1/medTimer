package com.futsch1.medtimer.utilities

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import com.adevinta.android.barista.interaction.BaristaDialogInteractions


fun clickDialogPositiveButton() {
    val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    device.waitForView(By.res("android:id/button1"), 1_000)
    BaristaDialogInteractions.clickDialogPositiveButton()
}

fun openNotification(): AutoCloseable {
    val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    device.openNotification()
    return AutoCloseable { device.closeNotification() }
}