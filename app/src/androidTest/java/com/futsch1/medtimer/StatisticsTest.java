package com.futsch1.medtimer;


import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
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

import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import androidx.test.espresso.DataInteraction;
import androidx.test.espresso.ViewInteraction;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.rule.GrantPermissionRule;

import com.evrencoskun.tableview.TableView;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
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
        ViewInteraction overflowMenuButton = onView(
                allOf(withContentDescription("More options"),
                        childAtPosition(
                                childAtPosition(
                                        withId(androidx.appcompat.R.id.action_bar),
                                        1),
                                0),
                        isDisplayed()));
        overflowMenuButton.perform(click());

        ViewInteraction materialTextView = onView(
                allOf(withId(androidx.recyclerview.R.id.title), withText("Generate test data"),
                        childAtPosition(
                                childAtPosition(
                                        withId(androidx.appcompat.R.id.content),
                                        0),
                                0),
                        isDisplayed()));
        materialTextView.perform(click());
        onView(isRoot()).perform(AndroidTestHelper.waitFor(4000));

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
        chip4.perform(click());

        ViewInteraction tabView = onView(
                allOf(withContentDescription("Analysis"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.tabs),
                                        0),
                                2),
                        isDisplayed()));
        tabView.perform(click());

        ViewInteraction appCompatSpinner = onView(
                allOf(withId(R.id.timeSpinner),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        0),
                                2),
                        isDisplayed()));
        appCompatSpinner.perform(click());

        DataInteraction materialTextView2 = onData(anything())
                .inAdapterView(childAtPosition(
                        withClassName(is("android.widget.PopupWindow$PopupBackgroundView")),
                        0))
                .atPosition(1);
        materialTextView2.perform(click());

        ViewInteraction materialButton = onView(
                allOf(withId(R.id.reminderTableButton), withText("Tabular view"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        0),
                                0),
                        isDisplayed()));
        materialButton.perform(click());

        ViewInteraction linearLayout = onView(
                allOf(withId(R.id.tableColumnHeaderContainer),
                        childAtPosition(
                                allOf(withId(com.evrencoskun.tableview.R.id.ColumnHeaderRecyclerView),
                                        childAtPosition(
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
                allOf(withId(R.id.filter),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.filterLayout),
                                        0),
                                0),
                        isDisplayed()));
        textInputEditText.perform(replaceText("B"), closeSoftKeyboard());

        ViewInteraction textView3 = onView(
                new RecyclerViewMatcher(tableCellRecyclerViewId).atPositionOnView(0, "medicineName"));
        textView3.check(matches(withText("B12 (500µg)")));

        ViewInteraction checkableImageButton = onView(
                allOf(withId(com.google.android.material.R.id.text_input_end_icon),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("com.google.android.material.textfield.EndCompoundLayout")),
                                        1),
                                0),
                        isDisplayed()));
        checkableImageButton.perform(click());
    }

    private static Matcher<View> childAtPosition(
            final Matcher<View> parentMatcher, final int position) {

        return new TypeSafeMatcher<>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("Child at position " + position + " in parent ");
                parentMatcher.describeTo(description);
            }

            @Override
            public boolean matchesSafely(View view) {
                ViewParent parent = view.getParent();
                return parent instanceof ViewGroup && parentMatcher.matches(parent)
                        && view.equals(((ViewGroup) parent).getChildAt(position));
            }
        };
    }
}
