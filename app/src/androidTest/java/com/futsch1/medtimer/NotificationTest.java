package com.futsch1.medtimer;


import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withResourceName;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static org.hamcrest.Matchers.allOf;
import static org.junit.Assert.assertNotNull;

import android.view.InputDevice;
import android.view.MotionEvent;

import androidx.test.espresso.action.GeneralClickAction;
import androidx.test.espresso.action.GeneralLocation;
import androidx.test.espresso.action.Press;
import androidx.test.espresso.action.Tap;
import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.filters.LargeTest;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.Direction;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.Until;

import org.junit.Rule;
import org.junit.Test;

@LargeTest
public class NotificationTest extends BaseTestHelper {

    @Rule
    public ActivityScenarioRule<MainActivity> mActivityScenarioRule =
            new ActivityScenarioRule<>(MainActivity.class);

    @Test
    public void notificationTest() {
        AndroidTestHelper.createMedicine("Test med");
        
        // Set color and icon
        onView(withId(R.id.enableColor)).perform(click());
        onView(withId(R.id.selectColor)).perform(click());
        onView(withResourceName("colorPickerView")).perform(new GeneralClickAction(Tap.SINGLE, GeneralLocation.CENTER_LEFT, Press.FINGER, InputDevice.SOURCE_UNKNOWN, MotionEvent.BUTTON_PRIMARY));
        onView(withId(android.R.id.button1)).perform(click());

        onView(withId(R.id.selectIcon)).perform(click());
        onView(withResourceName("icd_rcv_icon_list")).perform(RecyclerViewActions.actionOnItemAtPosition(1, click()));

        AndroidTestHelper.createReminder("1", AndroidTestHelper.getNextNotificationTime().toLocalTime());

        onView(withId(R.id.open_advanced_settings)).perform(click());

        onView(withId(R.id.addLinkedReminder)).perform(click());
        onView(allOf(withId(android.R.id.button1), withText("OK"))).perform(scrollTo(), click());
        AndroidTestHelper.setTime(0, 1, true);

        AndroidTestHelper.navigateTo(AndroidTestHelper.MainMenu.OVERVIEW);

        mActivityScenarioRule.getScenario().close();

        UiDevice device = UiDevice.getInstance(getInstrumentation());
        device.openNotification();
        UiObject2 object = device.wait(Until.findObject(By.textContains("Test med")), 240_000);
        assertNotNull(object);
        object.fling(Direction.RIGHT);
        object = device.wait(Until.findObject(By.textContains("Test med")), 180_000);
        assertNotNull(object);
        device.pressBack();
    }

}
