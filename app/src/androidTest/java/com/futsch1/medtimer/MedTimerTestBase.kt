package com.futsch1.medtimer

import androidx.annotation.StringRes
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.RootMatchers
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.futsch1.medtimer.core.common.helpers.MedicineHelper
import com.futsch1.medtimer.core.ui.TimeFormatter
import com.futsch1.medtimer.di.TimeFormatterEntryPoint
import com.futsch1.medtimer.harness.MedTimerTestHarness
import com.futsch1.medtimer.robots.ComposeUi
import com.futsch1.medtimer.robots.MaterialPickers
import com.futsch1.medtimer.robots.MedicineEditorRobot
import com.futsch1.medtimer.robots.MedicinesRobot
import com.futsch1.medtimer.robots.MenuRobot
import com.futsch1.medtimer.robots.NavigationRobot
import com.futsch1.medtimer.robots.NotificationShadeRobot
import com.futsch1.medtimer.robots.OverviewRobot
import com.futsch1.medtimer.robots.PreferenceScreenRobot
import com.futsch1.medtimer.robots.SettingsRobot
import com.futsch1.medtimer.robots.StatisticsRobot
import dagger.hilt.android.EntryPointAccessors
import org.hamcrest.Matchers
import org.junit.BeforeClass
import org.junit.Rule
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import kotlin.test.assertTrue

/**
 * Holds the harness and the screen robots.
 * Interactions with a screen belong in its robot;
 * what stays here is app-wide navigation, the menus and the legacy View surfaces not yet on Compose.
 */
abstract class MedTimerTestBase {
    @get:Rule
    val harness = MedTimerTestHarness(this.javaClass.name)

    protected val baristaRule get() = harness.baristaRule

    private val ui by lazy { ComposeUi(harness.composeTestRule) }

    protected val preferences by lazy { PreferenceScreenRobot(ui) }
    protected val pickers by lazy { MaterialPickers() }
    protected val navigation by lazy { NavigationRobot(ui) }
    protected val menus by lazy { MenuRobot(ui) }

    protected val overview by lazy { OverviewRobot(ui) }
    protected val medicines by lazy { MedicinesRobot(ui, navigation) }
    protected val statistics by lazy { StatisticsRobot(ui) }
    protected val medicineEditor by lazy { MedicineEditorRobot(ui, menus, preferences, pickers) }
    protected val settings by lazy { SettingsRobot(menus, preferences) }
    protected val notifications by lazy { NotificationShadeRobot() }

    protected fun amount(value: Double): String = MedicineHelper.formatAmount(value, "")

    protected fun timeFormatter(): TimeFormatter {
        val context = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
        return EntryPointAccessors.fromApplication(context, TimeFormatterEntryPoint::class.java).timeFormatter()
    }

    /** Close enough that the scheduler raises it while the test waits, far enough not to have fired yet. */
    protected fun aboutToFire(): LocalTime = LocalTime.now().plusMinutes(10)

    /** A time still ahead of now, so a reminder created with it stays scheduled for today. */
    protected fun laterToday(): LocalTime {
        val now = LocalTime.now()
        val later = now.plusHours(2)
        // plusHours wraps at midnight, which would put the reminder before now instead of after it.
        return if (later > now) later else LocalTime.MAX.truncatedTo(ChronoUnit.MINUTES)
    }

    protected fun getString(@StringRes textRes: Int): String = ui.getString(textRes)

    /** Tags live in a View dialog with its own list, so these scope to it rather than matching loose text. */
    private fun tagChip(name: String) = onView(
        Matchers.allOf(
            ViewMatchers.withText(name),
            ViewMatchers.isDescendantOfA(ViewMatchers.withId(com.futsch1.medtimer.feature.ui.R.id.tags)),
        )
    )

    protected fun clickTagChip(name: String) {
        tagChip(name).perform(ViewActions.click())
    }

    protected fun assertTagChipChecked(name: String) {
        tagChip(name).check(ViewAssertions.matches(ViewMatchers.isChecked()))
    }

    protected fun assertTagChipNotChecked(name: String) {
        tagChip(name).check(ViewAssertions.matches(ViewMatchers.isNotChecked()))
    }

    protected fun assertTagChipDisplayed(name: String) {
        tagChip(name).check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }

    protected fun assertTagChipDoesNotExist(name: String) {
        tagChip(name).check(ViewAssertions.doesNotExist())
    }

    /** The share sheet is a system window, so it is matched through UiAutomator rather than Espresso. */
    protected fun assertShareSheetShown() {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        assertTrue(device.wait(Until.hasObject(By.textContains("Sharing")), 15_000), "Share sheet did not open")
        device.pressBack()
        device.waitForIdle()
    }

    /** [descriptionRes] is the export entry's spoken name, which says both what and in which format. */
    protected fun exportViaAppOptions(@StringRes descriptionRes: Int) {
        menus.clickAppOptionNamed(descriptionRes)
        assertShareSheetShown()
    }

    protected fun assertPreferenceSummary(@StringRes titleRes: Int, expected: String) =
        preferences.assertSummary(titleRes, expected)

    protected fun clickPreference(@StringRes titleRes: Int) = preferences.click(titleRes)

    /** Position-independent: reminder order follows the reminder times, which move with the clock. */
    protected fun assertReminderListContains(text: String) {
        onView(
            Matchers.allOf(
                ViewMatchers.withSubstring(text),
                ViewMatchers.isDescendantOfA(ViewMatchers.withId(com.futsch1.medtimer.feature.ui.R.id.reminderList)),
            )
            // Effective visibility rather than isDisplayed: a card further down the list is bound but
            // off-screen, and this asserts the reminder exists rather than where it sits.
        ).check(ViewAssertions.matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
    }

    private fun dialogItem(text: String) = onView(
        Matchers.allOf(
            ViewMatchers.withText(text),
            ViewMatchers.isDescendantOfA(ViewMatchers.withId(androidx.appcompat.R.id.select_dialog_listview)),
        )
    ).inRoot(RootMatchers.isDialog())

    protected fun clickDialogItem(text: String) {
        dialogItem(text).perform(ViewActions.click())
    }

    protected fun clickDialogItem(@StringRes textRes: Int) = clickDialogItem(getString(textRes))

    /**
     * The amount and snooze dialogs open from a notification action, so the app has still to come
     * back to the foreground; matching in the dialog root is what waits for that window.
     */
    protected fun awaitInputDialog() {
        onView(ViewMatchers.withId(android.R.id.input))
            .inRoot(RootMatchers.isDialog())
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }

    /** Asserts text inside the dialog window, rather than anywhere in the app. */
    protected fun assertDialogContains(text: String) {
        onView(ViewMatchers.withText(Matchers.containsString(text)))
            .inRoot(RootMatchers.isDialog())
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }

    protected fun assertDialogItemDisplayed(text: String) {
        dialogItem(text).check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }

    protected fun assertDialogItemChecked(@StringRes textRes: Int) = assertDialogItemChecked(getString(textRes))

    protected fun assertDialogItemChecked(text: String) {
        dialogItem(text).check(ViewAssertions.matches(ViewMatchers.isChecked()))
    }

    protected fun assertDialogItemNotChecked(@StringRes textRes: Int) = assertDialogItemNotChecked(getString(textRes))

    protected fun assertDialogItemNotChecked(text: String) {
        dialogItem(text).check(ViewAssertions.matches(ViewMatchers.isNotChecked()))
    }

    companion object {
        @BeforeClass
        @JvmStatic
        fun dismissANRSystemDialog() = MedTimerTestHarness.dismissANRSystemDialog()
    }
}
