package com.futsch1.medtimer;


import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withClassName;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static com.futsch1.medtimer.AndroidTestHelper.childAtPosition;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;

import androidx.test.espresso.DataInteraction;
import androidx.test.espresso.ViewInteraction;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.filters.LargeTest;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.Until;

import org.junit.Rule;
import org.junit.Test;

@LargeTest
public class TestDataAndDeleteAndManualDoseTest extends BaseTestHelper {

    @Rule
    public ActivityScenarioRule<MainActivity> mActivityScenarioRule =
            new ActivityScenarioRule<>(MainActivity.class);

    @Test
    public void testDataAndDeleteAndManualDoseTest() {
        UiDevice device = UiDevice.getInstance(getInstrumentation());
        device.wait(Until.findObject(By.desc("More options")), 1000);
        openActionBarOverflowOrOptionsMenu(getInstrumentation().getTargetContext());
        onView(withText(R.string.generate_test_data)).perform(click());

        AndroidTestHelper.navigateTo(AndroidTestHelper.MainMenu.MEDICINES);

        onView(withId(R.id.medicineList)).perform(actionOnItemAtPosition(0, click()));

        openActionBarOverflowOrOptionsMenu(getInstrumentation().getTargetContext());
        onView(withText(R.string.delete)).perform(click());
        onView(withText(R.string.yes)).perform(scrollTo(), click());

        onView(withId(R.id.medicineList)).perform(actionOnItemAtPosition(2, click()));
        onView(new RecyclerViewMatcher(R.id.reminderList).atPositionOnView(1, R.id.open_advanced_settings)).perform(click());
        openActionBarOverflowOrOptionsMenu(getInstrumentation().getTargetContext());
        onView(withText(R.string.delete)).perform(click());
        onView(allOf(withId(android.R.id.button1), withText(R.string.yes))).perform(scrollTo(), click());

        pressBack();

        AndroidTestHelper.navigateTo(AndroidTestHelper.MainMenu.OVERVIEW);

        onView(allOf(withId(R.id.logManualDose), withText(R.string.log_additional_dose), isDisplayed()))
                .perform(click());

        DataInteraction materialTextView5 = onData(anything())
                .inAdapterView(allOf(withClassName(is("com.android.internal.app.AlertController$RecycleListView")),
                        childAtPosition(
                                withClassName(is("android.widget.FrameLayout")),
                                0)))
                .atPosition(2);
        materialTextView5.perform(click());

        ViewInteraction textInputEditText = onView(
                allOf(childAtPosition(
                                childAtPosition(
                                        withClassName(is("com.google.android.material.textfield.TextInputLayout")),
                                        0),
                                0),
                        isDisplayed()));
        textInputEditText.perform(replaceText("1"), closeSoftKeyboard());

        onView(allOf(withId(android.R.id.button1), withText("OK"))).perform(scrollTo(), click());

        ViewInteraction materialButton5 = onView(
                allOf(withId(com.google.android.material.R.id.material_timepicker_ok_button), withText("OK"),
                        childAtPosition(
                                childAtPosition(
                                        withId(android.R.id.content),
                                        0),
                                5),
                        isDisplayed()));
        materialButton5.perform(click());

        String expectedText = getInstrumentation().getTargetContext().getString(R.string.reminder_event, "1", "Ginseng (200mg)", "");
        onView(new RecyclerViewMatcher(R.id.latestReminders).atPositionOnView(0, R.id.reminderEventText))
                .check(matches(withText(startsWith(expectedText))));
    }

}
