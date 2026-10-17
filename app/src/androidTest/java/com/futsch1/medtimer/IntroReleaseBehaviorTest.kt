package com.futsch1.medtimer

import com.adevinta.android.barista.rule.flaky.AllowFlaky
import com.futsch1.medtimer.core.common.di.IsDebugBuild
import com.futsch1.medtimer.core.common.di.TimeAccessModule
import com.futsch1.medtimer.core.common.time.TimeAccess
import com.futsch1.medtimer.core.ui.R
import com.futsch1.medtimer.harness.FakeTimeAccess
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import org.junit.Test

/** Exercises the production first-launch path without building a release APK. */
@HiltAndroidTest
@UninstallModules(TimeAccessModule::class, com.futsch1.medtimer.di.BuildConfigModule::class)
class IntroReleaseBehaviorTest : MedTimerTestBase() {
    @BindValue
    @IsDebugBuild
    val isDebugBuild = false

    @BindValue
    val releaseTimeAccess: TimeAccess = FakeTimeAccess()

    @Test
    @AllowFlaky(attempts = 3)
    fun firstLaunchShowsShortIntroAndCompletionReturnsToOverview() {
        appIntro.assertPage(R.string.intro_welcome, R.string.intro_welcome_description)
        appIntro.next()
        appIntro.assertPage(R.string.intro_setup, R.string.intro_setup_description)
        appIntro.next()
        appIntro.assertPage(R.string.tab_overview, R.string.intro_overview_orientation_description)
        appIntro.next()
        appIntro.assertPage(R.string.intro_notifications, R.string.intro_notifications_description)
        appIntro.done()

        navigation.assertOverviewShown()
    }

    @Test
    @AllowFlaky(attempts = 3)
    fun skippingIntroReturnsToOverview() {
        appIntro.assertPage(R.string.intro_welcome, R.string.intro_welcome_description)
        appIntro.skip()

        navigation.assertOverviewShown()
    }
}
