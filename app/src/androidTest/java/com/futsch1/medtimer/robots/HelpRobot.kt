package com.futsch1.medtimer.robots

import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import androidx.test.platform.app.InstrumentationRegistry
import com.futsch1.medtimer.core.ui.R
import kotlin.test.assertNotNull

class HelpRobot(private val menus: MenuRobot) {
    private val device get() = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    fun open() = menus.clickAppOption(R.string.help)

    fun openGettingStarted() {
        findText("Getting started").click()
    }

    fun assertContentsDisplayed() {
        findText("This guide is currently available in English only.")
    }

    fun assertGettingStartedDisplayed() {
        findText("This guide is currently available in English only.")
        findText("MedTimer stores medicines and reminders separately.")
    }

    fun pressBack() {
        device.pressBack()
    }

    fun pressToolbarBack() {
        assertNotNull(
            device.wait(Until.findObject(By.desc("Back")), UI_TIMEOUT_MILLIS),
            "Missing help toolbar Back button"
        ).click()
    }

    private fun findText(text: String) =
        assertNotNull(device.wait(Until.findObject(By.textContains(text)), UI_TIMEOUT_MILLIS), "Missing help text: $text")

    companion object {
        private const val UI_TIMEOUT_MILLIS = 5_000L
    }
}
