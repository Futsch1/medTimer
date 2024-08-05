package com.futsch1.medtimer;


import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withClassName;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;

import androidx.test.espresso.ViewInteraction;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.rule.GrantPermissionRule;

import com.futsch1.medtimer.database.MedicineRepository;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class ActiveReminderTest {

    @Rule
    public ActivityScenarioRule<MainActivity> mActivityScenarioRule =
            new ActivityScenarioRule<>(MainActivity.class);

    @Rule
    public GrantPermissionRule mGrantPermissionRule =
            GrantPermissionRule.grant(
                    "android.permission.POST_NOTIFICATIONS");

    @Test
    public void basicUITest() {
        mActivityScenarioRule.getScenario().onActivity(activity -> {
            MedicineRepository repository = new MedicineRepository(activity.getApplication());
            repository.deleteAll();
        });
        onView(isRoot()).perform(AndroidTestHelper.waitFor(1000));

        AndroidTestHelper.navigateTo(AndroidTestHelper.MainMenu.MEDICINES);

        onView(withId(R.id.addMedicine)).perform(click());

        ViewInteraction textInputEditText = onView(
                allOf(AndroidTestHelper.childAtPosition(
                                AndroidTestHelper.childAtPosition(
                                        withClassName(is("com.google.android.material.textfield.TextInputLayout")),
                                        0),
                                0),
                        isDisplayed()));
        textInputEditText.perform(replaceText("Test"), closeSoftKeyboard());

        onView(allOf(withId(android.R.id.button1), withText("OK"))).perform(scrollTo(), click());

        onView(withId(R.id.medicineList)).perform(actionOnItemAtPosition(0, click()));
        onView(withId(R.id.addReminder)).perform(click());

        ViewInteraction textInputEditText2 = onView(
                allOf(AndroidTestHelper.childAtPosition(
                                AndroidTestHelper.childAtPosition(
                                        withClassName(is("com.google.android.material.textfield.TextInputLayout")),
                                        0),
                                0),
                        isDisplayed()));
        textInputEditText2.perform(replaceText("1"), closeSoftKeyboard());

        onView(allOf(withId(android.R.id.button1), withText("OK"))).perform(scrollTo(), click());

        onView(allOf(withId(com.google.android.material.R.id.material_timepicker_ok_button), withText("OK"))).perform(click());

        onView(isRoot()).perform(AndroidTestHelper.waitFor(1000));
        onView(withId(R.id.open_advanced_settings)).perform(click());

        onView(withId(R.id.inactive)).perform(click());

        pressBack();
        pressBack();
        AndroidTestHelper.navigateTo(AndroidTestHelper.MainMenu.OVERVIEW);
        onView(isRoot()).perform(AndroidTestHelper.waitFor(1000));
        onView(new RecyclerViewMatcher(R.id.nextReminders).sizeMatcher(0)).check(matches(isEnabled()));

        AndroidTestHelper.navigateTo(AndroidTestHelper.MainMenu.MEDICINES);
        onView(withId(R.id.medicineList)).perform(actionOnItemAtPosition(0, click()));
        onView(withId(R.id.open_advanced_settings)).perform(click());
        onView(withId(R.id.active)).perform(click());

        pressBack();
        pressBack();
        AndroidTestHelper.navigateTo(AndroidTestHelper.MainMenu.OVERVIEW);
        onView(isRoot()).perform(AndroidTestHelper.waitFor(1000));
        onView(new RecyclerViewMatcher(R.id.nextReminders).sizeMatcher(1)).check(matches(isEnabled()));

        AndroidTestHelper.navigateTo(AndroidTestHelper.MainMenu.MEDICINES);
        onView(withId(R.id.medicineList)).perform(actionOnItemAtPosition(0, click()));
        onView(withId(R.id.open_advanced_settings)).perform(click());
        onView(withId(R.id.timePeriod)).perform(click());
        onView(withId(R.id.periodStart)).perform(click());
        onView(withId(R.id.periodStartDate)).perform(replaceText("1/1/30"));

        pressBack();
        pressBack();
        AndroidTestHelper.navigateTo(AndroidTestHelper.MainMenu.OVERVIEW);
        onView(isRoot()).perform(AndroidTestHelper.waitFor(1000));
        onView(new RecyclerViewMatcher(R.id.nextReminders).sizeMatcher(0)).check(matches(isEnabled()));

        AndroidTestHelper.navigateTo(AndroidTestHelper.MainMenu.MEDICINES);
        onView(withId(R.id.medicineList)).perform(actionOnItemAtPosition(0, click()));
        onView(withId(R.id.open_advanced_settings)).perform(click());
        onView(withId(R.id.periodStart)).perform(click());
        onView(withId(R.id.periodEnd)).perform(click());
        onView(withId(R.id.periodEndDate)).perform(replaceText("1/1/23"));

        pressBack();
        pressBack();
        AndroidTestHelper.navigateTo(AndroidTestHelper.MainMenu.OVERVIEW);
        onView(isRoot()).perform(AndroidTestHelper.waitFor(1000));
        onView(new RecyclerViewMatcher(R.id.nextReminders).sizeMatcher(0)).check(matches(isEnabled()));

        AndroidTestHelper.navigateTo(AndroidTestHelper.MainMenu.MEDICINES);
        onView(withId(R.id.medicineList)).perform(actionOnItemAtPosition(0, click()));
        onView(withId(R.id.open_advanced_settings)).perform(click());
        onView(withId(R.id.periodStart)).perform(click());
        onView(withId(R.id.periodStartDate)).perform(replaceText("1/1/23"));
        onView(withId(R.id.periodEndDate)).perform(replaceText("1/1/30"));

        pressBack();
        pressBack();
        AndroidTestHelper.navigateTo(AndroidTestHelper.MainMenu.OVERVIEW);
        onView(isRoot()).perform(AndroidTestHelper.waitFor(1000));
        onView(new RecyclerViewMatcher(R.id.nextReminders).sizeMatcher(1)).check(matches(isEnabled()));
    }

}
