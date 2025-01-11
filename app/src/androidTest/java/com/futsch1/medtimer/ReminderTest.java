package com.futsch1.medtimer;


import static androidx.test.espresso.Espresso.pressBack;
import static com.adevinta.android.barista.assertion.BaristaHintAssertions.assertHint;
import static com.adevinta.android.barista.assertion.BaristaListAssertions.assertDisplayedAtPosition;
import static com.adevinta.android.barista.assertion.BaristaListAssertions.assertListItemCount;
import static com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertContains;
import static com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertNotDisplayed;
import static com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn;
import static com.adevinta.android.barista.interaction.BaristaDialogInteractions.clickDialogPositiveButton;
import static com.adevinta.android.barista.interaction.BaristaEditTextInteractions.clearText;
import static com.adevinta.android.barista.interaction.BaristaEditTextInteractions.typeTo;
import static com.adevinta.android.barista.interaction.BaristaEditTextInteractions.writeTo;
import static com.adevinta.android.barista.interaction.BaristaListInteractions.clickListItem;
import static com.adevinta.android.barista.interaction.BaristaListInteractions.clickListItemChild;
import static com.adevinta.android.barista.interaction.BaristaMenuClickInteractions.openMenu;
import static com.futsch1.medtimer.AndroidTestHelper.MainMenu.OVERVIEW;
import static com.futsch1.medtimer.AndroidTestHelper.navigateTo;
import static com.futsch1.medtimer.AndroidTestHelper.setTime;

import android.content.Context;

import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;

import com.futsch1.medtimer.helpers.TimeHelper;

import org.junit.Test;

import java.time.LocalTime;
import java.util.Calendar;

