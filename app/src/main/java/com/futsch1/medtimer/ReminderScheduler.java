package com.futsch1.medtimer;

import android.util.Log;

import androidx.annotation.NonNull;

import com.futsch1.medtimer.database.Medicine;
import com.futsch1.medtimer.database.MedicineWithReminders;
import com.futsch1.medtimer.database.Reminder;
import com.futsch1.medtimer.database.ReminderEvent;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

public class ReminderScheduler {
    private final ScheduleListener listener;
    private final TimeAccess timeAccess;

    public ReminderScheduler(ScheduleListener listener, TimeAccess timeAccess) {
        this.listener = listener;
        this.timeAccess = timeAccess;
    }

    public void schedule(@NonNull List<MedicineWithReminders> medicineWithReminders, ReminderEvent lastReminderEvent) {
        if (medicineWithReminders.size() > 0) {
            ArrayList<Reminder> sortedReminders = getSortedReminders(medicineWithReminders);

            if (sortedReminders.size() > 0) {
                Reminder nextReminder = null;
                LocalDate checkDate = this.timeAccess.localDate();
                Instant lastReminder = lastReminderEvent != null ? Instant.ofEpochSecond(lastReminderEvent.remindedTimestamp) : Instant.EPOCH;
                for (Reminder reminder : sortedReminders) {
                    boolean newReminder = lastReminderEvent == null || lastReminderEvent.reminderId != reminder.reminderId;
                    Instant targetInstant = getTargetInstant(reminder, checkDate);
                    if (newReminder && !targetInstant.isBefore(lastReminder) && targetInstant.isAfter(Instant.ofEpochSecond(reminder.createdTimestamp))) {
                        Log.d("Scheduler",
                                String.format("Scheduling reminder ID%d to %s, last was %s with ID %d",
                                        reminder.reminderId,
                                        targetInstant,
                                        lastReminder,
                                        lastReminderEvent != null ? lastReminderEvent.reminderId : -1));
                        nextReminder = reminder;
                        break;
                    }
                }
                if (nextReminder == null) {
                    checkDate = checkDate.plusDays(1);
                    nextReminder = sortedReminders.get(0);
                    Log.d("Scheduler",
                            String.format("Scheduling reminder ID %d to the next day",
                                    nextReminder.reminderId));
                }

                Instant targetInstant = getTargetInstant(nextReminder, checkDate);

                this.listener.schedule(targetInstant, getMedicine(nextReminder, medicineWithReminders), nextReminder);
            }
        }
    }

    private ArrayList<Reminder> getSortedReminders(List<MedicineWithReminders> medicineWithReminders) {
        ArrayList<Reminder> sortedReminders = new ArrayList<>();
        for (MedicineWithReminders medicineWithReminder : medicineWithReminders
        ) {
            sortedReminders.addAll(medicineWithReminder.reminders);
        }
        sortedReminders.sort((Reminder a, Reminder b) -> a.timeInMinutes == b.timeInMinutes ? (a.medicineRelId - b.medicineRelId) : (a.timeInMinutes - b.timeInMinutes));
        return sortedReminders;
    }

    private Instant getTargetInstant(Reminder reminder, LocalDate targetDate) {
        ZoneOffset offset = timeAccess.systemZone().getRules().getOffset(targetDate.atStartOfDay());
        OffsetDateTime localTime = OffsetDateTime.of(targetDate, LocalTime.of(reminder.timeInMinutes / 60, reminder.timeInMinutes % 60), offset);
        return localTime.toInstant();
    }

    private Medicine getMedicine(Reminder reminder, List<MedicineWithReminders> medicineWithReminders) {
        int medicineId = reminder.medicineRelId;
        //noinspection OptionalGetWithoutIsPresent
        return medicineWithReminders.stream().filter(mwr -> mwr.medicine.medicineId == medicineId).findFirst().get().medicine;
    }

    public interface ScheduleListener {
        void schedule(Instant timestamp, Medicine medicine, Reminder reminder);
    }

    public interface TimeAccess {
        ZoneId systemZone();

        LocalDate localDate();
    }
}
