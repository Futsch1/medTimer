package com.futsch1.medtimer;

import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;

import android.icu.util.Calendar;
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

import java.io.IOException;
import java.time.LocalDateTime;

public abstract class BaseTestHelper {
    @Rule
    public BaristaRule<MainActivity> baristaRule = BaristaRule.create(MainActivity.class);
    @Rule
    public GrantPermissionRule mGrantPermissionRule = getPermissionRule();

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
            System.out.println("Exception: " + e);
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
        Espresso.setFailureHandler(new MyFailureHandler(this.getClass().getName(),
                getInstrumentation().getTargetContext()));
        UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        device.pressHome();
        baristaRule.launchActivity();
    }

    protected boolean isNotTimeBetween9And23() {
        Calendar rightNow = Calendar.getInstance();
        LocalDateTime dateTime = LocalDateTime.of(rightNow.get(Calendar.YEAR), rightNow.get(Calendar.MONTH) + 1, rightNow.get(Calendar.DAY_OF_MONTH), rightNow.get(Calendar.HOUR_OF_DAY), rightNow.get(Calendar.MINUTE), 0);
        return (dateTime.getHour() < 9 || dateTime.getHour() > 23);
    }
}
