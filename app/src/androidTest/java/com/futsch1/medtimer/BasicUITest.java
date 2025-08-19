package com.futsch1.medtimer;


import static androidx.test.espresso.Espresso.pressBack;
import static com.adevinta.android.barista.assertion.BaristaCheckedAssertions.assertChecked;
import static com.adevinta.android.barista.assertion.BaristaCheckedAssertions.assertUnchecked;
import static com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertContains;
import static com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertDisplayed;
import static com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertNotContains;
import static com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn;
import static com.adevinta.android.barista.interaction.BaristaDialogInteractions.clickDialogPositiveButton;
import static com.adevinta.android.barista.interaction.BaristaEditTextInteractions.writeTo;
import static com.adevinta.android.barista.interaction.BaristaListInteractions.clickListItem;
import static com.adevinta.android.barista.interaction.BaristaListInteractions.clickListItemChild;
import static com.adevinta.android.barista.interaction.BaristaMenuClickInteractions.openMenu;
import static com.futsch1.medtimer.AndroidTestHelper.MainMenu.MEDICINES;
import static com.futsch1.medtimer.AndroidTestHelper.MainMenu.OVERVIEW;
import static com.futsch1.medtimer.AndroidTestHelper.navigateTo;

import android.view.View;
import android.widget.TextView;

import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiScrollable;
import androidx.test.uiautomator.UiSelector;

import org.junit.Test;

import java.time.LocalTime;
import java.util.Calendar;
import java.util.concurrent.atomic.AtomicReference;

public class BasicUITest extends BaseTestHelper {

    public static final String TEST_2 = "Test (2)";

    @Test
    //@AllowFlaky(attempts = 1)
    public void basicUITest() {
        AndroidTestHelper.createMedicine(" Test ");
        AndroidTestHelper.createReminder("1", LocalTime.of(18, 0));

        clickOn(R.id.openAdvancedSettings);

        UiScrollable appViews = new UiScrollable(
                new UiSelector().scrollable(true));
        try {
            appViews.scrollForward();
        } catch (UiObjectNotFoundException e) {
            internalAssert(false);
        }

        clickOn(com.google.android.material.R.id.text_input_end_icon);
        clickListItem(1);

        pressBack();

        clickOn(R.id.openAdvancedSettings);
        assertContains(R.string.before_meal);
        pressBack();

        writeTo(R.id.editAmount, " 2 ");
        pressBack();

        clickListItem(R.id.medicineList, 0);
        assertDisplayed(R.id.editAmount, "2");
        pressBack();

        AndroidTestHelper.navigateTo(AndroidTestHelper.MainMenu.OVERVIEW);
        assertContains(TEST_2);

        AndroidTestHelper.navigateTo(AndroidTestHelper.MainMenu.MEDICINES);
        clickListItem(R.id.medicineList, 0);
        writeTo(R.id.editMedicineName, " Test2 ");
        pressBack();

        AndroidTestHelper.navigateTo(AndroidTestHelper.MainMenu.OVERVIEW);
        assertContains("Test2 (2)");
    }

    @Test
    //@AllowFlaky(attempts = 1)
    public void menuHandlingTest() {
        AndroidTestHelper.createMedicine("Test");
        AndroidTestHelper.createReminder("1", LocalTime.of(12, 0));

        clickOn(R.id.openAdvancedSettings);

        Calendar cycleStart = Calendar.getInstance();
        cycleStart.set(2025, 1, 1);
        String cycleStartString = AndroidTestHelper.dateToString(cycleStart.getTime());
        writeTo(R.id.cycleStartDate, cycleStartString);
        writeTo(R.id.consecutiveDays, "5");
        writeTo(R.id.pauseDays, "6");

        clickOn(R.id.remindOnWeekdays);
        clickOn(R.string.monday);
        clickOn(R.string.tuesday);
        clickDialogPositiveButton();

        clickOn(R.id.remindOnDaysOfMonth);
        clickOn("1");
        clickOn("3");
        clickDialogPositiveButton();

        pressBack();

        clickOn(R.id.openAdvancedSettings);

        assertContains(R.id.cycleStartDate, cycleStartString);
        assertContains(R.id.consecutiveDays, "5");
        assertContains(R.id.pauseDays, "6");

        clickOn(R.id.remindOnWeekdays);
        assertUnchecked(R.string.monday);
        assertUnchecked(R.string.tuesday);
        assertChecked(R.string.wednesday);
        clickDialogPositiveButton();

        clickOn(R.id.remindOnDaysOfMonth);
        assertUnchecked("1");
        assertChecked("2");
        assertUnchecked("3");
        clickDialogPositiveButton();
    }

    @Test
    //@AllowFlaky(attempts = 1)
    public void appIntro() {
        openMenu();

        clickOn(R.string.show_intro);

        assertDisplayed(R.string.intro_welcome);
        assertDisplayed(R.string.intro_welcome_description);

        clickOn(com.github.appintro.R.id.skip);

        assertDisplayed(R.string.tab_overview);
    }

    @Test
    //@AllowFlaky(attempts = 1)
    public void overviewFilters() {
        AndroidTestHelper.createMedicine("Test");
        AndroidTestHelper.createIntervalReminder("2", 1000);

        pressBack();

        AndroidTestHelper.navigateTo(AndroidTestHelper.MainMenu.OVERVIEW);
        assertContains(TEST_2);

        clickOn(R.id.filterRaised);
        assertContains(TEST_2);
        clickOn(R.id.filterRaised);

        assertContains(TEST_2);

        clickOn(R.id.filterTaken);
        assertNotContains(TEST_2);
        clickOn(R.id.filterTaken);

        assertContains(TEST_2);

        clickListItemChild(R.id.reminders, 0, R.id.stateButton);
        clickOn(R.id.takenButton);

        clickOn(R.id.filterTaken);
        assertContains(TEST_2);
        clickOn(R.id.filterTaken);

        assertContains(TEST_2);

        clickOn(R.id.filterRaised);
        assertNotContains(TEST_2);
        clickOn(R.id.filterRaised);

        clickOn(R.id.filterSkipped);
        assertNotContains(TEST_2);
        clickOn(R.id.filterSkipped);

        clickOn(R.id.filterScheduled);
        assertNotContains(TEST_2);
        clickOn(R.id.filterScheduled);

        navigateTo(MEDICINES);
        clickOn("Test");
        AndroidTestHelper.createReminder("1", LocalTime.of(20, 0));

        navigateTo(OVERVIEW);

        assertContains("Test (1)");

        clickOn(R.id.filterScheduled);
        assertContains("Test (1)");
    }

    @Test
    //@AllowFlaky(attempts = 1)
    @SuppressWarnings("java:S2699") // Using internal assert
    public void overviewDaySelection() {
        clickOn("2");

        navigateTo(MEDICINES);

        navigateTo(OVERVIEW);

        AtomicReference<View> view = new AtomicReference<>();
        view.set(baristaRule.getActivityTestRule().getActivity().findViewById(R.id.overviewWeek));

        TextView currentDay = view.get().findViewWithTag("selected");
        internalAssert(currentDay.getText().equals("2"));

        navigateTo(OVERVIEW);
        view.set(baristaRule.getActivityTestRule().getActivity().findViewById(R.id.overviewWeek));
        currentDay = view.get().findViewWithTag("selected");
        internalAssert(currentDay.getText().equals("1"));
    }
}
