package com.futsch1.medtimer

import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import com.adevinta.android.barista.interaction.BaristaListInteractions.clickListItemChild
import com.adevinta.android.barista.interaction.BaristaMenuClickInteractions.openMenu
import com.adevinta.android.barista.interaction.BaristaSleepInteractions.sleep
import com.futsch1.medtimer.core.ui.CoreUiTestTags
import com.futsch1.medtimer.statistics.ui.StatisticsTestTags
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.ClassRule
import org.junit.Test
import tools.fastlane.screengrab.Screengrab
import tools.fastlane.screengrab.UiAutomatorScreenshotStrategy
import tools.fastlane.screengrab.locale.LocaleTestRule

@HiltAndroidTest
class ScreenshotsTest : BaseTestHelper() {
    companion object {
        @ClassRule
        @JvmField
        val localeTestRule = LocaleTestRule()
    }

    @Test
    //@AllowFlaky(attempts = 1)
    fun screenshotsTest() {
        Screengrab.setDefaultScreenshotStrategy(UiAutomatorScreenshotStrategy())
        val device = UiDevice.getInstance(getInstrumentation())

        openMenu()
        clickOn(R.string.generate_test_data)

        clickListItemChild(R.id.reminders, 0, R.id.overviewContentContainer)
        internalAssert(device.findObject(By.textContains("Some note")) != null)
        pressBack()

        clickListItemChild(R.id.reminders, 0, R.id.stateButton)
        clickOn(R.id.takenButton)
        clickListItemChild(R.id.reminders, 2, R.id.stateButton)
        clickOn(R.id.takenButton)
        clickListItemChild(R.id.reminders, 3, R.id.stateButton)
        clickOn(R.id.skippedButton)

        device.openNotification()
        makeNotificationExpanded(device, getNotificationText(R.string.taken))
        Screengrab.screenshot("5")
        device.pressBack()

        clickListItemChild(R.id.reminders, 4, R.id.stateButton)
        clickOn(R.id.takenButton)

        Screengrab.screenshot("1")

        AndroidTestHelper.navigateTo(AndroidTestHelper.MainMenu.MEDICINES)
        Screengrab.screenshot("2")

        onView(withId(R.id.medicineList)).perform(
            RecyclerViewActions.actionOnItemAtPosition<androidx.recyclerview.widget.RecyclerView.ViewHolder>(
                0,
                click()
            )
        )
        Screengrab.screenshot("3")

        clickListItemChild(R.id.reminderList, 0, R.id.openAdvancedSettings)
        sleep(500)
        Screengrab.screenshot("4")

        AndroidTestHelper.navigateTo(AndroidTestHelper.MainMenu.ANALYSIS)
        composeTestRule.onNodeWithTag(StatisticsTestTags.CHART_CHIP).performClick()
        Screengrab.screenshot("6")

        composeTestRule.onNodeWithTag(StatisticsTestTags.DAYS_DROPDOWN).performClick()
        composeTestRule.onNode(
            hasText("2 days") and hasAnyAncestor(hasTestTag(StatisticsTestTags.DAYS_DROPDOWN_MENU))
        ).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(StatisticsTestTags.TABLE_CHIP).performClick()
        composeTestRule.waitForIdle()
        Screengrab.screenshot("7")

        composeTestRule.onAllNodes(
            hasText("Selen (200 µg)") and hasAnyAncestor(hasTestTag(CoreUiTestTags.TABLE_DATA_ROW))
        ).onFirst().assertExists()

        composeTestRule.onNodeWithTag(StatisticsTestTags.TABLE_FILTER).performTextInput("B")

        composeTestRule.onAllNodes(
            hasText("B12 (500µg)") and hasAnyAncestor(hasTestTag(CoreUiTestTags.TABLE_DATA_ROW))
        ).onFirst().assertExists()

        composeTestRule.onNodeWithTag(StatisticsTestTags.CALENDAR_CHIP).performClick()
        Screengrab.screenshot("8")

        AndroidTestHelper.navigateTo(AndroidTestHelper.MainMenu.OVERVIEW)
    }
}
