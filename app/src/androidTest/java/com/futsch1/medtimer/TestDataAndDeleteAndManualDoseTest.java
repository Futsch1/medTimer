package com.futsch1.medtimer;


import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.longClick;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition;
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

import androidx.test.espresso.DataInteraction;
import androidx.test.espresso.ViewInteraction;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.rule.GrantPermissionRule;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class TestDataAndDeleteAndManualDoseTest {

    @Rule
    public ActivityScenarioRule<MainActivity> mActivityScenarioRule =
            new ActivityScenarioRule<>(MainActivity.class);

    @Rule
    public GrantPermissionRule mGrantPermissionRule =
            GrantPermissionRule.grant(
                    "android.permission.POST_NOTIFICATIONS");

    @Test
    public void testDataAndDeleteAndManualDoseTest() {
        ViewInteraction overflowMenuButton = onView(
                allOf(withContentDescription("More options"),
                        AndroidTestHelper.childAtPosition(
                                AndroidTestHelper.childAtPosition(
                                        withId(androidx.appcompat.R.id.action_bar),
                                        1),
                                0),
                        isDisplayed()));
        overflowMenuButton.perform(click());

        ViewInteraction materialTextView = onView(
                allOf(withId(androidx.recyclerview.R.id.title), withText("Settings"),
                        AndroidTestHelper.childAtPosition(
                                AndroidTestHelper.childAtPosition(
                                        withId(androidx.appcompat.R.id.content),
                                        1),
                                0),
                        isDisplayed()));
        materialTextView.perform(click());

        ViewInteraction recyclerView = onView(
                allOf(withId(com.takisoft.preferencex.ringtone.R.id.recycler_view),
                        AndroidTestHelper.childAtPosition(
                                withId(android.R.id.list_container),
                                0)));
        recyclerView.perform(actionOnItemAtPosition(7, click()));

        DataInteraction appCompatCheckedTextView = onData(anything())
                .inAdapterView(allOf(withId(androidx.appcompat.R.id.select_dialog_listview),
                        AndroidTestHelper.childAtPosition(
                                withId(androidx.appcompat.R.id.contentPanel),
                                0)))
                .atPosition(1);
        appCompatCheckedTextView.perform(click());

        ViewInteraction appCompatImageButton = onView(
                allOf(withContentDescription("Navigate up"),
                        AndroidTestHelper.childAtPosition(
                                allOf(withId(androidx.appcompat.R.id.action_bar),
                                        AndroidTestHelper.childAtPosition(
                                                withId(androidx.appcompat.R.id.action_bar_container),
                                                0)),
                                2),
                        isDisplayed()));
        appCompatImageButton.perform(click());

        ViewInteraction overflowMenuButton2 = onView(
                allOf(withContentDescription("More options"),
                        AndroidTestHelper.childAtPosition(
                                AndroidTestHelper.childAtPosition(
                                        withId(androidx.appcompat.R.id.action_bar),
                                        1),
                                0),
                        isDisplayed()));
        overflowMenuButton2.perform(click());

        ViewInteraction materialTextView2 = onView(
                allOf(withId(androidx.recyclerview.R.id.title), withText("Generate test data"),
                        AndroidTestHelper.childAtPosition(
                                AndroidTestHelper.childAtPosition(
                                        withId(androidx.appcompat.R.id.content),
                                        1),
                                0),
                        isDisplayed()));
        materialTextView2.perform(click());
        onView(isRoot()).perform(AndroidTestHelper.waitFor(2000));

        AndroidTestHelper.navigateTo(AndroidTestHelper.MainMenu.MEDICINES);

        ViewInteraction recyclerView2 = onView(
                allOf(withId(R.id.medicineList),
                        AndroidTestHelper.childAtPosition(
                                withClassName(is("android.widget.FrameLayout")),
                                0)));
        recyclerView2.perform(actionOnItemAtPosition(0, longClick()));

        ViewInteraction materialTextView3 = onView(
                allOf(withId(android.R.id.title), withText("Delete"),
                        AndroidTestHelper.childAtPosition(
                                AndroidTestHelper.childAtPosition(
                                        withId(android.R.id.content),
                                        0),
                                0),
                        isDisplayed()));
        materialTextView3.perform(click());

        ViewInteraction materialButton = onView(
                allOf(withId(android.R.id.button1), withText("Yes"),
                        AndroidTestHelper.childAtPosition(
                                AndroidTestHelper.childAtPosition(
                                        withClassName(is("android.widget.ScrollView")),
                                        0),
                                3)));
        materialButton.perform(scrollTo(), click());

        ViewInteraction recyclerView3 = onView(
                allOf(withId(R.id.medicineList),
                        AndroidTestHelper.childAtPosition(
                                withClassName(is("android.widget.FrameLayout")),
                                0)));
        recyclerView3.perform(actionOnItemAtPosition(2, click()));

        ViewInteraction recyclerView4 = onView(
                allOf(withId(R.id.reminderList),
                        AndroidTestHelper.childAtPosition(
                                withClassName(is("android.widget.LinearLayout")),
                                2)));
        recyclerView4.perform(actionOnItemAtPosition(1, longClick()));

        ViewInteraction materialTextView4 = onView(
                allOf(withId(android.R.id.title), withText("Delete"),
                        AndroidTestHelper.childAtPosition(
                                AndroidTestHelper.childAtPosition(
                                        withId(android.R.id.content),
                                        0),
                                0),
                        isDisplayed()));
        materialTextView4.perform(click());

        ViewInteraction materialButton2 = onView(
                allOf(withId(android.R.id.button1), withText("Yes"),
                        AndroidTestHelper.childAtPosition(
                                AndroidTestHelper.childAtPosition(
                                        withClassName(is("android.widget.ScrollView")),
                                        0),
                                3)));
        materialButton2.perform(scrollTo(), click());

        ViewInteraction appCompatImageButton2 = onView(
                allOf(withContentDescription("Navigate up"),
                        AndroidTestHelper.childAtPosition(
                                allOf(withId(androidx.appcompat.R.id.action_bar),
                                        AndroidTestHelper.childAtPosition(
                                                withId(androidx.appcompat.R.id.action_bar_container),
                                                0)),
                                2),
                        isDisplayed()));
        appCompatImageButton2.perform(click());

        AndroidTestHelper.navigateTo(AndroidTestHelper.MainMenu.OVERVIEW);

        ViewInteraction materialButton3 = onView(
                allOf(withId(R.id.logManualDose), withText("Log additional dose"),
                        isDisplayed()));
        materialButton3.perform(click());

        DataInteraction materialTextView5 = onData(anything())
                .inAdapterView(allOf(withClassName(is("com.android.internal.app.AlertController$RecycleListView")),
                        AndroidTestHelper.childAtPosition(
                                withClassName(is("android.widget.FrameLayout")),
                                0)))
                .atPosition(2);
        materialTextView5.perform(click());

        ViewInteraction textInputEditText = onView(
                allOf(AndroidTestHelper.childAtPosition(
                                AndroidTestHelper.childAtPosition(
                                        withClassName(is("com.google.android.material.textfield.TextInputLayout")),
                                        0),
                                0),
                        isDisplayed()));
        textInputEditText.perform(replaceText("1"), closeSoftKeyboard());

        ViewInteraction materialButton4 = onView(
                allOf(withId(android.R.id.button1), withText("OK"),
                        AndroidTestHelper.childAtPosition(
                                AndroidTestHelper.childAtPosition(
                                        withClassName(is("android.widget.ScrollView")),
                                        0),
                                3)));
        materialButton4.perform(scrollTo(), click());

        onView(new RecyclerViewMatcher(R.id.latestReminders).atPositionOnView(0, R.id.reminderEventText))
                .check(matches(withText(startsWith("1 of Ginseng (200mg)"))));
    }

}
