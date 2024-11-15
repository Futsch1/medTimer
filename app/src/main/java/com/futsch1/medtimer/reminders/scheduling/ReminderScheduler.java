package com.futsch1.medtimer.reminders.scheduling;

import androidx.annotation.NonNull;

import com.futsch1.medtimer.ScheduledReminder;
import com.futsch1.medtimer.database.Medicine;
import com.futsch1.medtimer.database.MedicineWithReminders;
import com.futsch1.medtimer.database.Reminder;
import com.futsch1.medtimer.database.ReminderEvent;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

public class ReminderScheduler {
    private final TimeAccess timeAccess;

    public ReminderScheduler(TimeAccess timeAccess) {
        this.timeAccess = timeAccess;
    }

    public List<ScheduledReminder> schedule(@NonNull List<MedicineWithReminders> medicineWithReminders, @NonNull List<ReminderEvent> reminderEvents) {
        ArrayList<Reminder> reminders = getReminders(medicineWithReminders);
        ArrayList<ScheduledReminder> scheduledReminders = new ArrayList<>();

        for (Reminder reminder : reminders) {
            Scheduling scheduling = new SchedulingFactory().create(reminder, reminderEvents, this.timeAccess);
            Instant reminderScheduledTime = scheduling.getNextScheduledTime();

            if (reminderScheduledTime != null) {
                scheduledReminders.add(new ScheduledReminder(getMedicine(reminder, medicineWithReminders), reminder, reminderScheduledTime));
            }
        }

        scheduledReminders.sort(Comparator.comparing(ScheduledReminder::timestamp));

        return scheduledReminders;
    }

    private ArrayList<Reminder> getReminders(List<MedicineWithReminders> medicineWithReminders) {
        ArrayList<Reminder> reminders = new ArrayList<>();
        for (MedicineWithReminders medicineWithReminder : medicineWithReminders
        ) {
            //noinspection SimplifyStreamApiCallChains
            reminders.addAll(medicineWithReminder.reminders.stream().filter(r -> r.active).collect(Collectors.toList()));
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

    public interface TimeAccess {
        ZoneId systemZone();

        LocalDate localDate();
    }

}
