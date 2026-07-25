package com.futsch1.medtimer

import androidx.test.espresso.Espresso
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import com.adevinta.android.barista.interaction.BaristaListInteractions.clickListItemChild
import com.adevinta.android.barista.rule.flaky.AllowFlaky
import com.futsch1.medtimer.AndroidTestHelper.navigateTo
import com.futsch1.medtimer.core.ui.R
import com.futsch1.medtimer.feature.ui.statistics.ANALYSIS_RANGES
import com.futsch1.medtimer.utilities.openNotification
import org.junit.ClassRule
import org.junit.Test
import tools.fastlane.screengrab.Screengrab
import tools.fastlane.screengrab.UiAutomatorScreenshotStrategy
import tools.fastlane.screengrab.locale.LocaleTestRule
import com.futsch1.medtimer.feature.ui.AppOptionsTestTags


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

        openAppOptionsMenu()
        clickTag(AppOptionsTestTags.GENERATE_TEST_DATA)

        clickOverviewEvent(0)
        internalAssert(device.findObject(By.textContains("Some note")) != null)
        Espresso.pressBack()

        clickOverviewEventState(0)
        clickMenuItem(com.futsch1.medtimer.core.ui.R.string.taken)
        clickOverviewEventState(2)
        clickMenuItem(com.futsch1.medtimer.core.ui.R.string.taken)
        clickOverviewEventState(3)
        clickMenuItem(com.futsch1.medtimer.core.ui.R.string.skipped)

        openNotification().use {
            makeNotificationExpanded(device, getNotificationText(R.string.taken))
            Screengrab.screenshot("5")
        }

        clickOverviewEventState(4)
        clickMenuItem(com.futsch1.medtimer.core.ui.R.string.taken)

        Screengrab.screenshot("1")

        navigateTo(AndroidTestHelper.MainMenu.MEDICINES)
        Screengrab.screenshot("2")

        AndroidTestHelper.clickMedicineItem(0)
        Screengrab.screenshot("3")

        clickListItemChild(com.futsch1.medtimer.feature.ui.R.id.reminderList, 0, com.futsch1.medtimer.feature.ui.R.id.openAdvancedSettings)
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
