package com.futsch1.medtimer;


import static androidx.test.espresso.Espresso.pressBack;
import static com.adevinta.android.barista.assertion.BaristaListAssertions.assertCustomAssertionAtPosition;
import static com.adevinta.android.barista.assertion.BaristaListAssertions.assertDisplayedAtPosition;
import static com.adevinta.android.barista.assertion.BaristaListAssertions.assertListItemCount;
import static com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertContains;
import static com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertNotContains;
import static com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn;
import static com.adevinta.android.barista.interaction.BaristaDialogInteractions.clickDialogPositiveButton;
import static com.adevinta.android.barista.interaction.BaristaEditTextInteractions.writeTo;
import static com.adevinta.android.barista.interaction.BaristaKeyboardInteractions.closeKeyboard;
import static com.adevinta.android.barista.interaction.BaristaListInteractions.clickListItem;
import static com.adevinta.android.barista.interaction.BaristaListInteractions.clickListItemChild;
import static com.adevinta.android.barista.interaction.BaristaMenuClickInteractions.openMenu;
import static com.adevinta.android.barista.interaction.BaristaSleepInteractions.sleep;
import static com.futsch1.medtimer.AndroidTestHelper.MainMenu.OVERVIEW;
import static com.futsch1.medtimer.AndroidTestHelper.navigateTo;
import static com.futsch1.medtimer.AndroidTestHelper.setDate;
import static com.futsch1.medtimer.AndroidTestHelper.setTime;
import static com.futsch1.medtimer.AndroidTestHelper.setValue;
import static junit.framework.TestCase.assertEquals;

import android.content.Context;
import android.widget.TextView;

import androidx.test.espresso.assertion.ViewAssertions;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.platform.app.InstrumentationRegistry;

import com.evrencoskun.tableview.TableView;
import com.futsch1.medtimer.helpers.TimeHelper;

import org.junit.Test;

import java.text.DateFormat;
import java.time.Instant;
import java.time.LocalTime;
import java.util.Calendar;
import java.util.concurrent.atomic.AtomicReference;

public class ReminderTest extends BaseTestHelper {
    @Test
    //@AllowFlaky(attempts = 1)
    public void activeReminderTest() {
        Calendar futureTime = Calendar.getInstance();
        int year = futureTime.get(Calendar.YEAR);
        futureTime.set(year + 1, 1, 1);
        Calendar pastTime = Calendar.getInstance();
        pastTime.set(year - 1, 1, 1);

        AndroidTestHelper.createMedicine("Test");
        AndroidTestHelper.createReminder("1", LocalTime.of(20, 0));

        clickOn(R.id.openAdvancedSettings);
        clickOn(R.string.reminder_status);
        clickOn(R.string.active);

        pressBack();
        pressBack();
        pressBack();
        AndroidTestHelper.navigateTo(AndroidTestHelper.MainMenu.OVERVIEW);
        assertListItemCount(R.id.reminders, 0);

        AndroidTestHelper.navigateTo(AndroidTestHelper.MainMenu.MEDICINES);
        clickListItem(R.id.medicineList, 0);

        clickOn(R.id.openAdvancedSettings);
        clickOn(R.string.reminder_status);
        clickOn(R.string.active);

        pressBack();
        pressBack();
        pressBack();
        AndroidTestHelper.navigateTo(AndroidTestHelper.MainMenu.OVERVIEW);
        assertListItemCount(R.id.reminders, 1);

        AndroidTestHelper.navigateTo(AndroidTestHelper.MainMenu.MEDICINES);
        clickListItem(R.id.medicineList, 0);

        clickOn(R.id.openAdvancedSettings);
        clickOn(R.string.reminder_status);
        clickOn(R.string.period_start);
        clickOn(R.string.start_date);
        setDate(futureTime.getTime());

        pressBack();
        pressBack();
        pressBack();
        AndroidTestHelper.navigateTo(AndroidTestHelper.MainMenu.OVERVIEW);
        assertListItemCount(R.id.reminders, 0);

        AndroidTestHelper.navigateTo(AndroidTestHelper.MainMenu.MEDICINES);
        clickListItem(R.id.medicineList, 0);
        clickOn(R.id.openAdvancedSettings);
        clickOn(R.string.reminder_status);
        clickOn(R.string.period_end);
        clickOn(R.string.end_date);
        setDate(pastTime.getTime());

        pressBack();
        pressBack();
        pressBack();
        AndroidTestHelper.navigateTo(AndroidTestHelper.MainMenu.OVERVIEW);
        assertListItemCount(R.id.reminders, 0);

        AndroidTestHelper.navigateTo(AndroidTestHelper.MainMenu.MEDICINES);
        clickListItem(R.id.medicineList, 0);
        clickOn(R.id.openAdvancedSettings);
        clickOn(R.string.reminder_status);
        clickOn(R.string.start_date);
        setDate(pastTime.getTime());
        clickOn(R.string.end_date);
        setDate(futureTime.getTime());

        pressBack();
        pressBack();
        pressBack();
        AndroidTestHelper.navigateTo(AndroidTestHelper.MainMenu.OVERVIEW);
        assertListItemCount(R.id.reminders, 1);
    }

