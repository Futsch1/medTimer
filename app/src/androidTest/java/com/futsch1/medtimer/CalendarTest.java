package com.futsch1.medtimer;


import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withSubstring;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.rule.GrantPermissionRule;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class CalendarTest {

    @Rule
    public ActivityScenarioRule<MainActivity> mActivityScenarioRule =
            new ActivityScenarioRule<>(MainActivity.class);

    @Rule
    public GrantPermissionRule mGrantPermissionRule =
            GrantPermissionRule.grant(
                    "android.permission.POST_NOTIFICATIONS");

    @Test
    public void calendarTest() {
        onView(allOf(withContentDescription("More options"))).perform(click());

        onView(withText("Generate test data")).perform(click());
        onView(isRoot()).perform(AndroidTestHelper.waitFor(2000));

        AndroidTestHelper.setAllRemindersTo12AM();

        onView(allOf(withId(R.id.showOnlyOpen), isDisplayed())).perform(click());

        int takenReminders = 0;

        while (takenReminders < 10) {
            try {
                onView(new RecyclerViewMatcher(R.id.latestReminders).atPositionOnView(0, R.id.chipTaken)).perform(click());
            } catch (Exception e) {
                break;
            }
            takenReminders++;
        }

        onView(new RecyclerViewMatcher(R.id.latestReminders).sizeMatcher(0)).check(matches(isDisplayed()));

        AndroidTestHelper.navigateTo(AndroidTestHelper.MainMenu.MEDICINES);

        onView(new RecyclerViewMatcher(R.id.medicineList).atPositionOnView(0, R.id.medicineCard)).perform(click());

        onView(allOf(withId(R.id.openCalendar), isDisplayed())).perform(click());

        onView(allOf(withId(R.id.currentDayEvents), isDisplayed())).check(matches(withSubstring("Omega 3 (EPA/DHA 500mg)")));

        onView(allOf(withContentDescription("Navigate up"))).perform(click());

        AndroidTestHelper.navigateTo(AndroidTestHelper.MainMenu.ANALYSIS);

        onView(allOf(withId(R.id.reminderCalendarButton), isDisplayed())).perform(click());

        onView(allOf(withId(R.id.currentDayEvents), isDisplayed())).check(matches(withSubstring("Selen (200 µg)")));
    }
}
