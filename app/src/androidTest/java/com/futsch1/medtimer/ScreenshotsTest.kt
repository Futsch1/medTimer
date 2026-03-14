package com.futsch1.medtimer

import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import com.adevinta.android.barista.interaction.BaristaListInteractions.clickListItem
import com.adevinta.android.barista.interaction.BaristaListInteractions.clickListItemChild
import com.adevinta.android.barista.interaction.BaristaMenuClickInteractions.openMenu
import com.adevinta.android.barista.rule.flaky.AllowFlaky
import com.evrencoskun.tableview.TableView
import com.futsch1.medtimer.AndroidTestHelper.navigateTo
import com.futsch1.medtimer.utilities.closeNotification
import junit.framework.TestCase
import org.junit.ClassRule
import org.junit.Test
import tools.fastlane.screengrab.Screengrab
import tools.fastlane.screengrab.UiAutomatorScreenshotStrategy
import tools.fastlane.screengrab.locale.LocaleTestRule
import java.util.concurrent.atomic.AtomicReference


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

        openMenu()
        clickOn(R.string.generate_test_data)

        clickListItemChild(R.id.reminders, 0, R.id.overviewContentContainer)
        internalAssert(device.findObject(By.textContains("Some note")) != null)
        Espresso.pressBack()

        clickListItemChild(R.id.reminders, 0, R.id.stateButton)
        clickOn(R.id.takenButton)
        clickListItemChild(R.id.reminders, 2, R.id.stateButton)
        clickOn(R.id.takenButton)
        clickListItemChild(R.id.reminders, 3, R.id.stateButton)
        clickOn(R.id.skippedButton)

        device.openNotification()
        makeNotificationExpanded(device, getNotificationText(R.string.taken))
        Screengrab.screenshot("5")
        device.closeNotification()

        clickListItemChild(R.id.reminders, 4, R.id.stateButton)
        clickOn(R.id.takenButton)

        Screengrab.screenshot("1")

        navigateTo(AndroidTestHelper.MainMenu.MEDICINES)
        Screengrab.screenshot("2")

        Espresso.onView(ViewMatchers.withId(R.id.medicineList)).perform(
            RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(
                0, ViewActions.click()
            )
        )
        Screengrab.screenshot("3")

        clickListItemChild(R.id.reminderList, 0, R.id.openAdvancedSettings)
        AndroidTestHelper.waitForIdle(500)
        Screengrab.screenshot("4")

        navigateTo(AndroidTestHelper.MainMenu.ANALYSIS)
        clickOn(R.id.chartChip)
        Screengrab.screenshot("6")

        clickOn(R.id.timeSpinner)

        clickListItem(position = 1)

        clickOn(R.id.tableChip)
        Screengrab.screenshot("7")

        clickListItem(com.evrencoskun.tableview.R.id.ColumnHeaderRecyclerView, 1)

        val tableView = AtomicReference<TableView>()
        tableView.set(
            baristaRule.activityTestRule.getActivity().findViewById(R.id.reminder_table)
        )

        var view = tableView.get().cellRecyclerView.findViewWithTag<TextView>("medicineName")
        TestCase.assertEquals("Selen (200 µg)", view.getText())

        Espresso.onView(ViewMatchers.withId(R.id.filter))
            .perform(ViewActions.replaceText("B"), ViewActions.closeSoftKeyboard())

        view = tableView.get().cellRecyclerView.findViewWithTag("medicineName")
        TestCase.assertEquals("B12 (500µg)", view.getText())

        clickOn(com.google.android.material.R.id.text_input_end_icon)

        clickOn(R.id.calendarChip)
        Screengrab.screenshot("8")

        navigateTo(AndroidTestHelper.MainMenu.OVERVIEW)
    }
}
