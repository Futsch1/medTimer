package com.futsch1.medtimer

import com.adevinta.android.barista.rule.flaky.AllowFlaky
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Test

@HiltAndroidTest
class HelpReaderTest : MedTimerTestBase() {
    @Test
    @AllowFlaky(attempts = 3)
    fun helpReaderNavigationWorksFromMenuAndNotificationSettings() {
        help.open()
        help.assertContentsDisplayed()
        help.openNotificationsMissingOrLate()
        help.assertNotificationsMissingOrLateDisplayed()
        help.pressToolbarBack()
        help.assertContentsDisplayed()

        help.openGettingStarted()
        help.assertGettingStartedDisplayed()
        help.openReminderTypes()
        help.assertReminderTypesDisplayed()
        help.pressToolbarBack()
        help.assertGettingStartedDisplayed()
        help.pressToolbarBack()
        help.assertContentsDisplayed()
        help.pressBack()
        navigation.assertOverviewShown()

        settings.openNotificationsHelp()
        help.assertNotificationsMissingOrLateDisplayed()
        help.pressBack()
        settings.returnToOverviewFromNotificationSettings()
        navigation.assertOverviewShown()
    }
}
