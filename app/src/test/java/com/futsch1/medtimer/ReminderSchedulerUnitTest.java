package com.futsch1.medtimer;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.futsch1.medtimer.database.Medicine;
import com.futsch1.medtimer.database.MedicineWithReminders;
import com.futsch1.medtimer.database.Reminder;

import org.junit.Test;

import java.time.Instant;
import java.util.ArrayList;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ReminderSchedulerUnitTest {

    @Test
    public void scheduler_initialization() {
        ReminderScheduler.ScheduleListener mock = mock(ReminderScheduler.ScheduleListener.class);
        ReminderScheduler.GetInstant mockDate = mock(ReminderScheduler.GetInstant.class);
        ReminderScheduler scheduler = new ReminderScheduler(mock, mockDate);

        scheduler.updateReminderEvents(new ArrayList<>());
        verifyNoInteractions(mock);

        scheduler = new ReminderScheduler(mock, mockDate);
        scheduler.updateMedicine(new ArrayList<>());
        verifyNoInteractions(mock);
    }

    @Test
    public void scheduler_emptyLists() {
        ReminderScheduler.ScheduleListener mock = mock(ReminderScheduler.ScheduleListener.class);
        ReminderScheduler.GetInstant mockDate = mock(ReminderScheduler.GetInstant.class);
        when(mockDate.now()).thenReturn(Instant.EPOCH);
        ReminderScheduler scheduler = new ReminderScheduler(mock, mockDate);

        // Two empty lists
        scheduler.updateReminderEvents(new ArrayList<>());
        scheduler.updateMedicine(new ArrayList<>());
        verifyNoInteractions(mock);

        // One medicine without reminders, no reminder events
        MedicineWithReminders medicineWithReminders = build("Test");
        scheduler.updateMedicine(new ArrayList<MedicineWithReminders>() {{
            add(medicineWithReminders);
        }});
        verifyNoInteractions(mock);

        // No reminder events
        Reminder reminder = new Reminder(1);
        reminder.timeInMinutes = 12;
        medicineWithReminders.reminders.add(reminder);
        scheduler.updateMedicine(new ArrayList<MedicineWithReminders>() {{
            add(medicineWithReminders);
        }});
        verify(mock, times(1)).schedule(Instant.ofEpochSecond(12 * 60), medicineWithReminders.medicine, reminder);
    }

    private MedicineWithReminders build(String medicineName) {
        MedicineWithReminders medicineWithReminders = new MedicineWithReminders();
        medicineWithReminders.medicine = new Medicine(medicineName);
        medicineWithReminders.medicine.medicineId = 1;
        medicineWithReminders.reminders = new ArrayList<>();
        return medicineWithReminders;
    }
}