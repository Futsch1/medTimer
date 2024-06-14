package com.futsch1.medtimer;


import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.action.ViewActions.swipeLeft;
import static androidx.test.espresso.action.ViewActions.swipeRight;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withClassName;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.futsch1.medtimer.AndroidTestHelper.childAtPosition;
import static com.futsch1.medtimer.AndroidTestHelper.setTime;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;

import androidx.test.espresso.NoMatchingViewException;
import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.rule.GrantPermissionRule;

import com.evrencoskun.tableview.TableView;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.atomic.AtomicReference;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class StatisticsTest {

    @Rule
    public ActivityScenarioRule<MainActivity> mActivityScenarioRule =
            new ActivityScenarioRule<>(MainActivity.class);

    @Rule
    public GrantPermissionRule mGrantPermissionRule =
            GrantPermissionRule.grant(
                    "android.permission.POST_NOTIFICATIONS");

    @Test
    public void statisticsTest() {
        onView(allOf(withContentDescription("More options"))).perform(click());

        onView(allOf(withId(androidx.recyclerview.R.id.title), withText("Generate test data"))).perform(click());
        onView(isRoot()).perform(AndroidTestHelper.waitFor(2000));

        setAllRemindersTo12AM();

        onView(new RecyclerViewMatcher(R.id.latestReminders).atPositionOnView(0, R.id.chipTaken)).perform(click());
        onView(new RecyclerViewMatcher(R.id.latestReminders).atPositionOnView(1, R.id.chipTaken)).perform(click());
        onView(new RecyclerViewMatcher(R.id.latestReminders).atPositionOnView(2, R.id.chipSkipped)).perform(click());
        onView(new RecyclerViewMatcher(R.id.latestReminders).atPositionOnView(3, R.id.chipTaken)).perform(scrollTo(), click());

        AndroidTestHelper.navigateTo(AndroidTestHelper.MainMenu.ANALYSIS);

        onView(withId(R.id.timeSpinner)).perform(click());

        onData(anything())
                .inAdapterView(childAtPosition(
                        withClassName(is("android.widget.PopupWindow$PopupBackgroundView")),
                        0))
                .atPosition(1).perform(click());

        onView(allOf(withId(R.id.reminderTableButton), withText("Tabular view"))).perform(click());

        onView(
                allOf(withId(R.id.tableColumnHeaderContainer),
                        childAtPosition(
                                allOf(withId(com.evrencoskun.tableview.R.id.ColumnHeaderRecyclerView),
                                        childAtPosition(
                                                withId(R.id.reminder_table),
                                                0)),
                                1),
                        isDisplayed())).perform(click());

        AtomicReference<TableView> tableView = new AtomicReference<>();
        mActivityScenarioRule.getScenario().onActivity(activity -> tableView.set(activity.findViewById(R.id.reminder_table)));
        int tableCellRecyclerViewId = tableView.get().getCellRecyclerView().getId();

        onView(new RecyclerViewMatcher(tableCellRecyclerViewId).atPositionOnView(0, "medicineName"))
                .check(matches(withText(startsWith("Selen (200 µg)"))));

        onView(withId(R.id.filter)).perform(replaceText("B"), closeSoftKeyboard());

        onView(new RecyclerViewMatcher(tableCellRecyclerViewId).atPositionOnView(0, "medicineName"))
                .check(matches(withText("B12 (500µg)")));

        onView(withId(com.google.android.material.R.id.text_input_end_icon)).perform(click());

        AndroidTestHelper.navigateTo(AndroidTestHelper.MainMenu.OVERVIEW);

        // Edit most recent reminder event
        onView(withId(R.id.latestReminders)).perform(RecyclerViewActions.actionOnItemAtPosition(0, swipeRight()));
        onView(withId(R.id.editEventName)).perform(replaceText("TestMedicine"));
        onView(withId(R.id.editEventAmount)).perform(replaceText("Much"));
        onView(allOf(withContentDescription("Navigate up"))).perform(click());

        onView(new RecyclerViewMatcher(R.id.latestReminders).atPositionOnView(0, R.id.reminderEventText, null))
                .check(matches(withText(startsWith("Much of TestMedicine"))));

        // And now delete it
        onView(withId(R.id.latestReminders)).perform(RecyclerViewActions.actionOnItemAtPosition(0, swipeLeft()));
        onView(allOf(withId(android.R.id.button1), withText("YES"))).perform(click());

        onView(new RecyclerViewMatcher(R.id.latestReminders).atPositionOnView(0, R.id.reminderEventText, null))
                .check(matches(withText(not(startsWith("Much of TestMedicine")))));
    }

    private void setAllRemindersTo12AM() {
        AndroidTestHelper.navigateTo(AndroidTestHelper.MainMenu.MEDICINES);

        setReminderTo12AM(0);
        setReminderTo12AM(1);
        setReminderTo12AM(2);
        setReminderTo12AM(3);

        AndroidTestHelper.navigateTo(AndroidTestHelper.MainMenu.OVERVIEW);
    }

    private void setReminderTo12AM(int position) {
        onView(new RecyclerViewMatcher(R.id.medicineList).atPositionOnView(position, R.id.medicineCard)).perform(click());
        try {
            onView(new RecyclerViewMatcher(R.id.medicineList).atPositionOnView(position, R.id.medicineCard)).perform(click());
        } catch (NoMatchingViewException ignored) {

        }

        onView(new RecyclerViewMatcher(R.id.reminderList).atPositionOnView(0, R.id.editReminderTime)).perform(click());

        setTime(0, 0);
        onView(allOf(withContentDescription("Navigate up"))).perform(click());
    }

}
