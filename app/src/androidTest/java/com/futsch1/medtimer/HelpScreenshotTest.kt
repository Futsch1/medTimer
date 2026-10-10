package com.futsch1.medtimer

import com.adevinta.android.barista.rule.flaky.AllowFlaky
import com.futsch1.medtimer.core.ui.R
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.ClassRule
import org.junit.Test
import tools.fastlane.screengrab.Screengrab
import tools.fastlane.screengrab.UiAutomatorScreenshotStrategy
import tools.fastlane.screengrab.locale.LocaleTestRule
import java.time.LocalDate
import java.time.LocalTime

@HiltAndroidTest
class HelpScreenshotTest : MedTimerTestBase() {
    companion object {
        @JvmField
        @ClassRule
        val localeTestRule: LocaleTestRule = LocaleTestRule()
    }

    @Test
    @AllowFlaky(attempts = 3)
    fun capturesGettingStartedReminder() {
        Screengrab.setDefaultScreenshotStrategy(UiAutomatorScreenshotStrategy())

        medicines.create("Medicine A")
        medicines.showList()
        medicines.clickNamed("Medicine A")
        medicineEditor.addReminder("1", LocalTime.of(8, 0))

        navigation.toOverview()
        overview.selectDay(LocalDate.now().plusDays(1))
        overview.assertEventContains("Medicine A")
        toast.waitUntilGone(
            androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
                .targetContext.getString(R.string.successfully_created_reminder)
        )
        Screengrab.screenshot("getting-started-medicine-reminder")
    }
}
