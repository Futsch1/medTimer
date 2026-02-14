package com.futsch1.medtimer.schedulertests;

import static com.futsch1.medtimer.schedulertests.ReminderSchedulerUnitTest.getScheduler;
import static com.futsch1.medtimer.schedulertests.TestHelper.on;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.futsch1.medtimer.database.FullMedicine;
import com.futsch1.medtimer.database.Reminder;
import com.futsch1.medtimer.database.ReminderEvent;
import com.futsch1.medtimer.reminders.scheduling.ReminderScheduler;
import com.futsch1.medtimer.reminders.scheduling.ScheduledReminder;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

class ReminderSchedulerActiveDayOfMonthUnitTest {
    @Test
    void scheduleDayOfMonth() {
        ReminderScheduler scheduler = getScheduler(1);

        FullMedicine medicineWithReminders = TestHelper.buildFullMedicine(1, "Test");
        Reminder reminder = TestHelper.buildReminder(1, 1, "1", 480, 1);
        reminder.activeDaysOfMonth = 0x7;
        medicineWithReminders.reminders.add(reminder);

        List<FullMedicine> medicineList = new ArrayList<>();
        medicineList.add(medicineWithReminders);

        List<ReminderEvent> reminderEventList = new ArrayList<>();

        List<ScheduledReminder> scheduledReminders = scheduler.schedule(medicineList, reminderEventList);
        assertEquals(1, scheduledReminders.size());
        assertEquals(on(2, 480), scheduledReminders.get(0).timestamp());

        when(scheduler.getTimeAccess().localDate()).thenReturn(LocalDate.EPOCH.plusDays(2));
        scheduledReminders = scheduler.schedule(medicineList, reminderEventList);
        assertEquals(1, scheduledReminders.size());
        assertEquals(on(3, 480), scheduledReminders.get(0).timestamp());

        when(scheduler.getTimeAccess().localDate()).thenReturn(LocalDate.EPOCH.plusDays(10));
        scheduledReminders = scheduler.schedule(medicineList, reminderEventList);
        assertEquals(1, scheduledReminders.size());
        assertEquals(on(32, 480), scheduledReminders.get(0).timestamp());
    }
}
