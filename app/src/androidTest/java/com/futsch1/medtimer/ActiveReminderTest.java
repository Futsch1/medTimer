package com.futsch1.medtimer;


import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.filters.LargeTest;
import androidx.test.rule.GrantPermissionRule;

import org.junit.Rule;
import org.junit.Test;

import java.util.Calendar;

@LargeTest
public class ActiveReminderTest {

    @Rule
    public ActivityScenarioRule<MainActivity> mActivityScenarioRule =
            new ActivityScenarioRule<>(MainActivity.class);

    @Rule
    public GrantPermissionRule mGrantPermissionRule =
            GrantPermissionRule.grant(
                    "android.permission.POST_NOTIFICATIONS");

    @Test
    public void activeReminderTest() {
        Calendar futureTime = Calendar.getInstance();
        int year = futureTime.get(Calendar.YEAR);
        futureTime.set(year + 1, 1, 1);
        Calendar pastTime = Calendar.getInstance();
        pastTime.set(year - 1, 1, 1);

        AndroidTestHelper.createMedicine("Test");
        AndroidTestHelper.createReminder("1", null);

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
        onView(withId(R.id.periodStartDate)).perform(replaceText(AndroidTestHelper.dateToString(futureTime.getTime())));

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
        onView(withId(R.id.periodEndDate)).perform(replaceText(AndroidTestHelper.dateToString(pastTime.getTime())));

        pressBack();
        pressBack();
        AndroidTestHelper.navigateTo(AndroidTestHelper.MainMenu.OVERVIEW);
        onView(isRoot()).perform(AndroidTestHelper.waitFor(1000));
        onView(new RecyclerViewMatcher(R.id.nextReminders).sizeMatcher(0)).check(matches(isEnabled()));

        AndroidTestHelper.navigateTo(AndroidTestHelper.MainMenu.MEDICINES);
        onView(withId(R.id.medicineList)).perform(actionOnItemAtPosition(0, click()));
        onView(withId(R.id.open_advanced_settings)).perform(click());
        onView(withId(R.id.periodStart)).perform(click());
        onView(withId(R.id.periodStartDate)).perform(replaceText(AndroidTestHelper.dateToString(pastTime.getTime())));
        onView(withId(R.id.periodEndDate)).perform(replaceText(AndroidTestHelper.dateToString(futureTime.getTime())));

        pressBack();
        pressBack();
        AndroidTestHelper.navigateTo(AndroidTestHelper.MainMenu.OVERVIEW);
        onView(isRoot()).perform(AndroidTestHelper.waitFor(1000));
        onView(new RecyclerViewMatcher(R.id.nextReminders).sizeMatcher(1)).check(matches(isEnabled()));
    }

}
