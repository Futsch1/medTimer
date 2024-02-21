package com.futsch1.medtimer.reminders;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.futsch1.medtimer.LogTags;
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

    public void schedule(@NonNull List<MedicineWithReminders> medicineWithReminders, List<ReminderEvent> lastReminderEvents) {
        ArrayList<Reminder> sortedReminders = getSortedReminders(medicineWithReminders);

        if (sortedReminders.size() > 0) {
            LocalDate checkDate = this.timeAccess.localDate();
            Reminder nextReminder = findNextReminder(sortedReminders, lastReminderEvents, checkDate);

            if (nextReminder == null) {
                checkDate = checkDate.plusDays(1);
                nextReminder = sortedReminders.get(0);
                Log.d(LogTags.SCHEDULER,
                        String.format("Scheduling reminder ID %d to the next day",
                                nextReminder.reminderId));
            }

            Instant targetInstant = getTargetInstant(nextReminder, checkDate);

            this.listener.schedule(targetInstant, getMedicine(nextReminder, medicineWithReminders), nextReminder);
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

    private @Nullable Reminder findNextReminder(List<Reminder> sortedReminders, List<ReminderEvent> lastReminderEvents, LocalDate checkDate) {
        Reminder nextReminder = null;

        ReminderEvent lastReminderEvent = lastReminderEvents.size() > 0 ? lastReminderEvents.get(lastReminderEvents.size() - 1) : null;
        Instant lastReminder = lastReminderEvent != null ? Instant.ofEpochSecond(lastReminderEvent.remindedTimestamp) : Instant.EPOCH;
        for (Reminder reminder : sortedReminders) {

            Instant targetInstant = getTargetInstant(reminder, checkDate);

            boolean justCreated = targetInstant.isBefore(Instant.ofEpochSecond(reminder.createdTimestamp));
            boolean isTimeForReminder = !targetInstant.isBefore(lastReminder);

            if (!justCreated && isTimeForReminder && !wasProcessed(reminder, lastReminderEvents, targetInstant)) {
                Log.d(LogTags.SCHEDULER,
                        String.format("Scheduling reminder ID%d to %s, last was %s with ID %d",
                                reminder.reminderId,
                                targetInstant,
                                lastReminder,
                                lastReminderEvent != null ? lastReminderEvent.reminderId : -1));
                nextReminder = reminder;
                break;
            }
        }

        return nextReminder;
    }

    private Instant getTargetInstant(Reminder reminder, LocalDate targetDate) {
        ZoneOffset offset = timeAccess.systemZone().getRules().getOffset(targetDate.atStartOfDay());
        OffsetDateTime localTime = OffsetDateTime.of(targetDate, LocalTime.of((reminder.timeInMinutes / 60) % 24, reminder.timeInMinutes % 60), offset);
        return localTime.toInstant();
    }

    private Medicine getMedicine(Reminder reminder, List<MedicineWithReminders> medicineWithReminders) {
        int medicineId = reminder.medicineRelId;
        //noinspection OptionalGetWithoutIsPresent
        return medicineWithReminders.stream().filter(mwr -> mwr.medicine.medicineId == medicineId).findFirst().get().medicine;
    }

    private boolean wasProcessed(Reminder reminder, List<ReminderEvent> reminderEvents, Instant targetInstant) {
        for (ReminderEvent reminderEvent : reminderEvents) {
            if (reminderEvent.reminderId == reminder.reminderId) {
                if (!Instant.ofEpochSecond(reminderEvent.remindedTimestamp).isBefore(targetInstant)) {
                    return true;
                }
            }
        }
        return false;
    }

    public interface ScheduleListener {
        void schedule(Instant timestamp, Medicine medicine, Reminder reminder);
    }

    public interface TimeAccess {
        ZoneId systemZone();

        LocalDate localDate();
    }
}
