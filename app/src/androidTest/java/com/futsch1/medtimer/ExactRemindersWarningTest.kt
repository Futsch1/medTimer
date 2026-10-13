package com.futsch1.medtimer

import com.adevinta.android.barista.rule.flaky.AllowFlaky
import com.futsch1.medtimer.core.common.di.IsDebugBuild
import com.futsch1.medtimer.core.common.di.TimeAccessModule
import com.futsch1.medtimer.core.common.time.TimeAccess
import com.futsch1.medtimer.core.datastore.PreferencesDataSource
import com.futsch1.medtimer.core.ui.R
import com.futsch1.medtimer.di.BuildConfigModule
import com.futsch1.medtimer.utilities.pollUntil
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import javax.inject.Inject

/** Debug builds suppress the overview warnings, so this test injects a release build to see them. */
@HiltAndroidTest
@UninstallModules(TimeAccessModule::class, BuildConfigModule::class)
class ExactRemindersWarningTest : MedTimerTestBase() {
    @BindValue
    @IsDebugBuild
    val isDebugBuild = false

    /** A test with its own bindings gets its own component, which does not see the base class's binding. */
    @BindValue
    val frozenTimeAccess: TimeAccess = timeAccess

    @Inject
    lateinit var preferencesDataSource: PreferencesDataSource

    @Test
    @AllowFlaky(attempts = 3)
    fun enableExactRemindersFromWarning() {
        assertFalse(preferencesDataSource.preferences.value.exactReminders)

        navigation.toOverview()
        overview.assertWarningShown(R.string.exact_reminders_warning_title)
        overview.clickWarningButton(R.string.enable)

        overview.assertWarningGone(R.string.exact_reminders_warning_title)
        assertTrue(pollUntil { preferencesDataSource.preferences.value.exactReminders })
    }
}
