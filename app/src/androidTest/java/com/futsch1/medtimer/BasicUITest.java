package com.futsch1.medtimer;


import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
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
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;

import androidx.test.espresso.DataInteraction;
import androidx.test.espresso.ViewInteraction;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.filters.LargeTest;
import androidx.test.rule.GrantPermissionRule;

import org.junit.Rule;
import org.junit.Test;

@LargeTest
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
        AndroidTestHelper.createMedicine("Test");

        ViewInteraction materialButton = onView(
                allOf(withId(android.R.id.button1), withText("OK")));
        materialButton.perform(scrollTo(), click());

        AndroidTestHelper.createReminder("1", null);

        onView(isRoot()).perform(AndroidTestHelper.waitFor(1000));
        ViewInteraction materialButton4 = onView(withId(R.id.open_advanced_settings));
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

        pressBack();

        ViewInteraction materialButton5 = onView(
                allOf(withId(R.id.open_advanced_settings)));
        materialButton5.perform(click());

        ViewInteraction editText = onView(
                allOf(withId(R.id.editInstructions), withText(R.string.before_meal)));
        editText.check(matches(withText(R.string.before_meal)));

        pressBack();

        ViewInteraction textInputEditText3 = onView(
                allOf(withId(R.id.editAmount), withText("1")));
        textInputEditText3.perform(replaceText("2"));

        ViewInteraction textInputEditText4 = onView(
                allOf(withId(R.id.editAmount), withText("2")));
        textInputEditText4.perform(closeSoftKeyboard());

        pressBack();

        onView(isRoot()).perform(AndroidTestHelper.waitFor(100));
        ViewInteraction recyclerView2 = onView(
                allOf(withId(R.id.medicineList)));
        recyclerView2.perform(actionOnItemAtPosition(0, click()));

        ViewInteraction editText2 = onView(
                allOf(withId(R.id.editAmount), withText("2")));
        editText2.check(matches(withText("2")));

        pressBack();

        AndroidTestHelper.navigateTo(AndroidTestHelper.MainMenu.OVERVIEW);
        onView(isRoot()).perform(AndroidTestHelper.waitFor(1000));

        ViewInteraction textView = onView(
                new RecyclerViewMatcher(R.id.nextReminders).atPositionOnView(0, R.id.nextReminderText));
        String expectedText = getInstrumentation().getTargetContext().getString(R.string.reminder_event, "2", "Test", "");
        textView.check(matches(withText(startsWith(expectedText))));
    }

}
