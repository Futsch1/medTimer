package com.futsch1.medtimer;

import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;

import android.icu.util.Calendar;
import android.os.Build;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiSelector;

import org.junit.Before;
import org.junit.BeforeClass;

import java.io.IOException;
import java.time.LocalDateTime;

public class BaseTestHelper {
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

    @Before
    @SuppressWarnings("java:S2925")
    public void setUp() {
        try {
            Thread.sleep(2000);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                UiDevice device = UiDevice.getInstance(getInstrumentation());
                UiObject allowPermissions = device.findObject(new UiSelector()
                        .clickable(true)
                        .checkable(false)
                        .index(0));
                if (allowPermissions.exists()) {
                    allowPermissions.click();
                }
            }
        } catch (UiObjectNotFoundException | InterruptedException e) {
            System.out.println("There is no permissions dialog to interact with");
        }
        Calendar rightNow = Calendar.getInstance();
        LocalDateTime dateTime = LocalDateTime.of(rightNow.get(Calendar.YEAR), rightNow.get(Calendar.MONTH) + 1, rightNow.get(Calendar.DAY_OF_MONTH), rightNow.get(Calendar.HOUR_OF_DAY), rightNow.get(Calendar.MINUTE), 0);
        assert (dateTime.getHour() >= 18 && dateTime.getHour() <= 23);
    }
}
