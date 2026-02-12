package com.futsch1.medtimer.schedulerTests;

import static com.futsch1.medtimer.schedulerTests.ReminderSchedulerUnitTest.getScheduler;
import static com.futsch1.medtimer.schedulerTests.TestHelper.on;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.futsch1.medtimer.database.FullMedicine;
import com.futsch1.medtimer.database.Reminder;
import com.futsch1.medtimer.database.ReminderEvent;
import com.futsch1.medtimer.reminders.scheduling.ReminderScheduler;
import com.futsch1.medtimer.reminders.scheduling.ScheduledReminder;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

class ReminderSchedulerDaysUnitTest {
    @Test
    void testScheduleSkipWeekdays() {
        ReminderScheduler scheduler = getScheduler(1);

        FullMedicine medicineWithReminders = TestHelper.buildFullMedicine(1, "Test");
        Reminder reminder = TestHelper.buildReminder(1, 1, "1", 480, 1);
        // 1.1.1970 was a Thursday, so skip the Friday and Saturday
        reminder.days.set(4, false);
        reminder.days.set(5, false);
        medicineWithReminders.reminders.add(reminder);

        List<FullMedicine> medicineList = new ArrayList<>();
        medicineList.add(medicineWithReminders);

        List<ReminderEvent> reminderEventList = new ArrayList<>();

        List<ScheduledReminder> scheduledReminders = scheduler.schedule(medicineList, reminderEventList);
        // Expect it to be on the 4.1.1970
        assertEquals(on(4, 480), scheduledReminders.get(0).timestamp());
        assertEquals(medicineWithReminders.medicine, scheduledReminders.get(0).medicine().medicine);
        assertEquals(reminder, scheduledReminders.get(0).reminder());
    }

    @Test
    void test_scheduleWeekdaysWithDaysBetweenReminders() {
        ReminderScheduler scheduler = getScheduler();

        FullMedicine medicineWithReminders = TestHelper.buildFullMedicine(1, "Test");
        Reminder reminder = TestHelper.buildReminder(1, 1, "1", 480, 6);
        // Allow only on Mondays and only every 6 days. The start of the cycle will be on the 1.1.1970.
        reminder.days.set(1, false);
        reminder.days.set(2, false);
        reminder.days.set(3, false);
        reminder.days.set(4, false);
        reminder.days.set(5, false);
        reminder.days.set(6, false);
        medicineWithReminders.reminders.add(reminder);

        List<FullMedicine> medicineList = new ArrayList<>();
        medicineList.add(medicineWithReminders);

        List<ReminderEvent> reminderEventList = new ArrayList<>();

        List<ScheduledReminder> scheduledReminders = scheduler.schedule(medicineList, reminderEventList);
        // Expect it to be on the 26.1.1970
        assertEquals(on(19, 480), scheduledReminders.get(0).timestamp());
        assertEquals(medicineWithReminders.medicine, scheduledReminders.get(0).medicine().medicine);
        assertEquals(reminder, scheduledReminders.get(0).reminder());
    }
}
