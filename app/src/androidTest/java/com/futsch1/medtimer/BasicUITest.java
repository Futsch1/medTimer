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

import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiScrollable;
import androidx.test.uiautomator.UiSelector;

import org.junit.Test;

import java.time.LocalTime;
import java.util.Calendar;

public class BasicUITest extends BaseTestHelper {

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
            throw new RuntimeException(e);
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
        assertContains("Test (2)");

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
        assertContains("Test (2)");

        clickOn(R.id.filterRaised);
        assertContains("Test (2)");
        clickOn(R.id.filterRaised);

        assertContains("Test (2)");

        clickOn(R.id.filterTaken);
        assertNotContains("Test (2)");
        clickOn(R.id.filterTaken);

        assertContains("Test (2)");

        clickListItemChild(R.id.reminders, 0, R.id.stateButton);
        clickOn(R.id.takenButton);

        clickOn(R.id.filterTaken);
        assertContains("Test (2)");
        clickOn(R.id.filterTaken);

        assertContains("Test (2)");

        clickOn(R.id.filterRaised);
        assertNotContains("Test (2)");
        clickOn(R.id.filterRaised);

        clickOn(R.id.filterSkipped);
        assertNotContains("Test (2)");
        clickOn(R.id.filterSkipped);

        clickOn(R.id.filterScheduled);
        assertNotContains("Test (2)");
        clickOn(R.id.filterScheduled);

        navigateTo(MEDICINES);
        clickOn("Test");
        AndroidTestHelper.createReminder("1", LocalTime.of(20, 0));

        navigateTo(OVERVIEW);

        assertContains("Test (1)");

        clickOn(R.id.filterScheduled);
        assertContains("Test (1)");
    }
}
