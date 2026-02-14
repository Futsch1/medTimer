package com.futsch1.medtimer.schedulertests;

import static com.futsch1.medtimer.schedulertests.ReminderSchedulerUnitTest.getScheduler;
import static com.futsch1.medtimer.schedulertests.TestHelper.assertReminded;
import static com.futsch1.medtimer.schedulertests.TestHelper.on;
import static com.futsch1.medtimer.schedulertests.TestHelper.onTZ;
import static org.mockito.Mockito.when;

import com.futsch1.medtimer.database.FullMedicine;
import com.futsch1.medtimer.database.Reminder;
import com.futsch1.medtimer.database.ReminderEvent;
import com.futsch1.medtimer.reminders.scheduling.ReminderScheduler;
import com.futsch1.medtimer.reminders.scheduling.ScheduledReminder;

import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

class ReminderSchedulerTimezoneUnitTest {

    @Test
    void scheduleWithEvents() {
        ReminderScheduler scheduler = getScheduler(1);

        FullMedicine medicineWithReminders1 = TestHelper.buildFullMedicine(1, "Test1");
        Reminder reminder1 = TestHelper.buildReminder(1, 1, "1", 1, 1);
        medicineWithReminders1.reminders.add(reminder1);
        ArrayList<FullMedicine> medicineWithReminders = new ArrayList<>() {{
            add(medicineWithReminders1);
        }};
        ArrayList<ReminderEvent> reminderEvents = new ArrayList<>() {{
            add(TestHelper.buildReminderEvent(1, on(1, 1).getEpochSecond()));
        }};
        when(scheduler.getTimeAccess().systemZone()).thenReturn(ZoneId.of("CET"));
        List<ScheduledReminder> scheduledReminders = scheduler.schedule(medicineWithReminders, reminderEvents);
        assertReminded(scheduledReminders, onTZ(2, 1, "CET"), medicineWithReminders1.medicine, reminder1);

        when(scheduler.getTimeAccess().systemZone()).thenReturn(ZoneId.of("America/New_York"));
        scheduledReminders = scheduler.schedule(medicineWithReminders, reminderEvents);
        assertReminded(scheduledReminders, onTZ(2, 1, "America/New_York"), medicineWithReminders1.medicine, reminder1);

        when(scheduler.getTimeAccess().systemZone()).thenReturn(ZoneId.of("Asia/Kolkata"));
        scheduledReminders = scheduler.schedule(medicineWithReminders, reminderEvents);
        assertReminded(scheduledReminders, onTZ(2, 1, "Asia/Kolkata"), medicineWithReminders1.medicine, reminder1);
    }
}