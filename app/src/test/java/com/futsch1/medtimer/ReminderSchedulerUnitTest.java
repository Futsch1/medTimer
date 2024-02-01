package com.futsch1.medtimer;

import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.futsch1.medtimer.database.Medicine;
import com.futsch1.medtimer.database.MedicineWithReminders;
import com.futsch1.medtimer.database.Reminder;
import com.futsch1.medtimer.database.ReminderEvent;

import org.junit.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ReminderSchedulerUnitTest {

    @Test
    public void scheduler_emptyLists() {
        ReminderScheduler.ScheduleListener mock = mock(ReminderScheduler.ScheduleListener.class);
        ReminderScheduler.TimeAccess mockTimeAccess = mock(ReminderScheduler.TimeAccess.class);
        when(mockTimeAccess.systemZone()).thenReturn(ZoneId.of("Z"));
        when(mockTimeAccess.localDate()).thenReturn(LocalDate.EPOCH);

        ReminderScheduler scheduler = new ReminderScheduler(mock, mockTimeAccess);

        // Two empty lists
        scheduler.schedule(new ArrayList<>(), new ArrayList<>());
        verifyNoInteractions(mock);

        // One medicine without reminders, no reminder events
        MedicineWithReminders medicineWithReminders = buildMedicineWithReminders(1, "Test");
        scheduler.schedule(new ArrayList<MedicineWithReminders>() {{
            add(medicineWithReminders);
        }}, new ArrayList<>());
        verifyNoInteractions(mock);

        // No reminder events
        Reminder reminder = new Reminder(1);
        reminder.timeInMinutes = 12;
        medicineWithReminders.reminders.add(reminder);
        scheduler.schedule(new ArrayList<MedicineWithReminders>() {{
            add(medicineWithReminders);
        }}, new ArrayList<>());
        verify(mock, times(1)).schedule(Instant.ofEpochSecond(12 * 60), medicineWithReminders.medicine, reminder);
    }

    private MedicineWithReminders buildMedicineWithReminders(int medicineId, String medicineName) {
        MedicineWithReminders medicineWithReminders = new MedicineWithReminders();
        medicineWithReminders.medicine = new Medicine(medicineName);
        medicineWithReminders.medicine.medicineId = medicineId;
        medicineWithReminders.reminders = new ArrayList<>();
        return medicineWithReminders;
    }


    @Test
    public void scheduler_reminders() {
        ReminderScheduler.ScheduleListener mock = mock(ReminderScheduler.ScheduleListener.class);
        ReminderScheduler.TimeAccess mockTimeAccess = mock(ReminderScheduler.TimeAccess.class);
        when(mockTimeAccess.systemZone()).thenReturn(ZoneId.of("Z"));
        when(mockTimeAccess.localDate()).thenReturn(LocalDate.EPOCH);

        ReminderScheduler scheduler = new ReminderScheduler(mock, mockTimeAccess);

        MedicineWithReminders medicineWithReminders1 = buildMedicineWithReminders(1, "Test1");
        Reminder reminder1 = buildReminder(1, 1, "1", 16);
        Reminder reminder2 = buildReminder(1, 1, "2", 12);
        medicineWithReminders1.reminders.add(reminder1);
        medicineWithReminders1.reminders.add(reminder2);
        scheduler.schedule(new ArrayList<MedicineWithReminders>() {{
            add(medicineWithReminders1);
        }}, new ArrayList<>());
        verify(mock, times(1)).schedule(Instant.ofEpochSecond(12 * 60), medicineWithReminders1.medicine, reminder2);
        clearInvocations(mock);

        // Now add a second medicine with an earlier reminder
        MedicineWithReminders medicineWithReminders2 = buildMedicineWithReminders(2, "Test2");
        Reminder reminder3 = buildReminder(2, 1, "1", 3);
        medicineWithReminders2.reminders.add(reminder3);
        scheduler.schedule(new ArrayList<MedicineWithReminders>() {{
            add(medicineWithReminders1);
            add(medicineWithReminders2);
        }}, new ArrayList<>());
        verify(mock, times(1)).schedule(Instant.ofEpochSecond(3 * 60), medicineWithReminders2.medicine, reminder3);
    }

    private Reminder buildReminder(int medicineId, int reminderId, String amount, int timeInMinutes) {
        Reminder reminder = new Reminder(medicineId);
        reminder.reminderId = reminderId;
        reminder.amount = amount;
        reminder.timeInMinutes = timeInMinutes;
        return reminder;
    }

    @Test
    public void scheduler_withEvents() {
        ReminderScheduler.ScheduleListener mock = mock(ReminderScheduler.ScheduleListener.class);
        ReminderScheduler.TimeAccess mockTimeAccess = mock(ReminderScheduler.TimeAccess.class);
        when(mockTimeAccess.systemZone()).thenReturn(ZoneId.of("Z"));
        when(mockTimeAccess.localDate()).thenReturn(LocalDate.EPOCH);

        ReminderScheduler scheduler = new ReminderScheduler(mock, mockTimeAccess);

        MedicineWithReminders medicineWithReminders1 = buildMedicineWithReminders(1, "Test1");
        Reminder reminder1 = buildReminder(1, 1, "1", 16);
        Reminder reminder2 = buildReminder(1, 2, "2", 12);
        medicineWithReminders1.reminders.add(reminder1);
        medicineWithReminders1.reminders.add(reminder2);
        MedicineWithReminders medicineWithReminders2 = buildMedicineWithReminders(2, "Test2");
        Reminder reminder3 = buildReminder(2, 3, "1", 3);
        medicineWithReminders2.reminders.add(reminder3);
        ArrayList<MedicineWithReminders> medicineWithReminders = new ArrayList<MedicineWithReminders>() {{
            add(medicineWithReminders1);
            add(medicineWithReminders2);
        }};
        // Reminder 3 already invoked
        scheduler.schedule(medicineWithReminders, new ArrayList<ReminderEvent>() {{
            add(buildReminderEvent(3, 4));
        }});
        verify(mock, times(1)).schedule(Instant.ofEpochSecond(12 * 60), medicineWithReminders1.medicine, reminder2);
        clearInvocations(mock);

        // All reminders already invoked, switch to next day
        scheduler.schedule(medicineWithReminders, new ArrayList<ReminderEvent>() {{
            add(buildReminderEvent(3, 4));
            add(buildReminderEvent(2, 12));
            add(buildReminderEvent(1, 16));
        }});
        verify(mock, times(1)).schedule(Instant.ofEpochSecond((3 + 1440) * 60), medicineWithReminders2.medicine, reminder3);
        clearInvocations(mock);

        // All reminders already invoked, we are on the next day
        when(mockTimeAccess.localDate()).thenReturn(LocalDate.EPOCH.plusDays(1));
        scheduler.schedule(medicineWithReminders, new ArrayList<ReminderEvent>() {{
            add(buildReminderEvent(3, 4));
            add(buildReminderEvent(2, 12));
            add(buildReminderEvent(1, 16));
        }});
        verify(mock, times(1)).schedule(Instant.ofEpochSecond((3 + 1440) * 60), medicineWithReminders2.medicine, reminder3);
        clearInvocations(mock);
    }

    private ReminderEvent buildReminderEvent(int reminderId, long raisedTimestamp) {
        ReminderEvent reminderEvent = new ReminderEvent();
        reminderEvent.reminderId = reminderId;
        reminderEvent.remindedTimestamp = raisedTimestamp;
        return reminderEvent;
    }
}