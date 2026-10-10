package com.futsch1.medtimer

import com.adevinta.android.barista.rule.flaky.AllowFlaky
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Test

@HiltAndroidTest
class HelpReaderTest : MedTimerTestBase() {
    @Test
    @AllowFlaky(attempts = 3)
    fun toolbarBackReturnsToContentsBeforeClosing() {
        help.open()
        help.assertContentsDisplayed()

        help.openGettingStarted()
        help.assertGettingStartedDisplayed()
        help.pressToolbarBack()
        help.assertContentsDisplayed()
        help.pressToolbarBack()
        navigation.assertOverviewShown()
    }

    @Test
    @AllowFlaky(attempts = 3)
    fun systemBackReturnsToContentsBeforeClosing() {
        help.open()
        help.assertContentsDisplayed()

        help.openGettingStarted()
        help.assertGettingStartedDisplayed()
        help.pressBack()
        help.assertContentsDisplayed()
        help.pressBack()
        navigation.assertOverviewShown()
    }
}
