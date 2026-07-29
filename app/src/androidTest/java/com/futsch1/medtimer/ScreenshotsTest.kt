package com.futsch1.medtimer

import com.adevinta.android.barista.rule.flaky.AllowFlaky
import com.futsch1.medtimer.core.domain.model.StatisticFragment
import com.futsch1.medtimer.core.ui.R
import com.futsch1.medtimer.feature.ui.statistics.ANALYSIS_RANGES
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.ClassRule
import org.junit.Test
import tools.fastlane.screengrab.Screengrab
import tools.fastlane.screengrab.UiAutomatorScreenshotStrategy
import tools.fastlane.screengrab.locale.LocaleTestRule


private const val GINSENG = "Ginseng (200mg)"
private const val B12 = "B12 (500µg)"
private const val SELEN = "Selen (200 µg)"

@HiltAndroidTest
class ScreenshotsTest : MedTimerTestBase() {
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
        menus.clickAppOption(R.string.generate_test_data)

        eventEditor.forEvent(0) { assertMedicineNotes("Some note") }

        overview.take(GINSENG)
        overview.skip(B12)

        notifications.inShade {
            expandFor(actionLabel(R.string.taken))
            Screengrab.screenshot("5")
        }

        overview.take(SELEN)

        Screengrab.screenshot("1")

        navigation.toMedicines()
        Screengrab.screenshot("2")

        medicines.clickItem(0)
        Screengrab.screenshot("3")

        reminders.inSettingsOf(0) { Screengrab.screenshot("4") }

        navigation.toAnalysis()
        // Default view is Charts; no chip click needed
        Screengrab.screenshot("6")

        statistics.selectRange(ANALYSIS_RANGES[1].second)

        statistics.selectView(StatisticFragment.TABLE)
        Screengrab.screenshot("7")

        statistics.sortByColumn(R.string.name)
        statistics.assertTableContains("Selen")

        statistics.filter("B")
        statistics.assertTableContains("B12")
        statistics.clearFilter()

        statistics.selectView(StatisticFragment.CALENDAR)
        Screengrab.screenshot("8")

        navigation.toOverview()
    }
}
