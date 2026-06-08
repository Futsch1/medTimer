package com.futsch1.medtimer

import androidx.test.espresso.Espresso
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import com.adevinta.android.barista.interaction.BaristaListInteractions.clickListItemChild
import com.adevinta.android.barista.interaction.BaristaMenuClickInteractions.openMenu
import com.adevinta.android.barista.rule.flaky.AllowFlaky
import com.futsch1.medtimer.AndroidTestHelper.navigateTo
import com.futsch1.medtimer.core.ui.R
import com.futsch1.medtimer.feature.ui.impl.statistics.ANALYSIS_RANGES
import com.futsch1.medtimer.utilities.openNotification
import org.junit.ClassRule
import org.junit.Test
import tools.fastlane.screengrab.Screengrab
import tools.fastlane.screengrab.UiAutomatorScreenshotStrategy
import tools.fastlane.screengrab.locale.LocaleTestRule


class ScreenshotsTest : BaseTestHelper() {
    companion object {
        // JvmField is needed for the @ClassRule to work
        @JvmField
        @ClassRule
        val localeTestRule: LocaleTestRule = LocaleTestRule()
    }

    @Test
    @AllowFlaky(attempts = 3)
    fun screenshotsTest() {
        Screengrab.setDefaultScreenshotStrategy(UiAutomatorScreenshotStrategy())
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        openMenu()
        clickOn(R.string.generate_test_data)

        clickListItemChild(com.futsch1.medtimer.feature.ui.impl.R.id.reminders, 0, com.futsch1.medtimer.feature.ui.impl.R.id.overviewContentContainer)
        internalAssert(device.findObject(By.textContains("Some note")) != null)
        Espresso.pressBack()

        clickListItemChild(com.futsch1.medtimer.feature.ui.impl.R.id.reminders, 0, com.futsch1.medtimer.feature.ui.impl.R.id.stateButton)
        clickOn(com.futsch1.medtimer.feature.ui.impl.R.id.takenButton)
        clickListItemChild(com.futsch1.medtimer.feature.ui.impl.R.id.reminders, 2, com.futsch1.medtimer.feature.ui.impl.R.id.stateButton)
        clickOn(com.futsch1.medtimer.feature.ui.impl.R.id.takenButton)
        clickListItemChild(com.futsch1.medtimer.feature.ui.impl.R.id.reminders, 3, com.futsch1.medtimer.feature.ui.impl.R.id.stateButton)
        clickOn(com.futsch1.medtimer.feature.ui.impl.R.id.skippedButton)

        openNotification().use {
            makeNotificationExpanded(device, getNotificationText(R.string.taken))
            Screengrab.screenshot("5")
        }

        clickListItemChild(com.futsch1.medtimer.feature.ui.impl.R.id.reminders, 4, com.futsch1.medtimer.feature.ui.impl.R.id.stateButton)
        clickOn(com.futsch1.medtimer.feature.ui.impl.R.id.takenButton)

        Screengrab.screenshot("1")

        navigateTo(AndroidTestHelper.MainMenu.MEDICINES)
        Screengrab.screenshot("2")

        AndroidTestHelper.clickMedicineItem(0)
        Screengrab.screenshot("3")

        clickListItemChild(com.futsch1.medtimer.feature.ui.impl.R.id.reminderList, 0, com.futsch1.medtimer.feature.ui.impl.R.id.openAdvancedSettings)
        AndroidTestHelper.waitForIdle(500)
        Screengrab.screenshot("4")

        navigateTo(AndroidTestHelper.MainMenu.ANALYSIS)
        // Default view is Charts; no chip click needed
        Screengrab.screenshot("6")

        // Open the range dropdown (button shows the currently-selected range) and select "2 days"
        val rangeLabels = ANALYSIS_RANGES.map { context.getString(it.first) }
        val rangeButton = rangeLabels.firstNotNullOfOrNull { device.findObject(By.text(it)) }
        rangeButton?.click()
        AndroidTestHelper.waitForIdle(300)
        device.findObject(By.text(rangeLabels[1]))?.click()
        AndroidTestHelper.waitForIdle(300)

        // Switch to Table view (view chips are icon-only; labels exposed as content descriptions)
        device.findObject(By.desc(context.getString(R.string.tabular_view)))?.click()
        AndroidTestHelper.waitForIdle(500)
        Screengrab.screenshot("7")

        // Sort by Name column
        device.findObject(By.text(context.getString(R.string.name)))?.click()
        AndroidTestHelper.waitForIdle(300)

        internalAssert(device.findObject(By.textContains("Selen")) != null)

        // Filter by "B" (the Compose text field surfaces as an EditText to UiAutomator)
        val filterField = device.findObject(By.clazz("android.widget.EditText"))
        filterField?.click()
        AndroidTestHelper.waitForIdle(200)
        filterField?.text = "B"
        AndroidTestHelper.waitForIdle(300)

        internalAssert(device.findObject(By.textContains("B12")) != null)

        // Clear the filter (trailing Cancel icon, described by R.string.cancel)
        device.findObject(By.desc(context.getString(R.string.cancel)))?.click()
        AndroidTestHelper.waitForIdle(300)

        // Switch to Calendar view
        device.findObject(By.desc(context.getString(R.string.calendar)))?.click()
        AndroidTestHelper.waitForIdle(500)
        Screengrab.screenshot("8")

        navigateTo(AndroidTestHelper.MainMenu.OVERVIEW)
    }
}
