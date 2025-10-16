package com.futsch1.medtimer;

import static com.futsch1.medtimer.TestHelper.assertReminded;
import static com.futsch1.medtimer.TestHelper.assertRemindedAtIndex;
import static com.futsch1.medtimer.TestHelper.on;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.SharedPreferences;

import com.futsch1.medtimer.database.FullMedicine;
import com.futsch1.medtimer.database.Reminder;
import com.futsch1.medtimer.database.ReminderEvent;
import com.futsch1.medtimer.reminders.scheduling.ReminderScheduler;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

class ReminderSchedulerUnitTest {

    public static final String TEST_1 = "Test1";
    public static final String TEST = "Test";
    public static final String TEST_2 = "Test2";

    @Test
    void testScheduleEmptyLists() {
        ReminderScheduler scheduler = getScheduler();

        // Two empty lists
        List<ScheduledReminder> scheduledReminders = scheduler.schedule(new ArrayList<>(), new ArrayList<>());
        assertTrue(scheduledReminders.isEmpty());

        // One medicine without reminders, no reminder events
        FullMedicine medicineWithReminders = TestHelper.buildFullMedicine(1, TEST);
        scheduledReminders = scheduler.schedule(new ArrayList<>() {{
            add(medicineWithReminders);
        }}, new ArrayList<>());
        assertTrue(scheduledReminders.isEmpty());

        // No reminder events
        Reminder reminder = TestHelper.buildReminder(1, 1, "1", 12, 1);
        reminder.createdTimestamp = on(1, 13).getEpochSecond();
        medicineWithReminders.reminders.add(reminder);
        scheduledReminders = scheduler.schedule(new ArrayList<>() {{
            add(medicineWithReminders);
        }}, new ArrayList<>());
        assertEquals(1, scheduledReminders.size());
        assertReminded(scheduledReminders, on(2, 12), medicineWithReminders.medicine, reminder);
    }

    public static ReminderScheduler getScheduler() {
        return getScheduler(0);
    }

    public static ReminderScheduler getScheduler(int plusDays) {
        ReminderScheduler.TimeAccess mockTimeAccess = mock(ReminderScheduler.TimeAccess.class);
        when(mockTimeAccess.systemZone()).thenReturn(ZoneId.of("Z"));
        when(mockTimeAccess.localDate()).thenReturn(LocalDate.EPOCH.plusDays(plusDays));
        SharedPreferences sharedPreferencesMock = mock(SharedPreferences.class);
        when(sharedPreferencesMock.getBoolean(anyString(), anyBoolean())).thenReturn(false);

        return new ReminderScheduler(mockTimeAccess, sharedPreferencesMock);
    }

    @Test
    void testScheduleReminders() {
        ReminderScheduler scheduler = getScheduler();

        FullMedicine medicineWithReminders1 = TestHelper.buildFullMedicine(1, TEST_1);
        Reminder reminder1 = TestHelper.buildReminder(1, 1, "1", 16, 1);
        Reminder reminder2 = TestHelper.buildReminder(1, 1, "2", 12, 1);
        medicineWithReminders1.reminders.add(reminder1);
        medicineWithReminders1.reminders.add(reminder2);
        List<ScheduledReminder> scheduledReminders = scheduler.schedule(new ArrayList<>() {{
            add(medicineWithReminders1);
        }}, new ArrayList<>());
        assertReminded(scheduledReminders, on(1, 12), medicineWithReminders1.medicine, reminder2);
        assertRemindedAtIndex(scheduledReminders, on(1, 16), medicineWithReminders1.medicine, reminder1, 1);

        // Now add a second medicine with an earlier reminder
        FullMedicine medicineWithReminders2 = TestHelper.buildFullMedicine(2, TEST_2);
        Reminder reminder3 = TestHelper.buildReminder(2, 1, "1", 3, 1);
        medicineWithReminders2.reminders.add(reminder3);
        scheduledReminders = scheduler.schedule(new ArrayList<>() {{
            add(medicineWithReminders1);
            add(medicineWithReminders2);
        }}, new ArrayList<>());
        assertReminded(scheduledReminders, on(1, 3), medicineWithReminders2.medicine, reminder3);
    }

