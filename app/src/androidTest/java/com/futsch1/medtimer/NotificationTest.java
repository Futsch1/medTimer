package com.futsch1.medtimer;


import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition;
import static androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withClassName;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withInputType;
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
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.Until;

import com.futsch1.medtimer.database.MedicineRepository;

import org.junit.Rule;
import org.junit.Test;

import java.time.LocalDateTime;

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
        mActivityScenarioRule.getScenario().onActivity(activity -> {
            MedicineRepository repository = new MedicineRepository(activity.getApplication());
            repository.deleteAll();
        });
        onView(isRoot()).perform(AndroidTestHelper.waitFor(1000));

        AndroidTestHelper.navigateTo(AndroidTestHelper.MainMenu.MEDICINES);

        ViewInteraction extendedFloatingActionButton = onView(
                withId(R.id.addMedicine));
        extendedFloatingActionButton.perform(click());

        ViewInteraction textInputEditText = onView(
                allOf(AndroidTestHelper.childAtPosition(
                                AndroidTestHelper.childAtPosition(
                                        withClassName(is("com.google.android.material.textfield.TextInputLayout")),
                                        0),
                                0),
                        isDisplayed()));
        textInputEditText.perform(replaceText("Test med"), closeSoftKeyboard());

        ViewInteraction materialButton = onView(
                allOf(withId(android.R.id.button1), withText("OK")));
        materialButton.perform(scrollTo(), click());

        ViewInteraction recyclerView = onView(
                allOf(withId(R.id.medicineList)));
        recyclerView.perform(actionOnItemAtPosition(0, click()));

        ViewInteraction extendedFloatingActionButton2 = onView(
                allOf(withId(R.id.addReminder)));
        extendedFloatingActionButton2.perform(click());

        ViewInteraction textInputEditText2 = onView(
                allOf(AndroidTestHelper.childAtPosition(
                                AndroidTestHelper.childAtPosition(
                                        withClassName(is("com.google.android.material.textfield.TextInputLayout")),
                                        0),
                                0),
                        isDisplayed()));
        textInputEditText2.perform(replaceText("1"), closeSoftKeyboard());

        ViewInteraction materialButton2 = onView(
                allOf(withId(android.R.id.button1), withText("OK")));
        materialButton2.perform(scrollTo(), click());

        LocalDateTime notificationTime = AndroidTestHelper.getNextNotificationTime();
        ViewInteraction mode = onView(withId(com.google.android.material.R.id.material_timepicker_mode_button));
        mode.perform(click());
        ViewInteraction hours = onView(allOf(withInputType(2),
                isDescendantOfA(withId(com.google.android.material.R.id.material_hour_text_input))));
        if (notificationTime.getHour() > 12) {
            onView(AndroidTestHelper.withIndex(withId(com.google.android.material.R.id.material_clock_period_pm_button), 1)).perform(click());
            notificationTime = notificationTime.minusHours(12);
        }
        if (notificationTime.getHour() == 0) {
            notificationTime = notificationTime.plusHours(12);
        }
        hours.perform(replaceText(String.valueOf(notificationTime.getHour())), closeSoftKeyboard());
        onView(withId(com.google.android.material.R.id.material_minute_text_input)).perform(click());
        ViewInteraction minutes = onView(allOf(withInputType(2),
                isDescendantOfA(withId(com.google.android.material.R.id.material_minute_text_input))));
        minutes.perform(replaceText(String.valueOf(notificationTime.getMinute())), closeSoftKeyboard());
        ViewInteraction materialButton3 = onView(
                allOf(withId(com.google.android.material.R.id.material_timepicker_ok_button), withText("OK")));
        materialButton3.perform(click());

        pressBack();
        AndroidTestHelper.navigateTo(AndroidTestHelper.MainMenu.OVERVIEW);

        mActivityScenarioRule.getScenario().close();

        UiDevice device = UiDevice.getInstance(getInstrumentation());
        device.openNotification();
        UiObject2 object = device.wait(Until.findObject(By.textContains("Test med")), 180_000);
        device.pressBack();
        assertNotNull(object);
    }

}
