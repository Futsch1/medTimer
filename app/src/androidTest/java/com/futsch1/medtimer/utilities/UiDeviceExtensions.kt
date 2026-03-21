package com.futsch1.medtimer.utilities

import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until

const val TAG = "UiDeviceExtensions"

fun UiDevice.waitForView(selector: BySelector, timeoutMs: Long) {
    wait(Until.hasObject(selector), timeoutMs)
}

fun UiDevice.closeNotification() {
    if (waitForAppInForeground(this)) {
        Log.d(TAG, "App is in foreground, skipping notification close")
        return
    }

    // Swipe to close the notification shade
    Log.d(TAG, "Swipe to close notification shade")
    swipe(displayWidth / 2, displayHeight, displayWidth / 2, displayHeight / 2, 20)
    if (waitForAppInForeground(this)) {
        return
    }

    Log.d(TAG, "App is not in foreground after swipe, pressing back to dismiss")
    pressBack()
    waitForAppInForeground(this)
}

private fun waitForAppInForeground(device: UiDevice): Boolean {
    val packageName = InstrumentationRegistry.getInstrumentation().targetContext.packageName
    return device.wait(Until.hasObject(By.pkg(packageName).depth(0)), 2_000)
}