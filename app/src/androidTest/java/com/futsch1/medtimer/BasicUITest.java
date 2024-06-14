package com.futsch1.medtimer;


import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition;
import static androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA;
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

import com.futsch1.medtimer.database.MedicineRepository;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class BasicUITest {

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

        ViewInteraction extendedFloatingActionButton = onView(
                withId(R.id.addMedicine));
        extendedFloatingActionButton.perform(click());

        ViewInteraction textInputEditText = onView(
                allOf(AndroidTestHelper.childAtPosition(
                                AndroidTestHelper.childAtPosition(
                                        withClassName(is("com.google.android.material.textfield.TextInputLayout")),
                                        0),
                                0),
                        isDisplayed()));
        textInputEditText.perform(replaceText("Test"), closeSoftKeyboard());

        ViewInteraction materialButton = onView(
                allOf(withId(android.R.id.button1), withText("OK")));
        materialButton.perform(scrollTo(), click());

        ViewInteraction recyclerView = onView(
                allOf(withId(R.id.medicineList)));
        recyclerView.perform(actionOnItemAtPosition(0, click()));

        ViewInteraction extendedFloatingActionButton2 = onView(
                allOf(withId(R.id.addReminder)));
        extendedFloatingActionButton2.perform(click());

        ViewInteraction textInputEditText2 = onView(
                allOf(AndroidTestHelper.childAtPosition(
                                AndroidTestHelper.childAtPosition(
                                        withClassName(is("com.google.android.material.textfield.TextInputLayout")),
                                        0),
                                0),
                        isDisplayed()));
        textInputEditText2.perform(replaceText("1"), closeSoftKeyboard());

        ViewInteraction materialButton2 = onView(
                allOf(withId(android.R.id.button1), withText("OK")));
        materialButton2.perform(scrollTo(), click());

        ViewInteraction materialButton3 = onView(
                allOf(withId(com.google.android.material.R.id.material_timepicker_ok_button), withText("OK")));
        materialButton3.perform(click());

        onView(isRoot()).perform(AndroidTestHelper.waitFor(1000));
        ViewInteraction materialButton4 = onView(
                allOf(withId(R.id.open_advanced_settings), withText("Advanced settings"),
                        AndroidTestHelper.childAtPosition(
                                AndroidTestHelper.childAtPosition(
                                        withId(R.id.advanced_settings),
                                        0),
                                1),
                        isDisplayed()));
        materialButton4.perform(click());

        ViewInteraction checkableImageButton = onView(
                allOf(withId(com.google.android.material.R.id.text_input_end_icon),
                        AndroidTestHelper.childAtPosition(
                                AndroidTestHelper.childAtPosition(
                                        withClassName(is("com.google.android.material.textfield.EndCompoundLayout")),
                                        1),
                                0),
                        isDescendantOfA(withId(R.id.editInstructionsLayout)),
                        isDisplayed()));
        checkableImageButton.perform(click());

        DataInteraction materialTextView = onData(anything())
                .inAdapterView(allOf(withClassName(is("com.android.internal.app.AlertController$RecycleListView")),
                        AndroidTestHelper.childAtPosition(
                                withClassName(is("android.widget.FrameLayout")),
                                0)))
                .atPosition(1);
        materialTextView.perform(click());

        ViewInteraction appCompatImageButton = onView(
                allOf(withContentDescription("Navigate up")));
        appCompatImageButton.perform(click());

        ViewInteraction materialButton5 = onView(
                allOf(withId(R.id.open_advanced_settings)));
        materialButton5.perform(click());

        ViewInteraction editText = onView(
                allOf(withId(R.id.editInstructions), withText("before the meal")));
        editText.check(matches(withText("before the meal")));

        ViewInteraction appCompatImageButton2 = onView(
                allOf(withContentDescription("Navigate up")));
        appCompatImageButton2.perform(click());

        ViewInteraction textInputEditText3 = onView(
                allOf(withId(R.id.editAmount), withText("1")));
        textInputEditText3.perform(replaceText("2"));

        ViewInteraction textInputEditText4 = onView(
                allOf(withId(R.id.editAmount), withText("2")));
        textInputEditText4.perform(closeSoftKeyboard());

        ViewInteraction appCompatImageButton3 = onView(
                allOf(withContentDescription("Navigate up")));
        appCompatImageButton3.perform(click());

        onView(isRoot()).perform(AndroidTestHelper.waitFor(100));
        ViewInteraction recyclerView2 = onView(
                allOf(withId(R.id.medicineList)));
        recyclerView2.perform(actionOnItemAtPosition(0, click()));

        ViewInteraction editText2 = onView(
                allOf(withId(R.id.editAmount), withText("2")));
        editText2.check(matches(withText("2")));

        ViewInteraction appCompatImageButton4 = onView(
                allOf(withContentDescription("Navigate up")));
        appCompatImageButton4.perform(click());

        AndroidTestHelper.navigateTo(AndroidTestHelper.MainMenu.OVERVIEW);
        onView(isRoot()).perform(AndroidTestHelper.waitFor(1000));

        ViewInteraction textView = onView(
                new RecyclerViewMatcher(R.id.nextReminders).atPositionOnView(0, R.id.nextReminderText));
        textView.check(matches(withText(startsWith("2 of Test"))));
    }

}
