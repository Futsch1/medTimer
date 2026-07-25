package com.futsch1.medtimer

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.RemoteException
import androidx.annotation.StringRes
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.espresso.Espresso
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObjectNotFoundException
import androidx.test.uiautomator.UiSelector
import com.adevinta.android.barista.rule.BaristaRule
import com.futsch1.medtimer.core.ui.TimeFormatter
import com.futsch1.medtimer.di.TimeFormatterEntryPoint
import com.futsch1.medtimer.feature.ui.medicine.EditMedicineTestTags
import com.futsch1.medtimer.feature.ui.medicine.MedicinesMenuTestTags
import com.futsch1.medtimer.feature.ui.overview.OverviewTestTags
import com.futsch1.medtimer.utilities.grantAppPermission
import dagger.hilt.android.EntryPointAccessors
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.rules.TestName
import java.io.IOException
import java.time.LocalDate
import com.futsch1.medtimer.feature.ui.AppOptionsTestTags
import com.futsch1.medtimer.core.domain.model.OverviewFilter

abstract class BaseTestHelper {
    @JvmField
    @Rule
    var baristaRule: BaristaRule<MainActivity> = BaristaRule.create(MainActivity::class.java)

    // Empty rather than createAndroidComposeRule: BaristaRule already launches the activity, and two
    // launching rules would conflict. This one only attaches to the existing Compose hierarchy.
    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    @Rule
    @JvmField
    var grantPermissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        *buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(Manifest.permission.USE_FULL_SCREEN_INTENT)
            }
        }.toTypedArray()
    )

    @Rule
    @JvmField
    var testName: TestName = TestName()

    protected var failureHandler: MyFailureHandler = MyFailureHandler(
        this.javaClass.getName(), testName,
        InstrumentationRegistry.getInstrumentation().targetContext
    )

    protected fun timeFormatter(): TimeFormatter {
        val context = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
        return EntryPointAccessors.fromApplication(context, TimeFormatterEntryPoint::class.java).timeFormatter()
    }

    @Before
    fun setup() {
        Espresso.setFailureHandler(failureHandler)
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        try {
            device.wakeUp()
        } catch (_: RemoteException) {
            // Ignore
        }

        dismissAllNotifications()

        // Grant permissions which cannot be granted via the GrantPermissionRule
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            device.grantAppPermission("SCHEDULE_EXACT_ALARM")
        }

        device.pressHome()
        baristaRule.launchActivity()

        if (!LocalDate.now().isEqual(LocalDate.of(2025, 8, 1))) {
            failureHandler.handle(
                AssertionError("Wrong date - tests require the date/time to be set to 01.08.2025, 16:00\nUse 'adb su 0 toybox date 0801160025' to set it."),
                ViewMatchers.withId(0)
            )
        }
    }

    private fun dismissAllNotifications() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancelAll()
    }

    /** Opens the overflow shared by the three top-level screens. */
    protected fun openAppOptionsMenu() = clickTag(AppOptionsTestTags.OVERFLOW)

    /** Opens the medicine-list menu (bulk activation, sorting). */
    protected fun openMedicinesMenu() = clickTag(MedicinesMenuTestTags.OVERFLOW)

    /** Opens the menu of the medicine currently being edited. */
    protected fun openEditMedicineMenu() = clickTag(EditMedicineTestTags.OVERFLOW)

    /** Clicks the state button of the Overview event at [index], opening its quick-action menu. */
    protected fun clickOverviewEventState(index: Int) {
        composeTestRule.onAllNodesWithTag(OverviewTestTags.EVENT_STATE_BUTTON)[index].performClick()
    }

    protected fun clickOverviewEvent(index: Int) {
        composeTestRule.onAllNodesWithTag(OverviewTestTags.EVENT_CARD)[index].performClick()
    }

    protected fun longClickOverviewEvent(index: Int) {
        composeTestRule.onAllNodesWithTag(OverviewTestTags.EVENT_CARD)[index]
            .performTouchInput { longClick() }
    }

    protected fun overviewEventCount(): Int =
        composeTestRule.onAllNodesWithTag(OverviewTestTags.EVENT_CARD).fetchSemanticsNodes().size

    protected fun assertOverviewEventTextContains(index: Int, substring: String) {
        composeTestRule.onAllNodesWithTag(OverviewTestTags.EVENT_TEXT)[index]
            .assertTextContains(substring, substring = true)
    }

    /** The day-of-month currently selected in the Overview week strip. */
    protected fun selectedOverviewDay(): String =
        composeTestRule.onNodeWithTag(OverviewTestTags.SELECTED_DAY)
            .fetchSemanticsNode().config[SemanticsProperties.Text].first().text

    protected fun toggleOverviewFilter(filter: OverviewFilter) = clickTag(OverviewTestTags.filter(filter))

    protected fun clickTag(tag: String) {
        composeTestRule.onNodeWithTag(tag).performClick()
    }

    /** Asserts the Overview event at [index] shows the state named by [stateRes]. */
    protected fun assertOverviewEventState(index: Int, @StringRes stateRes: Int) {
        composeTestRule.onAllNodesWithTag(OverviewTestTags.EVENT_STATE_BUTTON)[index]
            .assertContentDescriptionEquals(getString(stateRes))
    }

    protected fun assertMenuItemDisplayed(@StringRes textRes: Int) {
        composeTestRule.onNodeWithText(getString(textRes)).assertIsDisplayed()
    }

    protected fun assertMenuItemNotDisplayed(@StringRes textRes: Int) {
        composeTestRule.onNodeWithText(getString(textRes)).assertDoesNotExist()
    }

    protected fun clickMenuItem(@StringRes textRes: Int) {
        composeTestRule.onNodeWithText(getString(textRes)).performClick()
    }

    protected fun getString(@StringRes textRes: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(textRes)

    protected fun internalAssert(b: Boolean) {
        if (!b) {
            failureHandler.handle(AssertionError("MedTimer test assert"), ViewMatchers.withId(0))
        }
    }

    companion object {
        @BeforeClass
        @Throws(UiObjectNotFoundException::class)
        @JvmStatic
        fun dismissANRSystemDialog() {
            val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            try {
                device.wakeUp()
            } catch (_: RemoteException) {
                // Ignore
            }
            // If the device is running in English Locale
            val waitButton = device.findObject(UiSelector().textContains("wait"))
            if (waitButton.exists()) {
                waitButton.click()
            }
            try {
                UiDevice
                    .getInstance(InstrumentationRegistry.getInstrumentation())
                    .executeShellCommand(
                        "am broadcast -a android.intent.action.CLOSE_SYSTEM_DIALOGS"
                    )
            } catch (_: IOException) {
                // Intentionally empty
            }
        }
    }
}
