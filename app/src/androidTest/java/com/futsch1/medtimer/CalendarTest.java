package com.futsch1.medtimer;


import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withSubstring;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static com.futsch1.medtimer.AndroidTestHelper.onViewWithTimeout;
import static org.hamcrest.Matchers.allOf;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.filters.LargeTest;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.Until;

import org.junit.Rule;
import org.junit.Test;

@LargeTest
public class CalendarTest extends BaseHelper {

    @Rule
    public ActivityScenarioRule<MainActivity> mActivityScenarioRule =
            new ActivityScenarioRule<>(MainActivity.class);

    @Test
    public void calendarTest() {
        UiDevice device = UiDevice.getInstance(getInstrumentation());
        device.wait(Until.findObject(By.desc("More options")), 1000);
        openActionBarOverflowOrOptionsMenu(getInstrumentation().getTargetContext());
        onView(withText(R.string.generate_test_data)).perform(click());

        onView(isRoot()).perform(AndroidTestHelper.waitFor(2000));

        AndroidTestHelper.setAllRemindersTo12AM();

        onView(allOf(withId(R.id.showOnlyOpen), isDisplayed())).perform(click());

        int takenReminders = 0;

        while (takenReminders < 10) {
            try {
                onView(new RecyclerViewMatcher(R.id.latestReminders).atPositionOnView(0, R.id.chipTaken)).perform(click());
                onView(isRoot()).perform(AndroidTestHelper.waitFor(1000));
            } catch (Exception e) {
                break;
            }
            takenReminders++;
        }

        onViewWithTimeout(new RecyclerViewMatcher(R.id.latestReminders).sizeMatcher(0)).check(matches(isDisplayed()));

        AndroidTestHelper.navigateTo(AndroidTestHelper.MainMenu.MEDICINES);

        onViewWithTimeout(new RecyclerViewMatcher(R.id.medicineList).atPositionOnView(0, R.id.medicineCard)).perform(click());

        onView(allOf(withId(R.id.openCalendar), isDisplayed())).perform(click());

        onViewWithTimeout(allOf(withId(R.id.currentDayEvents), isDisplayed())).check(matches(withSubstring("Omega 3 (EPA/DHA 500mg)")));

        pressBack();

        AndroidTestHelper.navigateTo(AndroidTestHelper.MainMenu.ANALYSIS);

        onView(allOf(withId(R.id.calendarChip), isDisplayed())).perform(click());

        onViewWithTimeout(allOf(withId(R.id.currentDayEvents), isDisplayed())).check(matches(withSubstring("Selen (200 µg)")));
    }
}
