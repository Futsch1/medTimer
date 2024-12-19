package com.futsch1.medtimer;


import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition;
import static androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withClassName;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static com.futsch1.medtimer.AndroidTestHelper.onViewWithTimeout;
import static com.futsch1.medtimer.AndroidTestHelper.onViewWithTimeoutClickable;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;

import androidx.test.espresso.DataInteraction;
import androidx.test.espresso.ViewInteraction;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.filters.LargeTest;

import org.junit.Rule;
import org.junit.Test;

@LargeTest
public class BasicUITest extends BaseHelper {

    @Rule
    public ActivityScenarioRule<MainActivity> mActivityScenarioRule =
            new ActivityScenarioRule<>(MainActivity.class);

    @Test
    public void basicUITest() {
        AndroidTestHelper.createMedicine("Test");
        AndroidTestHelper.createReminder("1", null);

        ViewInteraction materialButton4 = onViewWithTimeout(withId(R.id.open_advanced_settings));
        materialButton4.perform(click());

        // For some strange reason, this test does not work on GitHub Android emulator.
        // The text input end icon does not become visible and therefore not clickable.
        // So for the time being skip this part there
        boolean couldClickEndIcon = false;
        try {
            ViewInteraction checkableImageButton = onViewWithTimeoutClickable(
                    allOf(withId(com.google.android.material.R.id.text_input_end_icon),
                            isDescendantOfA(withId(R.id.editInstructionsLayout))));
            checkableImageButton.perform(click());
            couldClickEndIcon = true;
        } catch (AssertionError e) {
            // Intentionally empty
        }

        if (couldClickEndIcon) {
            DataInteraction materialTextView = onData(anything())
                    .inAdapterView(allOf(withClassName(is("com.android.internal.app.AlertController$RecycleListView")),
                            AndroidTestHelper.childAtPosition(
                                    withClassName(is("android.widget.FrameLayout")),
                                    0)))
                    .atPosition(1);
            materialTextView.perform(click());

            pressBack();

            ViewInteraction materialButton5 = onViewWithTimeout(
                    allOf(withId(R.id.open_advanced_settings)));
            materialButton5.perform(click());

            ViewInteraction editText = onViewWithTimeout(
                    allOf(withId(R.id.editInstructions), withText(R.string.before_meal)));
            editText.check(matches(withText(R.string.before_meal)));
        }

        pressBack();

        onViewWithTimeout(withId(R.id.editAmount)).perform(replaceText("2"), closeSoftKeyboard());

        pressBack();

        ViewInteraction recyclerView2 = onViewWithTimeout(
                allOf(withId(R.id.medicineList)));
        recyclerView2.perform(actionOnItemAtPosition(0, click()));

        onViewWithTimeout(withId(R.id.editAmount)).check(matches(withText("2")));

        pressBack();

        AndroidTestHelper.navigateTo(AndroidTestHelper.MainMenu.OVERVIEW);
        onView(isRoot()).perform(AndroidTestHelper.waitFor(1000));

        ViewInteraction textView = onViewWithTimeout(
                new RecyclerViewMatcher(R.id.nextReminders).atPositionOnView(0, R.id.nextReminderText));
        String expectedText = getInstrumentation().getTargetContext().getString(R.string.reminder_event, "2", "Test", "");
        textView.check(matches(withText(startsWith(expectedText))));
    }

}
