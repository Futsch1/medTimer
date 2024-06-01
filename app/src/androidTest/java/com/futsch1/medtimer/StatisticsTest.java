package com.futsch1.medtimer;


import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withClassName;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertNotEquals;

import androidx.test.espresso.DataInteraction;
import androidx.test.espresso.NoMatchingViewException;
import androidx.test.espresso.ViewInteraction;
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
        int retries = 3;
        while (retries > 0) {
            ViewInteraction overflowMenuButton = onView(
                    allOf(withContentDescription("More options")));
            overflowMenuButton.perform(click());

            ViewInteraction materialTextView = onView(
                    allOf(withId(androidx.recyclerview.R.id.title), withText("Generate test data")));
            materialTextView.perform(click());
            onView(isRoot()).perform(AndroidTestHelper.waitFor(10000));

            try {
                ViewInteraction chip = onView(
                        new RecyclerViewMatcher(R.id.latestReminders).atPositionOnView(0, R.id.chipTaken));
                chip.perform(click());

                ViewInteraction chip2 = onView(
                        new RecyclerViewMatcher(R.id.latestReminders).atPositionOnView(1, R.id.chipTaken));
                chip2.perform(click());

                ViewInteraction chip3 = onView(
                        new RecyclerViewMatcher(R.id.latestReminders).atPositionOnView(2, R.id.chipSkipped));
                chip3.perform(click());

                ViewInteraction chip4 = onView(
                        new RecyclerViewMatcher(R.id.latestReminders).atPositionOnView(3, R.id.chipTaken));
                chip4.perform(scrollTo(), click());

                break;
            } catch (NoMatchingViewException e) {
                retries--;
            }
        }
        assertNotEquals(0, retries);

        AndroidTestHelper.navigateTo(AndroidTestHelper.MainMenu.ANALYSIS);

        ViewInteraction appCompatSpinner = onView(
                allOf(withId(R.id.timeSpinner)));
        appCompatSpinner.perform(click());

        DataInteraction materialTextView2 = onData(anything())
                .inAdapterView(AndroidTestHelper.childAtPosition(
                        withClassName(is("android.widget.PopupWindow$PopupBackgroundView")),
                        0))
                .atPosition(1);
        materialTextView2.perform(click());

        ViewInteraction materialButton = onView(
                allOf(withId(R.id.reminderTableButton), withText("Tabular view")));
        materialButton.perform(click());

        ViewInteraction linearLayout = onView(
                allOf(withId(R.id.tableColumnHeaderContainer),
                        AndroidTestHelper.childAtPosition(
                                allOf(withId(com.evrencoskun.tableview.R.id.ColumnHeaderRecyclerView),
                                        AndroidTestHelper.childAtPosition(
                                                withId(R.id.reminder_table),
                                                0)),
                                1),
                        isDisplayed()));
        linearLayout.perform(click());

        AtomicReference<TableView> tableView = new AtomicReference<>();
        mActivityScenarioRule.getScenario().onActivity(activity -> tableView.set(activity.findViewById(R.id.reminder_table)));
        int tableCellRecyclerViewId = tableView.get().getCellRecyclerView().getId();

        ViewInteraction textView = onView(
                new RecyclerViewMatcher(tableCellRecyclerViewId).atPositionOnView(0, "medicineName"));
        textView.check(matches(withText(startsWith("Selen (200 µg)"))));

        ViewInteraction textInputEditText = onView(
                allOf(withId(R.id.filter)));
        textInputEditText.perform(replaceText("B"), closeSoftKeyboard());

        ViewInteraction textView3 = onView(
                new RecyclerViewMatcher(tableCellRecyclerViewId).atPositionOnView(0, "medicineName"));
        textView3.check(matches(withText("B12 (500µg)")));

        ViewInteraction checkableImageButton = onView(
                allOf(withId(com.google.android.material.R.id.text_input_end_icon)));
        checkableImageButton.perform(click());
    }

}
