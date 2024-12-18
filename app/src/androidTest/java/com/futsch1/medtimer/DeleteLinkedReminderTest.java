package com.futsch1.medtimer;


import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static org.hamcrest.Matchers.allOf;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.filters.LargeTest;
import androidx.test.rule.GrantPermissionRule;

import org.junit.Rule;
import org.junit.Test;

import java.time.LocalTime;

@LargeTest
public class DeleteLinkedReminderTest {

    @Rule
    public ActivityScenarioRule<MainActivity> mActivityScenarioRule =
            new ActivityScenarioRule<>(MainActivity.class);

    @Rule
    public GrantPermissionRule mGrantPermissionRule =
            GrantPermissionRule.grant(
                    "android.permission.POST_NOTIFICATIONS");

    @Test
    public void deleteLinkedReminderTest() {
        AndroidTestHelper.createMedicine("Test med");
        AndroidTestHelper.createReminder("1", LocalTime.of(0, 0));

        onView(withId(R.id.open_advanced_settings)).perform(click());

        onView(withId(R.id.addLinkedReminder)).perform(click());
        onView(allOf(withId(android.R.id.button1), withText("OK"))).perform(scrollTo(), click());
        AndroidTestHelper.setTime(0, 1);

        onView(new RecyclerViewMatcher(R.id.reminderList).atPositionOnView(1, R.id.open_advanced_settings)).perform(click());

        onView(withId(R.id.addLinkedReminder)).perform(click());
        onView(allOf(withId(android.R.id.button1), withText("OK"))).perform(scrollTo(), click());
        AndroidTestHelper.setTime(0, 2);

        onView(new RecyclerViewMatcher(R.id.reminderList).atPositionOnView(0, R.id.open_advanced_settings)).perform(click());

        openActionBarOverflowOrOptionsMenu(getInstrumentation().getTargetContext());
        onView(withText(R.string.delete)).perform(click());
        onView(allOf(withId(android.R.id.button1), withText(R.string.yes))).perform(scrollTo(), click());

        // Check that the reminder list is empty
        onView(new RecyclerViewMatcher(R.id.reminderList).sizeMatcher(0)).check(matches(isDisplayed()));

    }

}
