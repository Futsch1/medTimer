package com.futsch1.medtimer

import android.os.Build
import android.os.RemoteException
import androidx.test.espresso.Espresso
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObjectNotFoundException
import androidx.test.uiautomator.UiSelector
import com.adevinta.android.barista.rule.BaristaRule
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.rules.TestName
import java.io.IOException
import java.time.LocalDate

abstract class BaseTestHelper {
    @JvmField
    @Rule
    var baristaRule: BaristaRule<MainActivity> = BaristaRule.create(MainActivity::class.java)

    @Rule
    @JvmField
    var mGrantPermissionRule: GrantPermissionRule? = permissionRule

    @Rule
    @JvmField
    var testName: TestName = TestName()

    protected var failureHandler: MyFailureHandler = MyFailureHandler(
        this.javaClass.getName(), testName,
        InstrumentationRegistry.getInstrumentation().targetContext
    )

    @Before
    fun setup() {
        Espresso.setFailureHandler(failureHandler)
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        try {
            device.wakeUp()
        } catch (_: RemoteException) {
            // Ignore
        }

        dismissAllNotifications(device)

        device.pressHome()
        baristaRule.launchActivity()

        if (!LocalDate.now().isEqual(LocalDate.of(2025, 8, 1))) {
            failureHandler.handle(
                AssertionError("Wrong date - tests require the date/time to be set to 01.08.2025, 16:00\nUse 'adb su 0 toybox date 0801160025' to set it."),
                ViewMatchers.withId(0)
            )
        }
    }

    private fun dismissAllNotifications(device: UiDevice) {
        try {
            device.openNotification()

            val clearAllButtons = device.findObjects(
                By.res("com.android.systemui:id/dismiss_text")
            ) + device.findObjects(
                By.res("com.android.systemui:id/dismiss_button")
            ) + device.findObjects(
                By.textContains("Clear all")
            ) + device.findObjects(
                By.textContains("CLEAR ALL")
            ) + device.findObjects(
                By.descContains("Clear all")
            )
            for (clearAllButton in clearAllButtons) {
                clearAllButton.click()
            }
        } catch (_: Exception) {
            device.pressBack()
        }
    }

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

        val permissionRule: GrantPermissionRule?
            get() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    return GrantPermissionRule.grant(
                        "android.permission.POST_NOTIFICATIONS",
                        "android.permission.USE_FULL_SCREEN_INTENT"
                    )
                }
                return null
            }
    }
}