    @Test
    //@AllowFlaky(attempts = 1)
    public void activeIntervalReminderTest() {
        Calendar futureTime = Calendar.getInstance();
        int year = futureTime.get(Calendar.YEAR);
        futureTime.set(year + 1, 1, 1);
        Calendar nowTime = Calendar.getInstance();

        AndroidTestHelper.createMedicine("Test");
        AndroidTestHelper.createIntervalReminder("1", 180);

        clickOn(R.id.openAdvancedSettings);
        clickOn(R.string.interval_start_time);
        setDate(futureTime.getTime());
        setTime(futureTime.get(Calendar.HOUR_OF_DAY), futureTime.get(Calendar.MINUTE), false);
        clickOn(R.string.reminder_status);
        clickOn(R.string.active);

        pressBack();
        pressBack();
        pressBack();

        clickListItem(R.id.medicineList, 0);
        openMenu();
        clickOn(R.string.activate_all);

        clickOn(R.id.openAdvancedSettings);
        assertContains(DateFormat.getDateInstance(DateFormat.SHORT).format(nowTime.getTime()));
    }

    @Test
    //@AllowFlaky(attempts = 1)
    public void deleteLinkedReminderTest() {
        AndroidTestHelper.createMedicine("Test med");
        AndroidTestHelper.createReminder("1", LocalTime.of(0, 0));

        clickOn(R.id.openAdvancedSettings);

        clickOn(R.string.add_linked_reminder);
        clickDialogPositiveButton();
        setTime(0, 1, true);

        clickListItemChild(R.id.reminderList, 1, R.id.openAdvancedSettings);

        clickOn(R.string.add_linked_reminder);
        clickDialogPositiveButton();
        setTime(0, 2, true);

        clickListItemChild(R.id.reminderList, 0, R.id.openAdvancedSettings);

        openMenu();
        clickOn(R.string.delete);
        clickDialogPositiveButton();

        // Check that the reminder list is empty
        assertListItemCount(R.id.reminderList, 0);
    }

    @Test
    //@AllowFlaky(attempts = 1)
    public void reminderTypeTest() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();

        AndroidTestHelper.createMedicine("Test");

        // Standard time based reminder (amount 1)
        LocalTime reminder1Time = LocalTime.now().plusMinutes(40);
        AndroidTestHelper.createReminder("1", reminder1Time);

        // Linked reminder (amount 2) 30 minutes later
        clickOn(R.id.openAdvancedSettings);
        clickOn(R.string.add_linked_reminder);
        writeTo(android.R.id.input, "2");
        clickDialogPositiveButton();

        setTime(0, 30, true);

        // Interval reminder (amount 3) 2 hours from now
        clickOn(R.id.addReminder);
        clickOn(R.id.continuousIntervalCard);
        writeTo(R.id.editAmount, "3");
        clickOn(R.id.intervalHours);
        writeTo(R.id.editIntervalTime, "2");
        closeKeyboard();
        clickOn(R.id.createReminder);

        // Windowed interval reminder (amount 4)
        clickOn(R.id.addReminder);
        clickOn(R.id.windowedIntervalCard);
        writeTo(R.id.editAmount, "4");
        closeKeyboard();
        clickOn(R.id.editIntervalDailyStartTime);
        setTime(20, 0, false);
        clickOn(R.id.editIntervalDailyEndTime);
        setTime(23, 30, false);
        clickOn(R.id.intervalHours);
        writeTo(R.id.editIntervalTime, "3");
        closeKeyboard();
        clickOn(R.id.createReminder);

        // Check calendar view not crashing
        clickOn(R.id.openCalendar);
        pressBack();

        String expectedString = context.getString(R.string.every_interval, "2 " + context.getResources().getQuantityString(R.plurals.hours, 2));
        assertCustomAssertionAtPosition(R.id.reminderList, 0, R.id.reminderCardLayout, ViewAssertions.matches(ViewMatchers.withChild(ViewMatchers.withSubstring(expectedString))));

