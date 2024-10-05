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

class ReminderSchedulerActivePeriodUnitTest {
    @Test
    void test_scheduleInactive() {
        ReminderScheduler.TimeAccess mockTimeAccess = mock(ReminderScheduler.TimeAccess.class);
        when(mockTimeAccess.systemZone()).thenReturn(ZoneId.of("Z"));
        when(mockTimeAccess.localDate()).thenReturn(LocalDate.EPOCH.plusDays(1));

        ReminderScheduler scheduler = new ReminderScheduler(mockTimeAccess);

        MedicineWithReminders medicineWithReminders = TestHelper.buildMedicineWithReminders(1, "Test");
        Reminder reminder = TestHelper.buildReminder(1, 1, "1", 480, 1);
        reminder.active = false;
        medicineWithReminders.reminders.add(reminder);

        List<MedicineWithReminders> medicineList = new ArrayList<>();
        medicineList.add(medicineWithReminders);

        List<ReminderEvent> reminderEventList = new ArrayList<>();

        List<ScheduledReminder> scheduledReminders = scheduler.schedule(medicineList, reminderEventList);
        assertEquals(0, scheduledReminders.size());
    }

    @Test
    void test_scheduleActive() {
        ReminderScheduler.TimeAccess mockTimeAccess = mock(ReminderScheduler.TimeAccess.class);
        when(mockTimeAccess.systemZone()).thenReturn(ZoneId.of("Z"));
        when(mockTimeAccess.localDate()).thenReturn(LocalDate.EPOCH);

        ReminderScheduler scheduler = new ReminderScheduler(mockTimeAccess);

        MedicineWithReminders medicineWithReminders = TestHelper.buildMedicineWithReminders(1, "Test");
        Reminder reminder = TestHelper.buildReminder(1, 1, "1", 480, 1);
        reminder.periodStart = 3;
        reminder.periodEnd = 4;
        medicineWithReminders.reminders.add(reminder);

        List<MedicineWithReminders> medicineList = new ArrayList<>();
        medicineList.add(medicineWithReminders);

        List<ReminderEvent> reminderEventList = new ArrayList<>();

        List<ScheduledReminder> scheduledReminders = scheduler.schedule(medicineList, reminderEventList);
        assertEquals(on(4, 480), scheduledReminders.get(0).timestamp());
        assertEquals(medicineWithReminders.medicine, scheduledReminders.get(0).medicine());
        assertEquals(reminder, scheduledReminders.get(0).reminder());

        when(mockTimeAccess.localDate()).thenReturn(LocalDate.EPOCH.plusDays(4));
        scheduledReminders = scheduler.schedule(medicineList, reminderEventList);
        assertEquals(on(5, 480), scheduledReminders.get(0).timestamp());

        when(mockTimeAccess.localDate()).thenReturn(LocalDate.EPOCH.plusDays(5));
        scheduledReminders = scheduler.schedule(medicineList, reminderEventList);
        assertEquals(0, scheduledReminders.size());
    }
}
