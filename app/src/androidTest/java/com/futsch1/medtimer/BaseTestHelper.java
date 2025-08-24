package com.futsch1.medtimer;

import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;

import android.os.Build;

import androidx.test.espresso.Espresso;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.GrantPermissionRule;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiSelector;

import com.adevinta.android.barista.rule.BaristaRule;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TestName;

import java.io.IOException;
import java.time.LocalDate;

public abstract class BaseTestHelper {
    @Rule
    public BaristaRule<MainActivity> baristaRule = BaristaRule.create(MainActivity.class);
    @Rule
    public GrantPermissionRule mGrantPermissionRule = getPermissionRule();
    @Rule
    public TestName testName = new TestName();

    protected MyFailureHandler failureHandler = new MyFailureHandler(this.getClass().getName(), testName,
            getInstrumentation().getTargetContext());

    @BeforeClass
    public static void dismissANRSystemDialog() throws UiObjectNotFoundException {
        UiDevice device = UiDevice.getInstance(getInstrumentation());
        // If the device is running in English Locale
        UiObject waitButton = device.findObject(new UiSelector().textContains("wait"));
        if (waitButton.exists()) {
            waitButton.click();
        }
        try {
            UiDevice
                    .getInstance(InstrumentationRegistry.getInstrumentation())
                    .executeShellCommand(
                            "am broadcast -a android.intent.action.CLOSE_SYSTEM_DIALOGS");
        } catch (IOException e) {
            // Intentionally empty
        }
    }

    public static GrantPermissionRule getPermissionRule() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return GrantPermissionRule.grant(
                    "android.permission.POST_NOTIFICATIONS");
        }
        return null;
    }

    @Before
    public void setup() {
        Espresso.setFailureHandler(failureHandler);
        UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        device.pressHome();
        baristaRule.launchActivity();

        if (!LocalDate.now().isEqual(LocalDate.of(2025, 8, 1))) {
            failureHandler.handle(new AssertionError("Wrong date - tests require the date/time to be set to 01.08.2025, 16:00\nUse 'adb su 0 toybox date' to set it."), withId(0));
        }
    }

    protected void internalAssert(boolean b) {
        if (!b) {
            failureHandler.handle(new AssertionError("MedTimer test assert"), withId(0));
        }
    }
}
