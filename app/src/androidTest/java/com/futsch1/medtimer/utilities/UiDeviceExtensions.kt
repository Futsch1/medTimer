package com.futsch1.medtimer.utilities

import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until

const val TAG = "UiDeviceExtensions"

fun UiDevice.grantAppPermission(permission: String) {
    val packageName = InstrumentationRegistry.getInstrumentation().targetContext.packageName
    executeShellCommand("appops set $packageName $permission allow")
}

fun UiDevice.closeNotification() {
    Log.d(TAG, "Collapsing notification shade")
    executeShellCommand("cmd statusbar collapse")
    if (waitForAppInForeground(this)) {
        return
    }

    Log.d(TAG, "App is not in foreground after collapse, pressing back to dismiss")
    pressBack()
    waitForAppInForeground(this)
}

private fun waitForAppInForeground(device: UiDevice): Boolean {
    val packageName = InstrumentationRegistry.getInstrumentation().targetContext.packageName
    return device.wait(Until.hasObject(By.pkg(packageName).depth(0)), 2_000)
}