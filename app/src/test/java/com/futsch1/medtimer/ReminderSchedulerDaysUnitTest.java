package com.futsch1.medtimer;

import static com.futsch1.medtimer.TestHelper.on;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.futsch1.medtimer.database.MedicineWithReminders;
import com.futsch1.medtimer.database.Reminder;
import com.futsch1.medtimer.database.ReminderEvent;
import com.futsch1.medtimer.reminders.scheduling.ReminderScheduler;

import org.junit.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

public class ReminderSchedulerDaysUnitTest {
    @Test
    public void test_scheduleSkipWeekdays() {
        ReminderScheduler.NextReminderReceiver mock = mock(ReminderScheduler.NextReminderReceiver.class);
        ReminderScheduler.TimeAccess mockTimeAccess = mock(ReminderScheduler.TimeAccess.class);
        when(mockTimeAccess.systemZone()).thenReturn(ZoneId.of("Z"));
        when(mockTimeAccess.localDate()).thenReturn(LocalDate.EPOCH.plusDays(1));

        ReminderScheduler scheduler = new ReminderScheduler(mock, mockTimeAccess);

        MedicineWithReminders medicineWithReminders = TestHelper.buildMedicineWithReminders(1, "Test");
        Reminder reminder = TestHelper.buildReminder(1, 1, "1", 480, 1);
        // 1.1.1970 was a Thursday, so skip the Friday and Saturday
        reminder.days.set(4, false);
        reminder.days.set(5, false);
        medicineWithReminders.reminders.add(reminder);

        List<MedicineWithReminders> medicineList = new ArrayList<>();
        medicineList.add(medicineWithReminders);

        List<ReminderEvent> reminderEventList = new ArrayList<>();

        scheduler.schedule(medicineList, reminderEventList);
        // Expect it to be on the 4.1.1970
        verify(mock, times(1)).onNextReminder(on(4, 480), medicineWithReminders.medicine, reminder);
    }

    @Test
    public void test_scheduleWeekdaysWithDaysBetweenReminders() {
        ReminderScheduler.NextReminderReceiver mock = mock(ReminderScheduler.NextReminderReceiver.class);
        ReminderScheduler.TimeAccess mockTimeAccess = mock(ReminderScheduler.TimeAccess.class);
        when(mockTimeAccess.systemZone()).thenReturn(ZoneId.of("Z"));
        when(mockTimeAccess.localDate()).thenReturn(LocalDate.EPOCH);

        ReminderScheduler scheduler = new ReminderScheduler(mock, mockTimeAccess);

        MedicineWithReminders medicineWithReminders = TestHelper.buildMedicineWithReminders(1, "Test");
        Reminder reminder = TestHelper.buildReminder(1, 1, "1", 480, 6);
        // Allow only on Mondays and only every 6 days. The start of the cycle will be on the 1.1.1970.
        reminder.days.set(1, false);
        reminder.days.set(2, false);
        reminder.days.set(3, false);
        reminder.days.set(4, false);
        reminder.days.set(5, false);
        reminder.days.set(6, false);
        medicineWithReminders.reminders.add(reminder);

        List<MedicineWithReminders> medicineList = new ArrayList<>();
        medicineList.add(medicineWithReminders);

        List<ReminderEvent> reminderEventList = new ArrayList<>();
        //reminderEventList.add(buildReminderEvent(1, on(2, 480).getEpochSecond()));

        scheduler.schedule(medicineList, reminderEventList);
        // Expect it to be on the 26.1.1970
        verify(mock, times(1)).onNextReminder(on(19, 480), medicineWithReminders.medicine, reminder);
    }
}
