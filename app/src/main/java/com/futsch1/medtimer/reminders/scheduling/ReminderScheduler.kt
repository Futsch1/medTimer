package com.futsch1.medtimer.reminders.scheduling;

import androidx.annotation.NonNull;

import com.futsch1.medtimer.ScheduledReminder;
import com.futsch1.medtimer.database.FullMedicine;
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

    public List<ScheduledReminder> schedule(@NonNull List<FullMedicine> fullMedicineWithTagsAndReminders, @NonNull List<ReminderEvent> reminderEvents) {
        ArrayList<Reminder> reminders = getReminders(fullMedicineWithTagsAndReminders);
        ArrayList<ScheduledReminder> scheduledReminders = new ArrayList<>();

        for (Reminder reminder : reminders) {
            Scheduling scheduling = new SchedulingFactory().create(reminder, reminderEvents, this.timeAccess);
            Instant reminderScheduledTime = scheduling.getNextScheduledTime();

            if (reminderScheduledTime != null) {
                scheduledReminders.add(new ScheduledReminder(getMedicine(reminder, fullMedicineWithTagsAndReminders), reminder, reminderScheduledTime));
            }
        }

        scheduledReminders.sort(Comparator.comparing(ScheduledReminder::timestamp));

        return scheduledReminders;
    }

    @SuppressWarnings("java:S6204") // Stream.toList() not available in SDK version selected
    private ArrayList<Reminder> getReminders(List<FullMedicine> fullMedicineWithTagsAndReminders) {
        ArrayList<Reminder> reminders = new ArrayList<>();
        for (FullMedicine medicineWithReminder : fullMedicineWithTagsAndReminders
        ) {
            //noinspection SimplifyStreamApiCallChains
            reminders.addAll(medicineWithReminder.reminders.stream().filter(r -> r.active).collect(Collectors.toList()));
        }
        return reminders;
    }

    private FullMedicine getMedicine(Reminder reminder, List<FullMedicine> fullMedicineWithTagsAndReminders) {
        int medicineId = reminder.medicineRelId;

        Optional<FullMedicine> medicineOptional = fullMedicineWithTagsAndReminders.stream().filter(mwr -> mwr.medicine.medicineId == medicineId).findFirst();
        if (medicineOptional.isPresent()) {
            return medicineOptional.get();
        } else {
            throw new NoSuchElementException();
        }
    }

    public interface TimeAccess {
        ZoneId systemZone();

        LocalDate localDate();
    }

}
