package com.futsch1.medtimer.utilities

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.NoActivityResumedException
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.adevinta.android.barista.interaction.BaristaDialogInteractions

private const val DIALOG_TIMEOUT = 1_000L

fun clickDialogPositiveButton(retryIfStillVisible: Boolean = true) {
    pollUntil(DIALOG_TIMEOUT) { positiveButtonShown() }
    clickDialogPositiveButtonIfVisible()
    if (retryIfStillVisible) {
        clickDialogPositiveButtonIfVisible()
    }
}

/**
 * A probe rather than an assertion: callers dismiss dialogs that may already be gone, and Espresso's
 * own matching reports absence by throwing.
 */
private fun positiveButtonShown(): Boolean {
    var shown = false
    try {
        onView(ViewMatchers.withId(android.R.id.button1)).check { view, _ -> shown = view?.isShown == true }
    } catch (_: NoActivityResumedException) {
        // ignore
    }
    return shown
}

private fun clickDialogPositiveButtonIfVisible() {
    if (positiveButtonShown()) {
        BaristaDialogInteractions.clickDialogPositiveButton()
    }
}

fun openNotification(): AutoCloseable {
    val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    device.openNotification()
    return AutoCloseable {
        device.closeNotification()
        device.waitForIdle(500)
    }
}
