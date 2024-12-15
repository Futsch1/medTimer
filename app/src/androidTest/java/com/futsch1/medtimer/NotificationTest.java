package com.futsch1.medtimer;


import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withClassName;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;

import androidx.test.espresso.ViewInteraction;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.filters.LargeTest;
import androidx.test.rule.GrantPermissionRule;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.Direction;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.Until;

import org.junit.Rule;
import org.junit.Test;

@LargeTest
public class NotificationTest {

    @Rule
    public ActivityScenarioRule<MainActivity> mActivityScenarioRule =
            new ActivityScenarioRule<>(MainActivity.class);

    @Rule
    public GrantPermissionRule mGrantPermissionRule =
            GrantPermissionRule.grant(
                    "android.permission.POST_NOTIFICATIONS");

    @Test
    public void notificationTest() {
        AndroidTestHelper.navigateTo(AndroidTestHelper.MainMenu.MEDICINES);

        onView(withId(R.id.addMedicine)).perform(click());

        ViewInteraction textInputEditText = onView(
                allOf(AndroidTestHelper.childAtPosition(
                                AndroidTestHelper.childAtPosition(
                                        withClassName(is("com.google.android.material.textfield.TextInputLayout")),
                                        0),
                                0),
                        isDisplayed()));
        textInputEditText.perform(replaceText("Test med"), closeSoftKeyboard());
        onView(allOf(withId(android.R.id.button1), withText("OK"))).perform(scrollTo(), click());

        AndroidTestHelper.createReminder("1", AndroidTestHelper.getNextNotificationTime().toLocalTime());

        onView(withId(R.id.open_advanced_settings)).perform(click());

        onView(withId(R.id.addLinkedReminder)).perform(click());
        onView(allOf(withId(android.R.id.button1), withText("OK"))).perform(scrollTo(), click());
        AndroidTestHelper.setTime(0, 1);

        AndroidTestHelper.navigateTo(AndroidTestHelper.MainMenu.OVERVIEW);

        mActivityScenarioRule.getScenario().close();

        UiDevice device = UiDevice.getInstance(getInstrumentation());
        device.openNotification();
        UiObject2 object = device.wait(Until.findObject(By.textContains("Test med")), 180_000);
        assertNotNull(object);
        object.fling(Direction.RIGHT);
        object = device.wait(Until.findObject(By.textContains("Test med")), 180_000);
        assertNotNull(object);
        device.pressBack();
    }

}
