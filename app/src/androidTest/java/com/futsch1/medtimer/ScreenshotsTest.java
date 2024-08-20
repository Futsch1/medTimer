package com.futsch1.medtimer;


import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.action.ViewActions.swipeLeft;
import static androidx.test.espresso.action.ViewActions.swipeRight;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withClassName;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static com.futsch1.medtimer.AndroidTestHelper.childAtPosition;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;

import android.content.Context;

import androidx.test.espresso.ViewInteraction;
import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.rule.GrantPermissionRule;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.Until;

import com.evrencoskun.tableview.TableView;
import com.futsch1.medtimer.helpers.TimeHelper;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicReference;

import tools.fastlane.screengrab.Screengrab;
import tools.fastlane.screengrab.UiAutomatorScreenshotStrategy;
import tools.fastlane.screengrab.locale.LocaleTestRule;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class ScreenshotsTest {
    @ClassRule
    public static final LocaleTestRule localeTestRule = new LocaleTestRule();

    @Rule
    public ActivityScenarioRule<MainActivity> mActivityScenarioRule =
            new ActivityScenarioRule<>(MainActivity.class);

    @Rule
    public GrantPermissionRule mGrantPermissionRule =
            GrantPermissionRule.grant(
                    "android.permission.POST_NOTIFICATIONS");

    @Test
    public void screenshotsTest() {
        Screengrab.setDefaultScreenshotStrategy(new UiAutomatorScreenshotStrategy());
        UiDevice device = UiDevice.getInstance(getInstrumentation());
        device.wait(Until.findObject(By.desc("More options")), 1000);

        openActionBarOverflowOrOptionsMenu(getInstrumentation().getTargetContext());

        onView(withText(R.string.generate_test_data)).perform(click());
        onView(isRoot()).perform(AndroidTestHelper.waitFor(2000));

        AndroidTestHelper.setAllRemindersTo12AM();

        device.openNotification();
        device.wait(Until.findObject(By.text("MedTimer")), 2000);
        UiObject2 medTimerNotifications = device.findObject(By.text("MedTimer"));
        int startX = medTimerNotifications.getVisibleBounds().centerX();
        int startY = medTimerNotifications.getVisibleBounds().top;
        int endY = medTimerNotifications.getVisibleBounds().bottom;
        device.swipe(startX, startY, startX, endY, 100);
        device.swipe(startX, startY + 100, startX, startY + 200, 100);
        Screengrab.screenshot("5");
        device.pressBack();

        onView(isRoot()).perform(AndroidTestHelper.waitFor(1000));

        onView(new RecyclerViewMatcher(R.id.latestReminders).atPositionOnView(0, R.id.chipTaken)).perform(click());
        onView(new RecyclerViewMatcher(R.id.latestReminders).atPositionOnView(1, R.id.chipTaken)).perform(click());
        onView(new RecyclerViewMatcher(R.id.latestReminders).atPositionOnView(2, R.id.chipSkipped)).perform(click());
        onView(withId(R.id.latestReminders)).perform(RecyclerViewActions.actionOnItemAtPosition(3, scrollTo()));
        onView(new RecyclerViewMatcher(R.id.latestReminders).atPositionOnView(3, R.id.chipTaken)).perform(scrollTo(), click());
        Screengrab.screenshot("1");

        AndroidTestHelper.navigateTo(AndroidTestHelper.MainMenu.MEDICINES);
        Screengrab.screenshot("2");

        ViewInteraction recyclerView = onView(
                allOf(withId(R.id.medicineList)));
        recyclerView.perform(actionOnItemAtPosition(0, click()));
        Screengrab.screenshot("3");

        onView(new RecyclerViewMatcher(R.id.reminderList).atPositionOnView(1, R.id.open_advanced_settings)).perform(click());
        Screengrab.screenshot("4");

        AndroidTestHelper.navigateTo(AndroidTestHelper.MainMenu.ANALYSIS);
        Screengrab.screenshot("6");

        onView(withId(R.id.timeSpinner)).perform(click());

        onData(anything())
                .inAdapterView(childAtPosition(
                        withClassName(is("android.widget.PopupWindow$PopupBackgroundView")),
                        0))
                .atPosition(1).perform(click());

        onView(allOf(withId(R.id.reminderTableButton))).perform(click());
        Screengrab.screenshot("7");

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

        pressBack();
        onView(allOf(withId(R.id.reminderCalendarButton))).perform(click());
        Screengrab.screenshot("8");

        AndroidTestHelper.navigateTo(AndroidTestHelper.MainMenu.OVERVIEW);

        // Edit most recent reminder event
        onView(withId(R.id.latestReminders)).perform(RecyclerViewActions.actionOnItemAtPosition(0, swipeRight()));
        onView(withId(R.id.editEventName)).perform(replaceText("TestMedicine"));
        onView(withId(R.id.editEventAmount)).perform(replaceText("Much"));
        pressBack();

        Context targetContext = getInstrumentation().getTargetContext();
        String dateString = TimeHelper.toLocalizedDateString(targetContext, LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() / 1000);
        String expectedText = getInstrumentation().getTargetContext().getString(R.string.reminder_event, "Much", "TestMedicine", dateString);
        onView(new RecyclerViewMatcher(R.id.latestReminders).atPositionOnView(0, R.id.reminderEventText, null))
                .check(matches(withText(startsWith(expectedText))));

        // And now delete it
        onView(withId(R.id.latestReminders)).perform(RecyclerViewActions.actionOnItemAtPosition(0, swipeLeft()));
        onView(allOf(withId(android.R.id.button1))).perform(click());

        onView(new RecyclerViewMatcher(R.id.latestReminders).atPositionOnView(0, R.id.reminderEventText, null))
                .check(matches(withText(not(startsWith(expectedText)))));
    }


}
