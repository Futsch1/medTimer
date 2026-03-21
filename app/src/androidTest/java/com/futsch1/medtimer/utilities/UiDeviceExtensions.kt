package com.futsch1.medtimer.utilities

import android.util.Log
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until

const val TAG = "UiDeviceExtensions"

fun UiDevice.waitForView(selector: BySelector, timeoutMs: Long) {
    wait(Until.hasObject(selector), timeoutMs)
}

fun UiDevice.closeNotification() {
    // Swipe to close the notification shade
    swipe(displayWidth / 2, displayHeight, displayWidth / 2, displayHeight / 2, 20)
    waitForIdle(2000)

    val isNotificationShadeOpen = findObjects(By.res("android:id/expand_button")).isNotEmpty() || findObjects(By.descContains("Expand")).isNotEmpty()
    if (isNotificationShadeOpen) {
        Log.w(TAG, "Expand button found after swipe, pressing back to dismiss")
        pressBack()
        waitForIdle(2000)
    }
}