@LargeTest
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
        AndroidTestHelper.createReminder("1", null);

        clickOn(R.id.open_advanced_settings);
        clickOn(R.id.inactive);

        pressBack();
        pressBack();
        AndroidTestHelper.navigateTo(AndroidTestHelper.MainMenu.OVERVIEW);
        assertNotDisplayed(R.id.nextReminders);

        AndroidTestHelper.navigateTo(AndroidTestHelper.MainMenu.MEDICINES);
        clickListItem(R.id.medicineList, 0);
        clickOn(R.id.open_advanced_settings);
        clickOn(R.id.active);

        pressBack();
        pressBack();
        AndroidTestHelper.navigateTo(AndroidTestHelper.MainMenu.OVERVIEW);
        assertListItemCount(R.id.nextReminders, 1);

        AndroidTestHelper.navigateTo(AndroidTestHelper.MainMenu.MEDICINES);
        clickListItem(R.id.medicineList, 0);
        clickOn(R.id.open_advanced_settings);
        clickOn(R.id.timePeriod);
        clickOn(R.id.periodStart);
        writeTo(R.id.periodStartDate, AndroidTestHelper.dateToString(futureTime.getTime()));

        pressBack();
        pressBack();
        AndroidTestHelper.navigateTo(AndroidTestHelper.MainMenu.OVERVIEW);
        assertNotDisplayed(R.id.nextReminders);

        AndroidTestHelper.navigateTo(AndroidTestHelper.MainMenu.MEDICINES);
        clickListItem(R.id.medicineList, 0);
        clickOn(R.id.open_advanced_settings);
        clickOn(R.id.periodStart);
        clickOn(R.id.periodEnd);
        writeTo(R.id.periodEndDate, AndroidTestHelper.dateToString(pastTime.getTime()));

        pressBack();
        pressBack();
        AndroidTestHelper.navigateTo(AndroidTestHelper.MainMenu.OVERVIEW);
        assertNotDisplayed(R.id.nextReminders);

        AndroidTestHelper.navigateTo(AndroidTestHelper.MainMenu.MEDICINES);
        clickListItem(R.id.medicineList, 0);
        clickOn(R.id.open_advanced_settings);
        clickOn(R.id.periodStart);
        writeTo(R.id.periodStartDate, AndroidTestHelper.dateToString(pastTime.getTime()));
        writeTo(R.id.periodEndDate, AndroidTestHelper.dateToString(futureTime.getTime()));

        pressBack();
        pressBack();
        AndroidTestHelper.navigateTo(AndroidTestHelper.MainMenu.OVERVIEW);
        assertListItemCount(R.id.nextReminders, 1);
    }

    @Test
    //@AllowFlaky(attempts = 1)
    public void deleteLinkedReminderTest() {
        AndroidTestHelper.createMedicine("Test med");
        AndroidTestHelper.createReminder("1", LocalTime.of(0, 0));

        clickOn(R.id.open_advanced_settings);

        clickOn(R.id.addLinkedReminder);
        clickDialogPositiveButton();
        setTime(0, 1, true);

        clickListItemChild(R.id.reminderList, 1, R.id.open_advanced_settings);

        clickOn(R.id.addLinkedReminder);
        clickDialogPositiveButton();
        setTime(0, 2, true);

        clickListItemChild(R.id.reminderList, 0, R.id.open_advanced_settings);

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
        clickOn(R.id.open_advanced_settings);
        clickOn(R.id.addLinkedReminder);
        writeTo(android.R.id.input, "2");
        clickDialogPositiveButton();

        setTime(0, 30, true);

        // Interval reminder (amount 3) 2 hours from now
        clickOn(R.id.addReminder);
        clearText(R.id.editAmount);
        typeTo(R.id.editAmount, "3");
        clickOn(R.id.intervalBased);
        clickOn(R.id.intervalHours);
        clearText(R.id.editIntervalTime);
        typeTo(R.id.editIntervalTime, "2");
        clickOn(R.id.createReminder);

        // Check calendar view not crashing
        clickOn(R.id.openCalendar);
        pressBack();

        // Check reminder list
        int positionOfReminder1 = reminder1Time.isBefore(LocalTime.of(2, 0)) ? 0 : 1;
        int positionOfReminder2 = reminder1Time.isBefore(LocalTime.of(1, 30)) ? 1 : 2;
        int positionOfReminder3 = reminder1Time.isBefore(LocalTime.of(2, 0)) ? (reminder1Time.isBefore(LocalTime.of(1, 30)) ? 2 : 1) : 0;

        String expectedString = context.getString(R.string.every_interval, "2 " + context.getResources().getQuantityString(R.plurals.hours, 2));
        assertDisplayedAtPosition(R.id.reminderList, positionOfReminder3, R.id.reminderCardLayout, expectedString);

        assertHint(R.id.editReminderTime, R.string.time);
        expectedString = TimeHelper.minutesToTimeString(context, reminder1Time.toSecondOfDay() / 60);
        assertDisplayedAtPosition(R.id.reminderList, positionOfReminder1, R.id.editReminderTime, expectedString);

        assertHint(R.id.editReminderTime, R.string.delay);
        expectedString = context.getString(R.string.linked_reminder_summary, TimeHelper.minutesToTimeString(context, reminder1Time.toSecondOfDay() / 60));
        assertDisplayedAtPosition(R.id.reminderList, positionOfReminder2, R.id.reminderCardLayout, expectedString);

        clickListItemChild(R.id.reminderList, positionOfReminder1, R.id.open_advanced_settings);
        pressBack();
        clickListItemChild(R.id.reminderList, positionOfReminder2, R.id.open_advanced_settings);
        pressBack();
        clickListItemChild(R.id.reminderList, positionOfReminder3, R.id.open_advanced_settings);
        pressBack();

        // Check overview and next reminders
        navigateTo(OVERVIEW);

        expectedString = context.getString(R.string.reminder_event, "1", "Test", "");
        assertContains(R.id.nextReminderText, expectedString);
        expectedString = TimeHelper.minutesToTimeString(context, reminder1Time.toSecondOfDay() / 60);
        assertContains(R.id.nextReminderText, expectedString);

        UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        if (device.getDisplayWidth() < device.getDisplayHeight()) {
            clickOn(R.id.expandNextReminders);
        }

        expectedString = context.getString(R.string.reminder_event, "3", "Test", "");
        assertContains(R.id.nextReminderText, expectedString);

        // If possible, take reminder 1 now and see if reminder 2 appears
        if (reminder1Time.isAfter(LocalTime.of(0, 30))) {
            clickListItemChild(R.id.nextReminders, 0, R.id.takenNow);

            expectedString = context.getString(R.string.reminder_event, "2", "Test", "");
            assertContains(R.id.nextReminderText, expectedString);
        }
    }
}
