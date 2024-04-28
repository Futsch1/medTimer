package com.futsch1.medtimer;

import static com.futsch1.medtimer.TestHelper.on;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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

public class ReminderSchedulerUnitTest {

    @Test
    public void test_scheduleEmptyLists() {
        ReminderScheduler.ScheduleListener mock = mock(ReminderScheduler.ScheduleListener.class);
        ReminderScheduler.TimeAccess mockTimeAccess = mock(ReminderScheduler.TimeAccess.class);
        when(mockTimeAccess.systemZone()).thenReturn(ZoneId.of("Z"));
        when(mockTimeAccess.localDate()).thenReturn(LocalDate.EPOCH);

        ReminderScheduler scheduler = new ReminderScheduler(mock, mockTimeAccess);

        // Two empty lists
        scheduler.schedule(new ArrayList<>(), new ArrayList<>());
        verifyNoInteractions(mock);

        // One medicine without reminders, no reminder events
        MedicineWithReminders medicineWithReminders = TestHelper.buildMedicineWithReminders(1, "Test");
        scheduler.schedule(new ArrayList<>() {{
            add(medicineWithReminders);
        }}, new ArrayList<>());
        verifyNoInteractions(mock);

        // No reminder events
        Reminder reminder = TestHelper.buildReminder(1, 1, "1", 12, 1);
        medicineWithReminders.reminders.add(reminder);
        scheduler.schedule(new ArrayList<>() {{
            add(medicineWithReminders);
        }}, new ArrayList<>());
        verify(mock, times(1)).schedule(on(2, 12), medicineWithReminders.medicine, reminder);
    }

    @Test
    public void test_scheduleReminders() {
        ReminderScheduler.ScheduleListener mock = mock(ReminderScheduler.ScheduleListener.class);
        ReminderScheduler.TimeAccess mockTimeAccess = mock(ReminderScheduler.TimeAccess.class);
        when(mockTimeAccess.systemZone()).thenReturn(ZoneId.of("Z"));
        when(mockTimeAccess.localDate()).thenReturn(LocalDate.EPOCH);

        ReminderScheduler scheduler = new ReminderScheduler(mock, mockTimeAccess);

        MedicineWithReminders medicineWithReminders1 = TestHelper.buildMedicineWithReminders(1, "Test1");
        Reminder reminder1 = TestHelper.buildReminder(1, 1, "1", 16, 1);
        Reminder reminder2 = TestHelper.buildReminder(1, 1, "2", 12, 1);
        medicineWithReminders1.reminders.add(reminder1);
        medicineWithReminders1.reminders.add(reminder2);
        scheduler.schedule(new ArrayList<>() {{
            add(medicineWithReminders1);
        }}, new ArrayList<>());
        verify(mock, times(1)).schedule(on(2, 12), medicineWithReminders1.medicine, reminder2);
        clearInvocations(mock);

        // Now add a second medicine with an earlier reminder
        MedicineWithReminders medicineWithReminders2 = TestHelper.buildMedicineWithReminders(2, "Test2");
        Reminder reminder3 = TestHelper.buildReminder(2, 1, "1", 3, 1);
        medicineWithReminders2.reminders.add(reminder3);
        scheduler.schedule(new ArrayList<>() {{
            add(medicineWithReminders1);
            add(medicineWithReminders2);
        }}, new ArrayList<>());
        verify(mock, times(1)).schedule(on(2, 3), medicineWithReminders2.medicine, reminder3);
    }

