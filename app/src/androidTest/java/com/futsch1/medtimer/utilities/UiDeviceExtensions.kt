package com.futsch1.medtimer.utilities

import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until

fun UiDevice.waitForView(selector: BySelector, timeoutMs: Long) {
    wait(Until.hasObject(selector), timeoutMs)
}

fun UiDevice.closeNotification() {
    swipe(displayWidth / 2, displayHeight, displayWidth / 2, displayHeight / 2, 20)
    waitForIdle(200)
    if (!findObjects(By.res("android:id/expand_button")).isEmpty() || !findObjects(By.descContains("Expand")).isEmpty()) {
        pressBack()
    }
}