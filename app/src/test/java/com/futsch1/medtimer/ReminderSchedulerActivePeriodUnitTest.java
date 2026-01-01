package com.futsch1.medtimer;

import static com.futsch1.medtimer.ReminderSchedulerUnitTest.getScheduler;
import static com.futsch1.medtimer.TestHelper.on;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.futsch1.medtimer.database.FullMedicine;
import com.futsch1.medtimer.database.Reminder;
import com.futsch1.medtimer.database.ReminderEvent;
import com.futsch1.medtimer.reminders.scheduling.ReminderScheduler;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

class ReminderSchedulerActivePeriodUnitTest {
    @Test
    void testScheduleInactive() {
        ReminderScheduler scheduler = getScheduler(1);

        FullMedicine medicineWithReminders = TestHelper.buildFullMedicine(1, "Test");
        Reminder reminder = TestHelper.buildReminder(1, 1, "1", 480, 1);
        reminder.active = false;
        medicineWithReminders.reminders.add(reminder);

        List<FullMedicine> medicineList = new ArrayList<>();
        medicineList.add(medicineWithReminders);

        List<ReminderEvent> reminderEventList = new ArrayList<>();

        List<ScheduledReminder> scheduledReminders = scheduler.schedule(medicineList, reminderEventList);
        assertEquals(0, scheduledReminders.size());
    }

    @Test
    void test_scheduleActive() {
        ReminderScheduler scheduler = getScheduler();

        FullMedicine medicineWithReminders = TestHelper.buildFullMedicine(1, "Test");
        Reminder reminder = TestHelper.buildReminder(1, 1, "1", 480, 1);
        reminder.periodStart = 3;
        reminder.periodEnd = 4;
        medicineWithReminders.reminders.add(reminder);

        List<FullMedicine> medicineList = new ArrayList<>();
        medicineList.add(medicineWithReminders);

        List<ReminderEvent> reminderEventList = new ArrayList<>();

        List<ScheduledReminder> scheduledReminders = scheduler.schedule(medicineList, reminderEventList);
        assertEquals(on(4, 480), scheduledReminders.get(0).timestamp());
        assertEquals(medicineWithReminders.medicine, scheduledReminders.get(0).medicine().medicine);
        assertEquals(reminder, scheduledReminders.get(0).reminder());

        when(scheduler.getTimeAccess().localDate()).thenReturn(LocalDate.EPOCH.plusDays(4));
        scheduledReminders = scheduler.schedule(medicineList, reminderEventList);
        assertEquals(on(5, 480), scheduledReminders.get(0).timestamp());

        when(scheduler.getTimeAccess().localDate()).thenReturn(LocalDate.EPOCH.plusDays(5));
        scheduledReminders = scheduler.schedule(medicineList, reminderEventList);
        assertEquals(0, scheduledReminders.size());
    }

    @Test
    void test_scheduleActiveInterval() {
        ReminderScheduler scheduler = getScheduler();

        FullMedicine medicineWithReminders = TestHelper.buildFullMedicine(1, "Test");
        Reminder reminder = TestHelper.buildReminder(1, 1, "1", 480, 1);
        reminder.intervalStart = 60;
        reminder.periodStart = 3;
        reminder.periodEnd = 4;
        medicineWithReminders.reminders.add(reminder);

        List<FullMedicine> medicineList = new ArrayList<>();
        medicineList.add(medicineWithReminders);

        List<ReminderEvent> reminderEventList = new ArrayList<>();

        List<ScheduledReminder> scheduledReminders = scheduler.schedule(medicineList, reminderEventList);
        assertEquals(on(4, 1), scheduledReminders.get(0).timestamp());
        assertEquals(medicineWithReminders.medicine, scheduledReminders.get(0).medicine().medicine);
        assertEquals(reminder, scheduledReminders.get(0).reminder());

        when(scheduler.getTimeAccess().localDate()).thenReturn(LocalDate.EPOCH.plusDays(4));
        scheduledReminders = scheduler.schedule(medicineList, reminderEventList);
        assertEquals(on(5, 1), scheduledReminders.get(0).timestamp());

        when(scheduler.getTimeAccess().localDate()).thenReturn(LocalDate.EPOCH.plusDays(5));
        scheduledReminders = scheduler.schedule(medicineList, reminderEventList);
        assertEquals(0, scheduledReminders.size());
    }

    @Test
    void test_scheduleActiveWindowedInterval() {
        ReminderScheduler scheduler = getScheduler();

        FullMedicine medicineWithReminders = TestHelper.buildFullMedicine(1, "Test");
        Reminder reminder = TestHelper.buildReminder(1, 1, "1", 480, 1);
        reminder.intervalStart = 1;
        reminder.windowedInterval = true;
        reminder.intervalStartsFromProcessed = false;
        reminder.intervalStartTimeOfDay = 120;
        reminder.intervalEndTimeOfDay = 700;
        reminder.periodStart = 3;
        reminder.periodEnd = 4;
        medicineWithReminders.reminders.add(reminder);

        List<FullMedicine> medicineList = new ArrayList<>();
        medicineList.add(medicineWithReminders);

        List<ReminderEvent> reminderEventList = new ArrayList<>();

        List<ScheduledReminder> scheduledReminders = scheduler.schedule(medicineList, reminderEventList);
        assertEquals(on(4, 120), scheduledReminders.get(0).timestamp());
        assertEquals(medicineWithReminders.medicine, scheduledReminders.get(0).medicine().medicine);
        assertEquals(reminder, scheduledReminders.get(0).reminder());

        when(scheduler.getTimeAccess().localDate()).thenReturn(LocalDate.EPOCH.plusDays(4));
        scheduledReminders = scheduler.schedule(medicineList, reminderEventList);
        assertEquals(on(5, 120), scheduledReminders.get(0).timestamp());

        when(scheduler.getTimeAccess().localDate()).thenReturn(LocalDate.EPOCH.plusDays(5));
        scheduledReminders = scheduler.schedule(medicineList, reminderEventList);
        assertEquals(0, scheduledReminders.size());
    }
}