    @Test
    void testScheduleWithEvents() {
        ReminderScheduler scheduler = getScheduler(1);

        FullMedicine medicineWithReminders1 = TestHelper.buildFullMedicine(1, TEST_1);
        Reminder reminder1 = TestHelper.buildReminder(1, 1, "1", 16, 1);
        Reminder reminder2 = TestHelper.buildReminder(1, 2, "2", 12, 1);
        medicineWithReminders1.reminders.add(reminder1);
        medicineWithReminders1.reminders.add(reminder2);
        FullMedicine medicineWithReminders2 = TestHelper.buildFullMedicine(2, TEST_2);
        Reminder reminder3 = TestHelper.buildReminder(2, 3, "1", 3, 1);
        medicineWithReminders2.reminders.add(reminder3);
        ArrayList<FullMedicine> medicineWithReminders = new ArrayList<>() {{
            add(medicineWithReminders1);
            add(medicineWithReminders2);
        }};
        // Reminder 3 already invoked
        List<ScheduledReminder> scheduledReminders = scheduler.schedule(medicineWithReminders, new ArrayList<>() {{
            add(TestHelper.buildReminderEvent(3, on(2, 3).getEpochSecond()));
        }});
        assertReminded(scheduledReminders, on(2, 12), medicineWithReminders1.medicine, reminder2);
        assertRemindedAtIndex(scheduledReminders, on(2, 16), medicineWithReminders1.medicine, reminder1, 1);
        assertRemindedAtIndex(scheduledReminders, on(3, 3), medicineWithReminders2.medicine, reminder3, 2);

        // Check two reminders at the same time
        Reminder reminder4 = TestHelper.buildReminder(2, 4, "1", 12, 1);
        medicineWithReminders2.reminders.add(reminder4);
        scheduledReminders = scheduler.schedule(medicineWithReminders, new ArrayList<>() {{
            add(TestHelper.buildReminderEvent(3, on(2, 3).getEpochSecond()));
            add(TestHelper.buildReminderEvent(2, on(2, 12).getEpochSecond()));
        }});
        assertReminded(scheduledReminders, on(2, 12), medicineWithReminders2.medicine, reminder4);

        scheduledReminders = scheduler.schedule(medicineWithReminders, new ArrayList<>() {{
            add(TestHelper.buildReminderEvent(3, on(2, 4).getEpochSecond()));
            add(TestHelper.buildReminderEvent(2, on(2, 12).getEpochSecond()));
            add(TestHelper.buildReminderEvent(4, on(2, 12).getEpochSecond()));
        }});
        assertReminded(scheduledReminders, on(2, 16), medicineWithReminders1.medicine, reminder1);

        // All reminders already invoked, switch to next day
        scheduledReminders = scheduler.schedule(medicineWithReminders, new ArrayList<>() {{
            add(TestHelper.buildReminderEvent(3, on(2, 4).getEpochSecond() + 4 * 60));
            add(TestHelper.buildReminderEvent(2, on(2, 12).getEpochSecond()));
            add(TestHelper.buildReminderEvent(1, on(2, 16).getEpochSecond()));
            add(TestHelper.buildReminderEvent(4, on(2, 16).getEpochSecond()));
        }});
        assertReminded(scheduledReminders, on(3, 3), medicineWithReminders2.medicine, reminder3);

        // All reminders already invoked, we are on the next day
        when(scheduler.getTimeAccess().localDate()).thenReturn(LocalDate.EPOCH.plusDays(2));
        scheduledReminders = scheduler.schedule(medicineWithReminders, new ArrayList<>() {{
            add(TestHelper.buildReminderEvent(3, on(2, 4).getEpochSecond()));
            add(TestHelper.buildReminderEvent(2, on(2, 12).getEpochSecond()));
            add(TestHelper.buildReminderEvent(1, on(2, 16).getEpochSecond()));
        }});
        assertReminded(scheduledReminders, on(3, 3), medicineWithReminders2.medicine, reminder3);
    }

    // schedules a reminder for the same day
    @Test
    void test_scheduleSameDayReminder() {
        ReminderScheduler scheduler = getScheduler();

        FullMedicine medicineWithReminders = TestHelper.buildFullMedicine(1, TEST);
        Reminder reminder = TestHelper.buildReminder(1, 1, "1", 480, 1);
        reminder.createdTimestamp = on(1, 500).getEpochSecond();
        medicineWithReminders.reminders.add(reminder);

        List<FullMedicine> medicineList = new ArrayList<>();
        medicineList.add(medicineWithReminders);

        List<ReminderEvent> reminderEventList = new ArrayList<>();

        List<ScheduledReminder> scheduledReminders = scheduler.schedule(medicineList, reminderEventList);

        assertReminded(scheduledReminders, on(2, 480), medicineWithReminders.medicine, reminder);
    }