        expectedString = TimeHelper.minutesToTimeString(context, reminder1Time.toSecondOfDay() / 60);
        assertDisplayedAtPosition(R.id.reminderList, 1, R.id.editReminderTime, expectedString);

        expectedString = context.getString(R.string.linked_reminder_summary, TimeHelper.minutesToTimeString(context, reminder1Time.toSecondOfDay() / 60));
        assertDisplayedAtPosition(R.id.reminderList, 2, R.id.reminderCardLayout, expectedString);

        expectedString = context.getString(R.string.every_interval, "3 " + context.getResources().getQuantityString(R.plurals.hours, 3));
        assertCustomAssertionAtPosition(R.id.reminderList, 3, R.id.reminderCardLayout, ViewAssertions.matches(ViewMatchers.withChild(ViewMatchers.withSubstring(expectedString))));

        clickListItemChild(R.id.reminderList, 0, R.id.openAdvancedSettings);
        pressBack();
        clickListItemChild(R.id.reminderList, 1, R.id.openAdvancedSettings);
        pressBack();
        clickListItemChild(R.id.reminderList, 2, R.id.openAdvancedSettings);
        pressBack();
        clickListItemChild(R.id.reminderList, 3, R.id.openAdvancedSettings);
        pressBack();

        // Check overview and next reminders
        navigateTo(OVERVIEW);

        assertContains(R.id.reminderText, "Test (1)");
        expectedString = TimeHelper.minutesToTimeString(context, reminder1Time.toSecondOfDay() / 60);
        assertContains(R.id.reminderText, expectedString);

        assertContains(R.id.reminderText, "Test (3)");

        assertContains(R.id.reminderText, "Test (4)");

        // If possible, take reminder 1 now and see if reminder 2 appears
        clickListItemChild(R.id.reminders, 1, R.id.stateButton);
        clickOn(R.id.takenButton);