    @Test
    public void test_scheduleWithEvents() {
        ReminderScheduler.ScheduleListener mock = mock(ReminderScheduler.ScheduleListener.class);
        ReminderScheduler.TimeAccess mockTimeAccess = mock(ReminderScheduler.TimeAccess.class);
        when(mockTimeAccess.systemZone()).thenReturn(ZoneId.of("Z"));
        when(mockTimeAccess.localDate()).thenReturn(LocalDate.EPOCH.plusDays(1));

        ReminderScheduler scheduler = new ReminderScheduler(mock, mockTimeAccess);

        MedicineWithReminders medicineWithReminders1 = TestHelper.buildMedicineWithReminders(1, "Test1");
        Reminder reminder1 = TestHelper.buildReminder(1, 1, "1", 16, 1);
        Reminder reminder2 = TestHelper.buildReminder(1, 2, "2", 12, 1);
        medicineWithReminders1.reminders.add(reminder1);
        medicineWithReminders1.reminders.add(reminder2);
        MedicineWithReminders medicineWithReminders2 = TestHelper.buildMedicineWithReminders(2, "Test2");
        Reminder reminder3 = TestHelper.buildReminder(2, 3, "1", 3, 1);
        medicineWithReminders2.reminders.add(reminder3);
        ArrayList<MedicineWithReminders> medicineWithReminders = new ArrayList<>() {{
            add(medicineWithReminders1);
            add(medicineWithReminders2);
        }};
        // Reminder 3 already invoked
        scheduler.schedule(medicineWithReminders, new ArrayList<>() {{
            add(TestHelper.buildReminderEvent(3, on(2, 3).getEpochSecond()));
        }});
        verify(mock, times(1)).schedule(on(2, 12), medicineWithReminders1.medicine, reminder2);
        clearInvocations(mock);

        // Check two reminders at the same time
        Reminder reminder4 = TestHelper.buildReminder(2, 4, "1", 12, 1);
        medicineWithReminders2.reminders.add(reminder4);
        scheduler.schedule(medicineWithReminders, new ArrayList<>() {{
            add(TestHelper.buildReminderEvent(3, on(2, 3).getEpochSecond()));
            add(TestHelper.buildReminderEvent(2, on(2, 12).getEpochSecond()));
        }});
        verify(mock, times(1)).schedule(on(2, 12), medicineWithReminders2.medicine, reminder4);
        clearInvocations(mock);

        scheduler.schedule(medicineWithReminders, new ArrayList<>() {{
            add(TestHelper.buildReminderEvent(3, on(2, 4).getEpochSecond()));
            add(TestHelper.buildReminderEvent(2, on(2, 12).getEpochSecond()));
            add(TestHelper.buildReminderEvent(4, on(2, 12).getEpochSecond()));
        }});
        verify(mock, times(1)).schedule(on(2, 16), medicineWithReminders1.medicine, reminder1);
        clearInvocations(mock);

        // All reminders already invoked, switch to next day
        scheduler.schedule(medicineWithReminders, new ArrayList<>() {{
            add(TestHelper.buildReminderEvent(3, on(2, 4).getEpochSecond() + 4 * 60));
            add(TestHelper.buildReminderEvent(2, on(2, 12).getEpochSecond()));
            add(TestHelper.buildReminderEvent(1, on(2, 16).getEpochSecond()));
            add(TestHelper.buildReminderEvent(4, on(2, 16).getEpochSecond()));
        }});
        verify(mock, times(1)).schedule(on(3, 3), medicineWithReminders2.medicine, reminder3);
        clearInvocations(mock);

        // All reminders already invoked, we are on the next day
        when(mockTimeAccess.localDate()).thenReturn(LocalDate.EPOCH.plusDays(2));
        scheduler.schedule(medicineWithReminders, new ArrayList<>() {{
            add(TestHelper.buildReminderEvent(3, on(2, 4).getEpochSecond()));
            add(TestHelper.buildReminderEvent(2, on(2, 12).getEpochSecond()));
            add(TestHelper.buildReminderEvent(1, on(2, 16).getEpochSecond()));
        }});
        verify(mock, times(1)).schedule(on(3, 3), medicineWithReminders2.medicine, reminder3);
        clearInvocations(mock);
    }

    // schedules a reminder for the same day
    @Test
    public void test_scheduleSameDayReminder() {
        ReminderScheduler.ScheduleListener mock = mock(ReminderScheduler.ScheduleListener.class);
        ReminderScheduler.TimeAccess mockTimeAccess = mock(ReminderScheduler.TimeAccess.class);
        when(mockTimeAccess.systemZone()).thenReturn(ZoneId.of("Z"));
        when(mockTimeAccess.localDate()).thenReturn(LocalDate.EPOCH);

        ReminderScheduler scheduler = new ReminderScheduler(mock, mockTimeAccess);

        MedicineWithReminders medicineWithReminders = TestHelper.buildMedicineWithReminders(1, "Test");
        Reminder reminder = TestHelper.buildReminder(1, 1, "1", 480, 1);
        medicineWithReminders.reminders.add(reminder);

        List<MedicineWithReminders> medicineList = new ArrayList<>();
        medicineList.add(medicineWithReminders);

        List<ReminderEvent> reminderEventList = new ArrayList<>();

        scheduler.schedule(medicineList, reminderEventList);

        verify(mock, times(1)).schedule(on(2, 480), medicineWithReminders.medicine, reminder);
    }

