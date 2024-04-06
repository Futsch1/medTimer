package com.futsch1.medtimer.reminders.scheduling;

import androidx.annotation.NonNull;

import com.futsch1.medtimer.database.Medicine;
import com.futsch1.medtimer.database.MedicineWithReminders;
import com.futsch1.medtimer.database.Reminder;
import com.futsch1.medtimer.database.ReminderEvent;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

public class ReminderScheduler {
    private final ScheduleListener listener;
    private final TimeAccess timeAccess;

    public ReminderScheduler(ScheduleListener listener, TimeAccess timeAccess) {
        this.listener = listener;
        this.timeAccess = timeAccess;
    }

    public void schedule(@NonNull List<MedicineWithReminders> medicineWithReminders, List<ReminderEvent> reminderEvents) {
        ArrayList<Reminder> reminders = getReminders(medicineWithReminders);
        Instant nextScheduledTime = null;
        Reminder nextReminder = null;

        for (Reminder reminder : reminders) {
            @SuppressWarnings("java:S6204")
            List<ReminderEvent> filteredEvents = reminderEvents.stream().filter(event -> event.reminderId == reminder.reminderId).collect(Collectors.toList());
            ReminderForScheduling reminderForScheduling = new ReminderForScheduling(reminder, filteredEvents, this.timeAccess);
            Instant reminderScheduledTime = reminderForScheduling.getNextScheduledTime();
            if (nextScheduledTime == null || reminderScheduledTime.isBefore(nextScheduledTime)) {
                nextScheduledTime = reminderScheduledTime;
                nextReminder = reminder;
            }
        }

        if (nextReminder != null) {
            this.listener.schedule(nextScheduledTime, getMedicine(nextReminder, medicineWithReminders), nextReminder);
        }
    }

    private ArrayList<Reminder> getReminders(List<MedicineWithReminders> medicineWithReminders) {
        ArrayList<Reminder> reminders = new ArrayList<>();
        for (MedicineWithReminders medicineWithReminder : medicineWithReminders
        ) {
            reminders.addAll(medicineWithReminder.reminders);
        }
        return reminders;
    }


    private Medicine getMedicine(Reminder reminder, List<MedicineWithReminders> medicineWithReminders) {
        int medicineId = reminder.medicineRelId;

        Optional<MedicineWithReminders> medicineOptional = medicineWithReminders.stream().filter(mwr -> mwr.medicine.medicineId == medicineId).findFirst();
        if (medicineOptional.isPresent()) {
            return medicineOptional.get().medicine;
        } else {
            throw new NoSuchElementException();
        }
    }

    public interface ScheduleListener {
        void schedule(Instant timestamp, Medicine medicine, Reminder reminder);
    }

    public interface TimeAccess {
        ZoneId systemZone();

        LocalDate localDate();
    }
}