        assertContains("Test (2)");
    }

    @Test
    //@AllowFlaky(attempts = 1)
    public void editReminderTest() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();

        AndroidTestHelper.createMedicine("Test");

        navigateTo(OVERVIEW);

        clickOn(R.id.logManualDose);

        clickListItem(1);

        writeTo(android.R.id.input, "12");
        clickDialogPositiveButton();
        long now = Instant.now().getEpochSecond();
        clickOn(com.google.android.material.R.id.material_timepicker_ok_button);

        clickListItemChild(R.id.reminders, 0, R.id.overviewContentContainer);
        assertContains(R.id.editEventName, "Test");
        assertContains(R.id.editEventAmount, "12");
        assertContains(R.id.editEventRemindedTimestamp, TimeHelper.secondsSinceEpochToTimeString(context, now));
        assertContains(R.id.editEventRemindedDate, TimeHelper.secondSinceEpochToDateString(context, now));
        assertContains(R.id.editEventTakenTimestamp, TimeHelper.secondsSinceEpochToTimeString(context, now));
        assertContains(R.id.editEventTakenDate, TimeHelper.secondSinceEpochToDateString(context, now));
        assertContains(R.id.editEventNotes, "");

        writeTo(R.id.editEventNotes, "Test notes");
        pressBack();

        clickListItemChild(R.id.reminders, 0, R.id.overviewContentContainer);
        assertContains(R.id.editEventNotes, "Test notes");

        long newReminded = now + 60 * 60 * 24 + 120;
        writeTo(R.id.editEventRemindedTimestamp, TimeHelper.secondsSinceEpochToTimeString(context, newReminded));
        writeTo(R.id.editEventRemindedDate, TimeHelper.secondSinceEpochToDateString(context, newReminded));

        long newTaken = now + 60 * 60 * 48 + 180;
        writeTo(R.id.editEventTakenTimestamp, TimeHelper.secondsSinceEpochToTimeString(context, newTaken));
        writeTo(R.id.editEventTakenDate, TimeHelper.secondSinceEpochToDateString(context, newTaken));

        pressBack();

        AndroidTestHelper.navigateTo(AndroidTestHelper.MainMenu.ANALYSIS);

        clickOn(R.id.tableChip);

        AtomicReference<TableView> tableView = new AtomicReference<>();
        tableView.set(baristaRule.getActivityTestRule().getActivity().findViewById(R.id.reminder_table));

        TextView view = tableView.get().getCellRecyclerView().findViewWithTag("time");
        assertEquals(TimeHelper.secondsSinceEpochToDateTimeString(context, newReminded), view.getText());
        view = tableView.get().getCellRecyclerView().findViewWithTag("taken");
        assertEquals(TimeHelper.secondsSinceEpochToDateTimeString(context, newTaken), view.getText());
    }

    @Test
    //@AllowFlaky(attempts = 1)
    public void deleteReminderTest() {
        AndroidTestHelper.createMedicine("Test");
        AndroidTestHelper.createReminder("1", LocalTime.of(20, 0));

        AndroidTestHelper.navigateTo(AndroidTestHelper.MainMenu.OVERVIEW);

        clickListItemChild(R.id.reminders, 0, R.id.stateButton);
        clickOn(R.id.takenButton);

        clickListItemChild(R.id.reminders, 0, R.id.stateButton);
        clickOn(R.id.deleteButton);
        clickDialogPositiveButton();

        assertListItemCount(R.id.reminders, 0);
    }

    @Test
    //@AllowFlaky(attempts = 1)
    public void intervalReminderTest() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();

        AndroidTestHelper.createMedicine("Test");
        AndroidTestHelper.createIntervalReminder("1", 10);
        pressBack();

        AndroidTestHelper.navigateTo(AndroidTestHelper.MainMenu.OVERVIEW);

        clickListItemChild(R.id.reminders, 0, R.id.stateButton);
        clickOn(R.id.takenButton);

        assertNotContains(context.getString(R.string.interval_time, "0 min"));

        clickListItemChild(R.id.reminders, 1, R.id.stateButton);
        clickOn(R.id.takenButton);

        sleep(1000);
        assertContains(context.getString(R.string.interval_time, "0 min"));
    }

    @Test
    //@AllowFlaky(attempts = 1)
    public void cyclicReminderTest() {
        CyclicReminderInfo[] reminders = {
                new CyclicReminderInfo(1, 0, false),
                new CyclicReminderInfo(1, 1, false),
                new CyclicReminderInfo(1, 2, false),
                new CyclicReminderInfo(2, 0, false),
                new CyclicReminderInfo(2, 1, true),
        };

        // Create medicine
        AndroidTestHelper.createMedicine("Test");

        for (CyclicReminderInfo reminder : reminders) {
            // Create reminder
            AndroidTestHelper.createReminder("1", LocalTime.of(20, 0));

            // Set active and pause days
            clickOn(R.id.openAdvancedSettings);
            clickOn(R.string.cycle_reminder);
            clickOn(R.string.cycle_consecutive_days);
            setValue(Integer.toString(reminder.consecutiveDays));
            clickOn(R.string.cycle_pause_days);
            setValue(Integer.toString(reminder.pauseDays));

            // Set cycle start date of the reminder
            Calendar cycleStart = Calendar.getInstance();
            // The month here is 7, not 8, since it is zero-indexed (so January is 0)
            cycleStart.set(2025, 7, 1);
            clickOn(R.string.cycle_start_date);
            setDate(cycleStart.getTime());

            // Go back to medicines list
            pressBack();
            pressBack();
            pressBack();

            // Mark event as taken
            AndroidTestHelper.navigateTo(AndroidTestHelper.MainMenu.OVERVIEW);
            clickOn(R.id.stateButton);
            clickOn(R.id.takenButton);

            // Check if cyclic information is present
            clickOn(R.id.overviewContentContainer);
            if (reminder.shouldHaveInfo) {
                assertContains(R.id.editEventName, String.format("Test (1/%d)", reminder.consecutiveDays));
            } else {
                assertNotContains(R.id.editEventName, "Test (");
                assertContains(R.id.editEventName, "Test");
            }
            pressBack();

            // Remove event
            clickOn(R.id.stateButton);
            clickOn(R.id.deleteButton);
            clickDialogPositiveButton();

            // Remove reminder
            AndroidTestHelper.navigateTo(AndroidTestHelper.MainMenu.MEDICINES);
            clickListItem(R.id.medicineList, 0);
            clickOn(R.id.openAdvancedSettings);
            openMenu();
            clickOn(R.string.delete);
            clickDialogPositiveButton();
        }
    }

    private static class CyclicReminderInfo {
        public int consecutiveDays;
        public int pauseDays;
        public boolean shouldHaveInfo;

        public CyclicReminderInfo(int consecutiveDays, int pauseDays, boolean shouldHaveInfo) {
            this.consecutiveDays = consecutiveDays;
            this.pauseDays = pauseDays;
            this.shouldHaveInfo = shouldHaveInfo;
        }
    }
}
