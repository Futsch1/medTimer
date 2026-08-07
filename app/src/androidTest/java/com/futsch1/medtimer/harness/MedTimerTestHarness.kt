package com.futsch1.medtimer.harness

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.RemoteException
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.test.espresso.Espresso.setFailureHandler
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import com.adevinta.android.barista.rule.BaristaRule
import com.futsch1.medtimer.MainActivity
import com.futsch1.medtimer.MyFailureHandler
import com.futsch1.medtimer.feature.ui.overview.overviewEventListCacheWindowEnabled
import com.futsch1.medtimer.feature.ui.overview.overviewEventListScrollToNowEnabled
import com.futsch1.medtimer.utilities.grantAppPermission
import org.junit.rules.RuleChain
import org.junit.rules.TestName
import org.junit.rules.TestRule
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runners.model.Statement
import java.io.IOException

/**
 * Everything a MedTimer instrumented test needs before its first interaction, as one rule with an
 * explicit order: name and failure capture, permissions, device state, the activity, Compose.
 */
class MedTimerTestHarness(testClassName: String) : TestRule {

    val baristaRule: BaristaRule<MainActivity> = BaristaRule.create(MainActivity::class.java)
    val composeTestRule: ComposeTestRule = createEmptyComposeRule()

    private val testName = TestName()

    val failureHandler: MyFailureHandler = MyFailureHandler(
        testClassName, testName, InstrumentationRegistry.getInstrumentation().targetContext
    )

    private val screenshotOnFailure = object : TestWatcher() {
        override fun failed(e: Throwable, description: Description) = failureHandler.capture(e)
    }

    private val grantPermissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        *buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(Manifest.permission.USE_FULL_SCREEN_INTENT)
            }
        }.toTypedArray()
    )

    override fun apply(base: Statement, description: Description): Statement =
        RuleChain.outerRule(testName)
            .around(screenshotOnFailure)
            .around(grantPermissionRule)
            // Inside the Barista rule so a retried @AllowFlaky attempt starts from a fresh app.
            .around(baristaRule)
            .around(composeTestRule)
            .around(startApp)
            .apply(base, description)

    private val startApp = TestRule { base, _ ->
        object : Statement() {
            override fun evaluate() {
                prepareDevice()
                setFailureHandler(failureHandler)
                failureHandler.resetCapture()
                baristaRule.launchActivity()
                base.evaluate()
            }
        }
    }

    private fun prepareDevice() {
        // The cache window's off-screen rows are composed-but-unplaced,
        // which reports a stale semantics position that confuses UI tests asserting on row order.
        overviewEventListCacheWindowEnabled = false

        // The list otherwise jumps to the next upcoming event, scrolling already-acted-on events out of composition.
        overviewEventListScrollToNowEnabled = false

        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        try {
            device.wakeUp()
        } catch (_: RemoteException) {
            // Ignore
        }

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancelAll()

        // Grant permissions which cannot be granted via the GrantPermissionRule
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            device.grantAppPermission("SCHEDULE_EXACT_ALARM")
        }

        // Pre-confirms the immersive-mode panel so it can't steal window focus from the alarm screen.
        try {
            device.executeShellCommand("settings put secure immersive_mode_confirmations confirmed")
        } catch (_: IOException) {
            // Intentionally empty
        }

        device.pressHome()
    }

    companion object {
        /** Runs once per class, before any rule, so a leftover system dialog cannot swallow the first tap. */
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
                device.executeShellCommand("am broadcast -a android.intent.action.CLOSE_SYSTEM_DIALOGS")
            } catch (_: IOException) {
                // Intentionally empty
            }
        }
    }
}
