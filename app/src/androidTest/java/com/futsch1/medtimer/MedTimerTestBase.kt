package com.futsch1.medtimer

import androidx.annotation.StringRes
import androidx.test.platform.app.InstrumentationRegistry
import com.futsch1.medtimer.core.common.di.TimeAccessModule
import com.futsch1.medtimer.core.common.helpers.MedicineHelper
import com.futsch1.medtimer.core.common.time.TimeAccess
import com.futsch1.medtimer.core.ui.TimeFormatter
import com.futsch1.medtimer.di.TimeFormatterEntryPoint
import com.futsch1.medtimer.harness.FakeTimeAccess
import com.futsch1.medtimer.harness.MedTimerTestHarness
import com.futsch1.medtimer.robots.Robots
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.rules.RuleChain
import org.junit.runners.model.Statement
import java.time.LocalTime
import java.time.temporal.ChronoUnit

/**
 * The harness, the robots and the values a test needs to build its expectations.
 * Anything that touches the UI lives in a robot - see docs/guidelines/testing.md.
 */
@HiltAndroidTest
@UninstallModules(TimeAccessModule::class)
abstract class MedTimerTestBase {
    private val hiltRule = HiltAndroidRule(this)

    @BindValue
    val timeAccess: TimeAccess = FakeTimeAccess()

    private val harness = MedTimerTestHarness(this.javaClass.name)

    @get:Rule
    val ruleChain: RuleChain = RuleChain.outerRule(hiltRule)
        .around { base, _ ->
            object : Statement() {
                override fun evaluate() {
                    hiltRule.inject()
                    base.evaluate()
                }
            }
        }
        .around(harness)

    protected val baristaRule get() = harness.baristaRule

    private val robots by lazy { Robots(harness.composeTestRule) }

    protected val dialogs get() = robots.dialogs
    protected val pickers get() = robots.pickers
    protected val preferences get() = robots.preferences
    protected val navigation get() = robots.navigation
    protected val menus get() = robots.menus

    protected val overview get() = robots.overview
    protected val medicines get() = robots.medicines
    protected val statistics get() = robots.statistics
    protected val settings get() = robots.settings
    protected val notifications get() = robots.notifications

    protected val reminders get() = robots.reminders
    protected val reminderSettings get() = robots.reminderSettings
    protected val medicineEditor get() = robots.medicineEditor
    protected val medicineSettings get() = robots.medicineSettings
    protected val eventEditor get() = robots.eventEditor
    protected val manualDose get() = robots.manualDose
    protected val tags get() = robots.tags
    protected val notes get() = robots.notes
    protected val calendar get() = robots.calendar
    protected val appIntro get() = robots.appIntro
    protected val alarm get() = robots.alarm
    protected val shareSheet get() = robots.shareSheet
    protected val export get() = robots.export

    protected fun amount(value: Double): String = MedicineHelper.formatAmount(value, "")

    protected fun timeFormatter(): TimeFormatter {
        val context = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
        return EntryPointAccessors.fromApplication(context, TimeFormatterEntryPoint::class.java).timeFormatter()
    }

    /** The frozen instant the app itself sees for the duration of this test, as a time of day. */
    private fun frozenNow(): LocalTime = timeAccess.now().atZone(timeAccess.systemZone()).toLocalTime()

    /** Close enough that the scheduler raises it while the test waits, far enough not to have fired yet. */
    protected fun aboutToFire(): LocalTime = frozenNow().plusMinutes(10)

    /** A time still ahead of now, so a reminder created with it stays scheduled for today. */
    protected fun laterToday(): LocalTime {
        val now = frozenNow()
        val later = now.plusHours(2)
        // plusHours wraps at midnight, which would put the reminder before now instead of after it.
        return if (later > now) later else LocalTime.MAX.truncatedTo(ChronoUnit.MINUTES)
    }

    /** A time already behind now, so a reminder created with it counts as today's dose having passed. */
    protected fun earlierToday(): LocalTime {
        val now = frozenNow()
        val earlier = now.minusHours(2)
        return if (earlier < now) earlier else LocalTime.MIN
    }

    protected fun getString(@StringRes textRes: Int): String = robots.getString(textRes)

    companion object {
        @BeforeClass
        @JvmStatic
        fun dismissANRSystemDialog() = MedTimerTestHarness.dismissANRSystemDialog()
    }
}