    // schedules a reminder for a different medicine
    @Test
    public void test_scheduleDifferentMedicineReminder() {
        ReminderScheduler.ScheduleListener mock = mock(ReminderScheduler.ScheduleListener.class);
        ReminderScheduler.TimeAccess mockTimeAccess = mock(ReminderScheduler.TimeAccess.class);
        when(mockTimeAccess.systemZone()).thenReturn(ZoneId.of("Z"));
        when(mockTimeAccess.localDate()).thenReturn(LocalDate.EPOCH.plusDays(1));

        ReminderScheduler scheduler = new ReminderScheduler(mock, mockTimeAccess);

        MedicineWithReminders medicineWithReminders1 = TestHelper.buildMedicineWithReminders(1, "Test1");
        Reminder reminder1 = TestHelper.buildReminder(1, 1, "1", 480, 1);
        medicineWithReminders1.reminders.add(reminder1);

        MedicineWithReminders medicineWithReminders2 = TestHelper.buildMedicineWithReminders(2, "Test2");
        Reminder reminder2 = TestHelper.buildReminder(2, 2, "2", 480, 1);
        medicineWithReminders2.reminders.add(reminder2);

        List<MedicineWithReminders> medicineList = new ArrayList<>();
        medicineList.add(medicineWithReminders1);
        medicineList.add(medicineWithReminders2);

        List<ReminderEvent> reminderEventList = new ArrayList<>();

        scheduler.schedule(medicineList, reminderEventList);

        verify(mock, times(1)).schedule(on(2, 480), medicineWithReminders1.medicine, reminder1);
    }

    // schedules a reminder for every two days
    @Test
    public void test_scheduleReminderWithOneDayPause() {
        ReminderScheduler.ScheduleListener mock = mock(ReminderScheduler.ScheduleListener.class);
        ReminderScheduler.TimeAccess mockTimeAccess = mock(ReminderScheduler.TimeAccess.class);
        when(mockTimeAccess.systemZone()).thenReturn(ZoneId.of("Z"));
        when(mockTimeAccess.localDate()).thenReturn(LocalDate.EPOCH);

        ReminderScheduler scheduler = new ReminderScheduler(mock, mockTimeAccess);

        MedicineWithReminders medicineWithReminders = TestHelper.buildMedicineWithReminders(1, "Test");
        Reminder reminder = TestHelper.buildReminder(1, 1, "1", 480, 2);
        medicineWithReminders.reminders.add(reminder);

        List<MedicineWithReminders> medicineList = new ArrayList<>();
        medicineList.add(medicineWithReminders);

        List<ReminderEvent> reminderEventList = new ArrayList<>();

        scheduler.schedule(medicineList, reminderEventList);

        verify(mock, times(1)).schedule(on(1, 480), medicineWithReminders.medicine, reminder);

        reminderEventList.add(TestHelper.buildReminderEvent(1, on(1, 480).getEpochSecond()));
        when(mockTimeAccess.localDate()).thenReturn(LocalDate.EPOCH.plusDays(1));

        scheduler.schedule(medicineList, reminderEventList);
        verify(mock, times(1)).schedule(on(3, 480), medicineWithReminders.medicine, reminder);
    }

    // schedules a reminder for every two days
    @Test
    public void test_scheduleTwoDayReminderVsOneDay() {
        ReminderScheduler.ScheduleListener mock = mock(ReminderScheduler.ScheduleListener.class);
        ReminderScheduler.TimeAccess mockTimeAccess = mock(ReminderScheduler.TimeAccess.class);
        when(mockTimeAccess.systemZone()).thenReturn(ZoneId.of("Z"));
        when(mockTimeAccess.localDate()).thenReturn(LocalDate.EPOCH);

        ReminderScheduler scheduler = new ReminderScheduler(mock, mockTimeAccess);

        MedicineWithReminders medicineWithReminders = TestHelper.buildMedicineWithReminders(1, "Test");
        Reminder reminder = TestHelper.buildReminder(1, 1, "1", 480, 2);
        medicineWithReminders.reminders.add(reminder);
        Reminder reminder2 = TestHelper.buildReminder(1, 2, "2", 481, 1);
        medicineWithReminders.reminders.add(reminder2);

        List<MedicineWithReminders> medicineList = new ArrayList<>();
        medicineList.add(medicineWithReminders);

        List<ReminderEvent> reminderEventList = new ArrayList<>();
        reminderEventList.add(TestHelper.buildReminderEvent(1, on(1, 480).getEpochSecond()));

        when(mockTimeAccess.localDate()).thenReturn(LocalDate.EPOCH.plusDays(1));

        scheduler.schedule(medicineList, reminderEventList);
        verify(mock, times(1)).schedule(on(2, 481), medicineWithReminders.medicine, reminder2);
    }

