package com.futsch1.medtimer;

import static com.futsch1.medtimer.TestHelper.on;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.futsch1.medtimer.database.MedicineWithReminders;
import com.futsch1.medtimer.database.Reminder;
import com.futsch1.medtimer.database.ReminderEvent;
import com.futsch1.medtimer.reminders.scheduling.ReminderScheduler;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

class ReminderSchedulerActiveDayOfMonthUnitTest {
    @Test
    void test_scheduleDayOfMonth() {
        ReminderScheduler.TimeAccess mockTimeAccess = mock(ReminderScheduler.TimeAccess.class);
        when(mockTimeAccess.systemZone()).thenReturn(ZoneId.of("Z"));
        when(mockTimeAccess.localDate()).thenReturn(LocalDate.EPOCH.plusDays(1));

        ReminderScheduler scheduler = new ReminderScheduler(mockTimeAccess);

        MedicineWithReminders medicineWithReminders = TestHelper.buildMedicineWithReminders(1, "Test");
        Reminder reminder = TestHelper.buildReminder(1, 1, "1", 480, 1);
        reminder.activeDaysOfMonth = 0x7;
        medicineWithReminders.reminders.add(reminder);

        List<MedicineWithReminders> medicineList = new ArrayList<>();
        medicineList.add(medicineWithReminders);

        List<ReminderEvent> reminderEventList = new ArrayList<>();

        List<ScheduledReminder> scheduledReminders = scheduler.schedule(medicineList, reminderEventList);
        assertEquals(1, scheduledReminders.size());
        assertEquals(on(2, 480), scheduledReminders.get(0).timestamp());

        when(mockTimeAccess.localDate()).thenReturn(LocalDate.EPOCH.plusDays(2));
        scheduledReminders = scheduler.schedule(medicineList, reminderEventList);
        assertEquals(1, scheduledReminders.size());
        assertEquals(on(3, 480), scheduledReminders.get(0).timestamp());

        when(mockTimeAccess.localDate()).thenReturn(LocalDate.EPOCH.plusDays(10));
        scheduledReminders = scheduler.schedule(medicineList, reminderEventList);
        assertEquals(1, scheduledReminders.size());
        assertEquals(on(32, 480), scheduledReminders.get(0).timestamp());
    }
}
