package com.futsch1.medtimer;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withClassName;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;

import android.icu.util.Calendar;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import androidx.test.espresso.NoMatchingViewException;
import androidx.test.espresso.PerformException;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.ViewInteraction;

import com.google.android.material.textfield.TextInputEditText;

import junit.framework.AssertionFailedError;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import java.text.DateFormat;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;

public class AndroidTestHelper {
    static void setAllRemindersTo12AM() {
        AndroidTestHelper.navigateTo(AndroidTestHelper.MainMenu.MEDICINES);

        setReminderTo12AM(0);
        setReminderTo12AM(1);
        setReminderTo12AM(2);
        setReminderTo12AM(3);

        AndroidTestHelper.navigateTo(AndroidTestHelper.MainMenu.OVERVIEW);
    }

    private static void setReminderTo12AM(int position) {
        onViewWithTimeout(new RecyclerViewMatcher(R.id.medicineList).atPositionOnView(position, R.id.medicineCard)).perform(click());

        onViewWithTimeout(new RecyclerViewMatcher(R.id.reminderList).atPositionOnView(0, R.id.editReminderTime)).perform(click());

        setTime(0, 0);
        pressBack();
        pressBack();
    }

    public static void createReminder(String amount, LocalTime time) {
        onViewWithTimeoutClickable(withId(R.id.addReminder)).perform(click());

        onView(withId(R.id.editAmount)).perform(replaceText(amount), closeSoftKeyboard());

        if (time != null) {
            onView(withId(R.id.editReminderTime)).perform(click());
            setTime(time.getHour(), time.getMinute());
        }

        onView(withId(R.id.createReminder)).perform(click());
    }

    static ViewInteraction onViewWithTimeoutClickable(
            Matcher<View> matcher
    ) {
        int retries = 10;
        while (retries-- >= 0) {
            ViewInteraction viewInteraction = onView(matcher);
            try {
                viewInteraction.check(matches(isCompletelyDisplayed()));
                return viewInteraction;
            } catch (NoMatchingViewException e) {
                onView(isRoot()).perform(AndroidTestHelper.waitFor(500));
            } catch (AssertionFailedError e) {
                try {
                    viewInteraction.perform(scrollTo());
                } catch (PerformException e2) {
                    // Intentionally empty
                }
                onView(isRoot()).perform(AndroidTestHelper.waitFor(500));
            }
        }
        throw new AssertionError("View did not become clickable");
    }

    public static void setTime(int hour, int minute) {
        try {
            onView(withId(com.google.android.material.R.id.material_clock_period_am_button)).perform(click());
            if (hour == 12) {
                onView(withId(com.google.android.material.R.id.material_clock_period_pm_button)).perform(click());
            }
            if (hour > 12) {
                hour -= 12;
                onView(withId(com.google.android.material.R.id.material_clock_period_pm_button)).perform(click());
            }
            if (hour == 0) {
                hour = 12;
            }
        } catch (PerformException e) {
            // Happens when am/pm button is not visible - in this case, we are in 24h format
        }

        onView(withId(com.google.android.material.R.id.material_timepicker_mode_button)).perform(click());
        onView(withId(com.google.android.material.R.id.material_hour_text_input)).perform(click());
        onView(allOf(isDisplayed(), withClassName(is(TextInputEditText.class.getName())))).perform(replaceText(String.valueOf(hour)));
        onView(withId(com.google.android.material.R.id.material_minute_text_input)).perform(click());
        onView(allOf(isDisplayed(), withClassName(is(TextInputEditText.class.getName())))).perform(replaceText(String.valueOf(minute)));
        onView(withId(com.google.android.material.R.id.material_timepicker_ok_button)).perform(click());
    }

    public static ViewAction waitFor(long delay) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isRoot();
            }

            @Override
            public String getDescription() {
                return "wait for " + delay + "milliseconds";
            }

            @Override
            public void perform(UiController uiController, View view) {
                uiController.loopMainThreadForAtLeast(delay);
            }
        };
    }

    public static void createMedicine(String name) {
        AndroidTestHelper.navigateTo(AndroidTestHelper.MainMenu.MEDICINES);

        onViewWithTimeoutClickable(withId(R.id.addMedicine)).perform(click());

        ViewInteraction textInputEditText = onView(
                allOf(AndroidTestHelper.childAtPosition(
                                AndroidTestHelper.childAtPosition(
                                        withClassName(is("com.google.android.material.textfield.TextInputLayout")),
                                        0),
                                0),
                        isDisplayed()));
        textInputEditText.perform(replaceText(name), closeSoftKeyboard());

        onView(allOf(withId(android.R.id.button1), withText("OK"))).perform(scrollTo(), click());
    }

    public static void navigateTo(MainMenu mainMenu) {
        int[] menuItems = {R.string.tab_overview, R.string.tab_medicine, R.string.analysis};
        int[] menuIds = {R.id.overviewFragment, R.id.medicinesFragment, R.id.statisticsFragment};
        int[] targetView = {R.id.nextRemindersCard, R.id.medicineList, R.id.analysisView};
        ViewInteraction bottomNavigationItemView = onViewWithTimeout(
                allOf(withId(menuIds[mainMenu.ordinal()]), withContentDescription(menuItems[mainMenu.ordinal()]),
                        isDisplayed()));
        int repeat = 10;
        while (repeat-- > 0) {
            try {
                bottomNavigationItemView.perform(click());
                onViewWithTimeout(withId(targetView[mainMenu.ordinal()])).check(matches(isDisplayed()));
                return;
            } catch (AssertionFailedError e) {
                onView(isRoot()).perform(AndroidTestHelper.waitFor(500));
            }
        }
        assert false;
    }

    public static Matcher<View> childAtPosition(
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

    static ViewInteraction onViewWithTimeout(
            Matcher<View> matcher
    ) {
        int retries = 10;
        while (retries-- >= 0) {
            try {
                ViewInteraction viewInteraction = onView(matcher);
                viewInteraction.check(matches(isDisplayed()));
                return viewInteraction;
            } catch (NoMatchingViewException e) {
                onView(isRoot()).perform(AndroidTestHelper.waitFor(500));
            }
        }
        throw new AssertionError("View did not become visible");
    }

    public static String dateToString(Date date) {
        return DateFormat.getDateInstance(DateFormat.SHORT).format(date);
    }

    public static LocalDateTime getNextNotificationTime() {
        Calendar rightNow = Calendar.getInstance();
        LocalDateTime dateTime = LocalDateTime.of(rightNow.get(Calendar.YEAR), rightNow.get(Calendar.MONTH) + 1, rightNow.get(Calendar.DAY_OF_MONTH), rightNow.get(Calendar.HOUR_OF_DAY), rightNow.get(Calendar.MINUTE), 0);
        if (dateTime.getSecond() < 55) {
            dateTime = dateTime.plusMinutes(1);
        } else {
            dateTime = dateTime.plusMinutes(2);
        }
        return dateTime;
    }

    public enum MainMenu {OVERVIEW, MEDICINES, ANALYSIS}
}
