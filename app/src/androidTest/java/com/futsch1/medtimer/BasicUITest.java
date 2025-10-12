package com.futsch1.medtimer;


import static androidx.test.espresso.Espresso.pressBack;
import static com.adevinta.android.barista.assertion.BaristaCheckedAssertions.assertChecked;
import static com.adevinta.android.barista.assertion.BaristaCheckedAssertions.assertUnchecked;
import static com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertContains;
import static com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertDisplayed;
import static com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertNotContains;
import static com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn;
import static com.adevinta.android.barista.interaction.BaristaDialogInteractions.clickDialogPositiveButton;
import static com.adevinta.android.barista.interaction.BaristaEditTextInteractions.clearText;
import static com.adevinta.android.barista.interaction.BaristaEditTextInteractions.writeTo;
import static com.adevinta.android.barista.interaction.BaristaKeyboardInteractions.closeKeyboard;
import static com.adevinta.android.barista.interaction.BaristaListInteractions.clickListItem;
import static com.adevinta.android.barista.interaction.BaristaListInteractions.clickListItemChild;
import static com.adevinta.android.barista.interaction.BaristaMenuClickInteractions.openMenu;
import static com.futsch1.medtimer.AndroidTestHelper.MainMenu.MEDICINES;
import static com.futsch1.medtimer.AndroidTestHelper.MainMenu.OVERVIEW;
import static com.futsch1.medtimer.AndroidTestHelper.navigateTo;
import static com.futsch1.medtimer.AndroidTestHelper.setDate;
import static com.futsch1.medtimer.AndroidTestHelper.setValue;

import android.view.View;
import android.widget.TextView;

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

        clickOn(R.string.dosing_instructions);
        clickOn(R.string.sample_instructions);
        clickListItem(0);

        pressBack();
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
        clickOn(R.string.cycle_reminders);
        clickOn(R.string.cycle_start_date);
        setDate(cycleStart.getTime());
        clickOn(R.string.cycle_consecutive_days);
        setValue("5");
        clickOn(R.string.cycle_pause_days);
        setValue("6");
        pressBack();

        clickOn(R.string.remind_on_weekdays);
        clickOn(R.string.monday);
        clickOn(R.string.tuesday);
        clickDialogPositiveButton();

        clickOn(R.string.remind_on_days_of_month);
        clickOn("1");
        clickOn("3");
        clickDialogPositiveButton();

        pressBack();

        clickOn(R.id.openAdvancedSettings);

        clickOn(R.string.cycle_reminders);
        assertContains(cycleStartString);
        assertContains("5");
        assertContains("6");
        pressBack();

        clickOn(R.string.remind_on_weekdays);
        assertUnchecked(R.string.monday);
        assertUnchecked(R.string.tuesday);
        assertChecked(R.string.wednesday);
        clickDialogPositiveButton();

        clickOn(R.string.remind_on_days_of_month);
        assertChecked("1");
        assertUnchecked("2");
        assertChecked("3");
        clickDialogPositiveButton();
    }

    @Test
    //@AllowFlaky(attempts = 1)
    public void notesTest() {
        AndroidTestHelper.createMedicine("Test");

        // Test saving notes
        String notes = "Contains catnip\n\nmeow :3";

        clickOn(R.id.openNotes);
        writeTo(R.id.notes, notes);
        closeKeyboard();
        clickOn(R.id.confirmSaveNotes);

        // Check if the note is saved
        clickOn(R.id.openNotes);
        assertDisplayed(R.id.notes, notes);

        // Test cancelling saving notes
        clearText(R.id.notes);
        closeKeyboard();
        clickOn(R.id.cancelSaveNotes);

        // Check that the note is unmodified
        clickOn(R.id.openNotes);
        assertDisplayed(R.id.notes, notes);
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

        clickOn(R.id.overviewFragment);

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