    // schedules a reminder for a different medicine
    @Test
    void testScheduleDifferentMedicineReminder() {
        ReminderScheduler scheduler = getScheduler(1);

        FullMedicine medicineWithReminders1 = TestHelper.buildFullMedicine(1, TEST_1);
        Reminder reminder1 = TestHelper.buildReminder(1, 1, "1", 480, 1);
        medicineWithReminders1.reminders.add(reminder1);

        FullMedicine medicineWithReminders2 = TestHelper.buildFullMedicine(2, TEST_2);
        Reminder reminder2 = TestHelper.buildReminder(2, 2, "2", 480, 1);
        medicineWithReminders2.reminders.add(reminder2);

        List<FullMedicine> medicineList = new ArrayList<>();
        medicineList.add(medicineWithReminders1);
        medicineList.add(medicineWithReminders2);

        List<ReminderEvent> reminderEventList = new ArrayList<>();

        List<ScheduledReminder> scheduledReminders = scheduler.schedule(medicineList, reminderEventList);

        assertReminded(scheduledReminders, on(2, 480), medicineWithReminders1.medicine, reminder1);
    }

    // schedules a reminder for every two days
    @Test
    void testScheduleReminderWithOneDayPause() {
        ReminderScheduler scheduler = getScheduler();

        FullMedicine medicineWithReminders = TestHelper.buildFullMedicine(1, TEST);
        Reminder reminder = TestHelper.buildReminder(1, 1, "1", 480, 2);
        medicineWithReminders.reminders.add(reminder);

        List<FullMedicine> medicineList = new ArrayList<>();
        medicineList.add(medicineWithReminders);

        List<ReminderEvent> reminderEventList = new ArrayList<>();

        List<ScheduledReminder> scheduledReminders = scheduler.schedule(medicineList, reminderEventList);
        assertReminded(scheduledReminders, on(1, 480), medicineWithReminders.medicine, reminder);

        reminderEventList.add(TestHelper.buildReminderEvent(1, on(1, 480).getEpochSecond()));
        when(scheduler.getTimeAccess().localDate()).thenReturn(LocalDate.EPOCH.plusDays(1));

        scheduledReminders = scheduler.schedule(medicineList, reminderEventList);
        assertReminded(scheduledReminders, on(3, 480), medicineWithReminders.medicine, reminder);
    }

    // schedules a reminder for every two days
    @Test
    void testScheduleTwoDayReminderVsOneDay() {
        ReminderScheduler scheduler = getScheduler();

        FullMedicine medicineWithReminders = TestHelper.buildFullMedicine(1, TEST);
        Reminder reminder = TestHelper.buildReminder(1, 1, "1", 480, 2);
        medicineWithReminders.reminders.add(reminder);
        Reminder reminder2 = TestHelper.buildReminder(1, 2, "2", 481, 1);
        medicineWithReminders.reminders.add(reminder2);

        List<FullMedicine> medicineList = new ArrayList<>();
        medicineList.add(medicineWithReminders);

        List<ReminderEvent> reminderEventList = new ArrayList<>();
        reminderEventList.add(TestHelper.buildReminderEvent(1, on(1, 480).getEpochSecond()));

        when(scheduler.getTimeAccess().localDate()).thenReturn(LocalDate.EPOCH.plusDays(1));

        List<ScheduledReminder> scheduledReminders = scheduler.schedule(medicineList, reminderEventList);
        assertReminded(scheduledReminders, on(2, 481), medicineWithReminders.medicine, reminder2);
    }

    @Test
    void testScheduleReminderWithOneDayPauseVsThreeDaysPause() {
        ReminderScheduler scheduler = getScheduler();

        FullMedicine medicineWithReminders = TestHelper.buildFullMedicine(1, TEST);
        Reminder reminder = TestHelper.buildReminder(1, 1, "1", 480, 2);
        medicineWithReminders.reminders.add(reminder);
        Reminder reminder2 = TestHelper.buildReminder(1, 2, "2", 481, 4);
        medicineWithReminders.reminders.add(reminder2);

        List<FullMedicine> medicineList = new ArrayList<>();
        medicineList.add(medicineWithReminders);

        List<ReminderEvent> reminderEventList = new ArrayList<>();
        reminderEventList.add(TestHelper.buildReminderEvent(1, on(1, 480).getEpochSecond()));

        List<ScheduledReminder> scheduledReminders = scheduler.schedule(medicineList, reminderEventList);
        assertReminded(scheduledReminders, on(1, 481), medicineWithReminders.medicine, reminder2);

        reminderEventList.add(TestHelper.buildReminderEvent(2, on(1, 481).getEpochSecond()));

        scheduledReminders = scheduler.schedule(medicineList, reminderEventList);
        assertReminded(scheduledReminders, on(3, 480), medicineWithReminders.medicine, reminder);

        // On day 3
        when(scheduler.getTimeAccess().localDate()).thenReturn(LocalDate.EPOCH.plusDays(2));

        scheduledReminders = scheduler.schedule(medicineList, reminderEventList);
        assertReminded(scheduledReminders, on(3, 480), medicineWithReminders.medicine, reminder);

        reminderEventList.add(TestHelper.buildReminderEvent(1, on(3, 480).getEpochSecond()));

        scheduledReminders = scheduler.schedule(medicineList, reminderEventList);
        assertReminded(scheduledReminders, on(5, 480), medicineWithReminders.medicine, reminder);

        // On day 5
        when(scheduler.getTimeAccess().localDate()).thenReturn(LocalDate.EPOCH.plusDays(4));

        reminderEventList.add(TestHelper.buildReminderEvent(1, on(5, 480).getEpochSecond()));

        scheduledReminders = scheduler.schedule(medicineList, reminderEventList);
        assertReminded(scheduledReminders, on(5, 481), medicineWithReminders.medicine, reminder2);
    }