    @Test
    public void test_scheduleReminderWithOneDayPauseVsThreeDaysPause() {
        ReminderScheduler.ScheduleListener mock = mock(ReminderScheduler.ScheduleListener.class);
        ReminderScheduler.TimeAccess mockTimeAccess = mock(ReminderScheduler.TimeAccess.class);
        when(mockTimeAccess.systemZone()).thenReturn(ZoneId.of("Z"));
        when(mockTimeAccess.localDate()).thenReturn(LocalDate.EPOCH);

        ReminderScheduler scheduler = new ReminderScheduler(mock, mockTimeAccess);

        MedicineWithReminders medicineWithReminders = TestHelper.buildMedicineWithReminders(1, "Test");
        Reminder reminder = TestHelper.buildReminder(1, 1, "1", 480, 2);
        medicineWithReminders.reminders.add(reminder);
        Reminder reminder2 = TestHelper.buildReminder(1, 2, "2", 481, 4);
        medicineWithReminders.reminders.add(reminder2);

        List<MedicineWithReminders> medicineList = new ArrayList<>();
        medicineList.add(medicineWithReminders);

        List<ReminderEvent> reminderEventList = new ArrayList<>();
        reminderEventList.add(TestHelper.buildReminderEvent(1, on(1, 480).getEpochSecond()));

        scheduler.schedule(medicineList, reminderEventList);
        verify(mock, times(1)).schedule(on(1, 481), medicineWithReminders.medicine, reminder2);

        reminderEventList.add(TestHelper.buildReminderEvent(2, on(1, 481).getEpochSecond()));

        scheduler.schedule(medicineList, reminderEventList);
        verify(mock, times(1)).schedule(on(3, 480), medicineWithReminders.medicine, reminder);

        // On day 3
        when(mockTimeAccess.localDate()).thenReturn(LocalDate.EPOCH.plusDays(2));

        reset(mock);
        scheduler.schedule(medicineList, reminderEventList);
        verify(mock, times(1)).schedule(on(3, 480), medicineWithReminders.medicine, reminder);

        reminderEventList.add(TestHelper.buildReminderEvent(1, on(3, 480).getEpochSecond()));

        scheduler.schedule(medicineList, reminderEventList);
        verify(mock, times(1)).schedule(on(5, 480), medicineWithReminders.medicine, reminder);

        // On day 5
        when(mockTimeAccess.localDate()).thenReturn(LocalDate.EPOCH.plusDays(4));

        reminderEventList.add(TestHelper.buildReminderEvent(1, on(5, 480).getEpochSecond()));

        scheduler.schedule(medicineList, reminderEventList);
        verify(mock, times(1)).schedule(on(5, 481), medicineWithReminders.medicine, reminder2);
    }

    @Test
    public void test_scheduleCycleInFuture() {
        ReminderScheduler.ScheduleListener mock = mock(ReminderScheduler.ScheduleListener.class);
        ReminderScheduler.TimeAccess mockTimeAccess = mock(ReminderScheduler.TimeAccess.class);
        when(mockTimeAccess.systemZone()).thenReturn(ZoneId.of("Z"));
        when(mockTimeAccess.localDate()).thenReturn(LocalDate.EPOCH);

        ReminderScheduler scheduler = new ReminderScheduler(mock, mockTimeAccess);

        MedicineWithReminders medicineWithReminders = TestHelper.buildMedicineWithReminders(1, "Test");
        Reminder reminder = TestHelper.buildReminder(1, 1, "1", 480, 3);
        reminder.cycleStartDay = 4;
        medicineWithReminders.reminders.add(reminder);

        List<MedicineWithReminders> medicineList = new ArrayList<>();
        medicineList.add(medicineWithReminders);

        List<ReminderEvent> reminderEventList = new ArrayList<>();

        scheduler.schedule(medicineList, reminderEventList);
        verify(mock, times(1)).schedule(on(5, 480), medicineWithReminders.medicine, reminder);

        when(mockTimeAccess.localDate()).thenReturn(LocalDate.EPOCH.plusDays(4));

        reset(mock);
        scheduler.schedule(medicineList, reminderEventList);
        verify(mock, times(1)).schedule(on(5, 480), medicineWithReminders.medicine, reminder);

        when(mockTimeAccess.localDate()).thenReturn(LocalDate.EPOCH.plusDays(5));

        reset(mock);
        scheduler.schedule(medicineList, reminderEventList);
        verify(mock, times(1)).schedule(on(8, 480), medicineWithReminders.medicine, reminder);
    }

}