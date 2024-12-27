package com.futsch1.medtimer;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withClassName;
import static androidx.test.espresso.matcher.ViewMatchers.withHint;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.futsch1.medtimer.AndroidTestHelper.MainMenu.OVERVIEW;
import static com.futsch1.medtimer.AndroidTestHelper.navigateTo;
import static com.futsch1.medtimer.AndroidTestHelper.setTime;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import android.content.Context;

import androidx.test.espresso.ViewInteraction;
import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;

import com.futsch1.medtimer.helpers.TimeHelper;

import org.junit.Rule;
import org.junit.Test;

import java.time.LocalTime;

@LargeTest
public class ReminderTypeTest extends BaseTestHelper {

    @Rule
    public ActivityScenarioRule<MainActivity> mActivityScenarioRule =
            new ActivityScenarioRule<>(MainActivity.class);

    @Test
    public void reminderTypeTest() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();

        AndroidTestHelper.createMedicine("Test");

        // Standard time based reminder (amount 1)
        LocalTime reminder1Time = LocalTime.now().plusMinutes(40);
        AndroidTestHelper.createReminder("1", reminder1Time);

        // Linked reminder (amount 2) 30 minutes later
        onView(withId(R.id.open_advanced_settings)).perform(click());
        onView(withId(R.id.addLinkedReminder)).perform(click());
        ViewInteraction textInputEditText = onView(
                allOf(AndroidTestHelper.childAtPosition(
                                AndroidTestHelper.childAtPosition(
                                        withClassName(is("com.google.android.material.textfield.TextInputLayout")),
                                        0),
                                0),
                        isDisplayed()));
        textInputEditText.perform(replaceText("2"), closeSoftKeyboard());
        onView(withId(android.R.id.button1)).perform(scrollTo(), click());

        setTime(0, 30);


        // Interval reminder (amount 3) 2 hours from now
        onView(withId(R.id.addReminder)).perform(click());

        onView(withId(R.id.editAmount)).perform(replaceText("3"), closeSoftKeyboard());
        onView(withId(R.id.intervalBased)).perform(click());
        onView(withId(R.id.intervalHours)).perform(click());
        onView(withId(R.id.editIntervalTime)).perform(replaceText("2"), closeSoftKeyboard());
        onView(withId(R.id.createReminder)).perform(click());

        // Check calendar view not crashing
        onView(withId(R.id.openCalendar)).perform(click());
        pressBack();

        // Check reminder list
        int positionOfReminder1 = reminder1Time.isBefore(LocalTime.of(2, 0)) ? 0 : 1;
        int positionOfReminder2 = reminder1Time.isBefore(LocalTime.of(1, 30)) ? 1 : 2;
        int positionOfReminder3 = reminder1Time.isBefore(LocalTime.of(2, 0)) ? (reminder1Time.isBefore(LocalTime.of(1, 30)) ? 2 : 1) : 0;

        onView(withId(R.id.reminderList)).perform(RecyclerViewActions.scrollToPosition(positionOfReminder3));
        ViewInteraction cardOfReminder3 = onView(new RecyclerViewMatcher(R.id.reminderList).atPositionOnView(positionOfReminder3, R.id.reminderCardLayout));
        String expectedString = context.getString(R.string.every_interval, "2 " + context.getResources().getQuantityString(R.plurals.hours, 2));
        cardOfReminder3.check(matches(hasDescendant(withText(containsString(expectedString)))));

        onView(withId(R.id.reminderList)).perform(RecyclerViewActions.scrollToPosition(positionOfReminder1));
        ViewInteraction cardOfReminder1 = onView(new RecyclerViewMatcher(R.id.reminderList).atPositionOnView(positionOfReminder1, R.id.reminderCardLayout));
        cardOfReminder1.check(matches(hasDescendant(withHint(R.string.time))));
        expectedString = TimeHelper.minutesToTimeString(context, reminder1Time.toSecondOfDay() / 60);
        cardOfReminder1.check(matches(hasDescendant(withText(containsString(expectedString)))));

        onView(withId(R.id.reminderList)).perform(RecyclerViewActions.scrollToPosition(positionOfReminder2));
        ViewInteraction cardOfReminder2 = onView(new RecyclerViewMatcher(R.id.reminderList).atPositionOnView(positionOfReminder2, R.id.reminderCardLayout));
        cardOfReminder2.check(matches(hasDescendant(withHint(R.string.delay))));
        expectedString = context.getString(R.string.linked_reminder_summary, TimeHelper.minutesToTimeString(context, reminder1Time.toSecondOfDay() / 60));
        cardOfReminder2.check(matches(hasDescendant(withText(containsString(expectedString)))));

        onView(withId(R.id.reminderList)).perform(RecyclerViewActions.scrollToPosition(positionOfReminder3));
        onView(new RecyclerViewMatcher(R.id.reminderList).atPositionOnView(positionOfReminder3, R.id.open_advanced_settings)).perform(click());
        pressBack();
        onView(withId(R.id.reminderList)).perform(RecyclerViewActions.scrollToPosition(positionOfReminder2));
        onView(new RecyclerViewMatcher(R.id.reminderList).atPositionOnView(positionOfReminder2, R.id.open_advanced_settings)).perform(click());
        pressBack();
        onView(withId(R.id.reminderList)).perform(RecyclerViewActions.scrollToPosition(positionOfReminder1));
        onView(new RecyclerViewMatcher(R.id.reminderList).atPositionOnView(positionOfReminder1, R.id.open_advanced_settings)).perform(click());
        pressBack();

        // Check overview and next reminders
        navigateTo(OVERVIEW);

        cardOfReminder1 = onView(new RecyclerViewMatcher(R.id.nextReminders).atPositionOnView(0, R.id.nextReminderCardLayout));
        cardOfReminder1.perform(scrollTo());
        expectedString = context.getString(R.string.reminder_event, "1", "Test", "");
        cardOfReminder1.check(matches(hasDescendant(withText(containsString(expectedString)))));
        expectedString = TimeHelper.minutesToTimeString(context, reminder1Time.toSecondOfDay() / 60);
        cardOfReminder1.check(matches(hasDescendant(withText(containsString(expectedString)))));

        UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        if (device.getDisplayWidth() < device.getDisplayHeight()) {
            onView(withId(R.id.expandNextReminders)).perform(click());
        }

        cardOfReminder3 = onView(new RecyclerViewMatcher(R.id.nextReminders).atPositionOnView(1, R.id.nextReminderCardLayout));
        cardOfReminder3.perform(scrollTo());
        expectedString = context.getString(R.string.reminder_event, "3", "Test", "");
        cardOfReminder3.check(matches(hasDescendant(withText(containsString(expectedString)))));

        // If possible, take reminder 1 now and see if reminder 2 appears
        if (reminder1Time.isAfter(LocalTime.of(0, 30))) {
            onView(new RecyclerViewMatcher(R.id.nextReminders).atPositionOnView(0, R.id.takenNow)).perform(click());

            cardOfReminder2 = onView(new RecyclerViewMatcher(R.id.nextReminders).atPositionOnView(0, R.id.nextReminderCardLayout));
            expectedString = context.getString(R.string.reminder_event, "2", "Test", "");
            cardOfReminder2.check(matches(hasDescendant(withText(containsString(expectedString)))));
        }
    }
}