    @Test
    void testScheduleCycleInFuture() {
        ReminderScheduler scheduler = getScheduler();

        FullMedicine medicineWithReminders = TestHelper.buildFullMedicine(1, TEST);
        Reminder reminder = TestHelper.buildReminder(1, 1, "1", 480, 3);
        reminder.cycleStartDay = 4;
        medicineWithReminders.reminders.add(reminder);

        List<FullMedicine> medicineList = new ArrayList<>();
        medicineList.add(medicineWithReminders);

        List<ReminderEvent> reminderEventList = new ArrayList<>();

        List<ScheduledReminder> scheduledReminders = scheduler.schedule(medicineList, reminderEventList);
        assertReminded(scheduledReminders, on(5, 480), medicineWithReminders.medicine, reminder);

        when(scheduler.getTimeAccess().localDate()).thenReturn(LocalDate.EPOCH.plusDays(4));

        scheduledReminders = scheduler.schedule(medicineList, reminderEventList);
        assertReminded(scheduledReminders, on(5, 480), medicineWithReminders.medicine, reminder);

        when(scheduler.getTimeAccess().localDate()).thenReturn(LocalDate.EPOCH.plusDays(5));

        scheduledReminders = scheduler.schedule(medicineList, reminderEventList);
        assertReminded(scheduledReminders, on(8, 480), medicineWithReminders.medicine, reminder);

        // Reminder already scheduled for tomorrow
        when(scheduler.getTimeAccess().localDate()).thenReturn(LocalDate.EPOCH.plusDays(6));
        reminderEventList.add(TestHelper.buildReminderEvent(1, on(8, 480).getEpochSecond()));

        scheduledReminders = scheduler.schedule(medicineList, reminderEventList);
        assertReminded(scheduledReminders, on(11, 480), medicineWithReminders.medicine, reminder);
    }


    @Test
    void testReminderOverMidnight() {
        ReminderScheduler scheduler = getScheduler();

        FullMedicine medicineWithReminders = TestHelper.buildFullMedicine(1, TEST);
        Reminder reminder = TestHelper.buildReminder(1, 1, "1", 23 * 60 + 45, 1);
        medicineWithReminders.reminders.add(reminder);

        List<FullMedicine> medicineList = new ArrayList<>();
        medicineList.add(medicineWithReminders);

        List<ReminderEvent> reminderEventList = new ArrayList<>();
        ReminderEvent reminderEvent = TestHelper.buildReminderEvent(1, on(1, 23 * 60 + 46).getEpochSecond());
        reminderEvent.processedTimestamp = on(2, 1).getEpochSecond();
        reminderEventList.add(reminderEvent);

        List<ScheduledReminder> scheduledReminders = scheduler.schedule(medicineList, reminderEventList);
        assertReminded(scheduledReminders, on(2, 23 * 60 + 45), medicineWithReminders.medicine, reminder);
    }

    @Test
    void test_reminderTomorrow() {
        ReminderScheduler scheduler = getScheduler();

        FullMedicine medicineWithReminders1 = TestHelper.buildFullMedicine(1, TEST_1);
        Reminder reminder1 = TestHelper.buildReminder(1, 1, "1", 16, 1);
        medicineWithReminders1.reminders.add(reminder1);

        List<FullMedicine> medicines = new ArrayList<>() {{
            add(medicineWithReminders1);
        }};

        List<ReminderEvent> reminderEventList = new ArrayList<>();
        reminderEventList.add(TestHelper.buildReminderEvent(1, on(1, 16).getEpochSecond()));
        reminderEventList.add(TestHelper.buildReminderEvent(1, on(2, 16).getEpochSecond()));

        List<ScheduledReminder> scheduledReminders = scheduler.schedule(medicines, reminderEventList);
        assertReminded(scheduledReminders, on(3, 16), medicineWithReminders1.medicine, reminder1);

    }
}