package com.futsch1.medtimer

import com.adevinta.android.barista.rule.flaky.AllowFlaky
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.ClassRule
import org.junit.Test
import tools.fastlane.screengrab.Screengrab
import tools.fastlane.screengrab.UiAutomatorScreenshotStrategy
import tools.fastlane.screengrab.locale.LocaleTestRule
import java.time.LocalTime

@HiltAndroidTest
class HelpScreenshotTest : MedTimerTestBase() {
    companion object {
        private const val TEST_MEDICINE_NAME = "Medicine A"

        @JvmField
        @ClassRule
        val localeTestRule: LocaleTestRule = LocaleTestRule()
    }

    @Test
    @AllowFlaky(attempts = 3)
    fun capturesGettingStartedScreenshots() {
        Screengrab.setDefaultScreenshotStrategy(UiAutomatorScreenshotStrategy())

        medicines.assertAddMedicineGuidanceDisplayed()
        medicines.create(TEST_MEDICINE_NAME)
        medicines.showList()
        medicines.clickNamed(TEST_MEDICINE_NAME)
        medicineEditor.assertTitle(TEST_MEDICINE_NAME)
        medicineEditor.assertReminderSetupGuidanceDisplayed()

        val reminderTime = LocalTime.of(8, 0)
        medicineEditor.addReminder("1", reminderTime)

        navigation.toOverview()
        overview.selectDay(frozenToday().plusDays(1))
        overview.assertEventContains(TEST_MEDICINE_NAME)
        overview.assertEventContains(timeFormatter().minutesToTimeString(reminderTime.hour * 60 + reminderTime.minute))
        toast.waitForToastToExpire()
        Screengrab.screenshot("getting-started-upcoming-reminder")
    }
}
