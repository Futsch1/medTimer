package com.futsch1.medtimer;

import static com.futsch1.medtimer.TestHelper.assertReminded;
import static com.futsch1.medtimer.TestHelper.on;
import static com.futsch1.medtimer.TestHelper.onTZ;
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

class ReminderSchedulerTimezoneUnitTest {

    @Test
    void test_scheduleWithEvents() {
        ReminderScheduler.TimeAccess mockTimeAccess = mock(ReminderScheduler.TimeAccess.class);
        when(mockTimeAccess.localDate()).thenReturn(LocalDate.EPOCH.plusDays(1));

        ReminderScheduler scheduler = new ReminderScheduler(mockTimeAccess);

        MedicineWithReminders medicineWithReminders1 = TestHelper.buildMedicineWithReminders(1, "Test1");
        Reminder reminder1 = TestHelper.buildReminder(1, 1, "1", 1, 1);
        medicineWithReminders1.reminders.add(reminder1);
        ArrayList<MedicineWithReminders> medicineWithReminders = new ArrayList<>() {{
            add(medicineWithReminders1);
        }};
        ArrayList<ReminderEvent> reminderEvents = new ArrayList<>() {{
            add(TestHelper.buildReminderEvent(1, on(1, 1).getEpochSecond()));
        }};
        when(mockTimeAccess.systemZone()).thenReturn(ZoneId.of("CET"));
        List<ScheduledReminder> scheduledReminders = scheduler.schedule(medicineWithReminders, reminderEvents);
        assertReminded(scheduledReminders, onTZ(2, 1, "CET"), medicineWithReminders1.medicine, reminder1);

        when(mockTimeAccess.systemZone()).thenReturn(ZoneId.of("America/New_York"));
        scheduledReminders = scheduler.schedule(medicineWithReminders, reminderEvents);
        assertReminded(scheduledReminders, onTZ(2, 1, "America/New_York"), medicineWithReminders1.medicine, reminder1);

        when(mockTimeAccess.systemZone()).thenReturn(ZoneId.of("Asia/Kolkata"));
        scheduledReminders = scheduler.schedule(medicineWithReminders, reminderEvents);
        assertReminded(scheduledReminders, onTZ(2, 1, "Asia/Kolkata"), medicineWithReminders1.medicine, reminder1);
    }
}