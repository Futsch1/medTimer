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
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

public class ReminderScheduler {
    private final NextReminderReceiver listener;
    private final TimeAccess timeAccess;
    private AllNextRemindersReceiver allNextRemindersReceiver;

    public ReminderScheduler(NextReminderReceiver listener, TimeAccess timeAccess) {
        this.listener = listener;
        this.timeAccess = timeAccess;
    }

    public void schedule(@NonNull List<MedicineWithReminders> medicineWithReminders, List<ReminderEvent> reminderEvents) {
        ArrayList<Reminder> reminders = getReminders(medicineWithReminders);
        ArrayList<AllNextRemindersReceiver.ScheduledReminder> scheduledReminders = new ArrayList<>();

        for (Reminder reminder : reminders) {
            List<ReminderEvent> filteredEvents = getFilteredEvents(reminderEvents, reminder.reminderId);
            ReminderForScheduling reminderForScheduling = new ReminderForScheduling(reminder, filteredEvents, this.timeAccess);
            Instant reminderScheduledTime = reminderForScheduling.getNextScheduledTime();

            if (reminderScheduledTime != null) {
                scheduledReminders.add(new AllNextRemindersReceiver.ScheduledReminder(getMedicine(reminder, medicineWithReminders), reminder, reminderScheduledTime));
            }
        }

        scheduledReminders.sort(Comparator.comparing(o -> o.timestamp));

        processFoundNextReminder(scheduledReminders);

        if (allNextRemindersReceiver != null) {
            allNextRemindersReceiver.onAllNextReminders(scheduledReminders);
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

    @SuppressWarnings("java:S6204")
    private List<ReminderEvent> getFilteredEvents(List<ReminderEvent> reminderEvents, int reminderId) {
        return reminderEvents.stream().filter(event -> event.reminderId == reminderId).collect(Collectors.toList());
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

    private void processFoundNextReminder(List<AllNextRemindersReceiver.ScheduledReminder> scheduledReminders) {
        if (!scheduledReminders.isEmpty()) {
            AllNextRemindersReceiver.ScheduledReminder scheduledReminder = scheduledReminders.get(0);
            this.listener.onNextReminder(scheduledReminder.timestamp, scheduledReminder.medicine, scheduledReminder.reminder);
        }
    }

    public void setAllNextRemindersReceiver(AllNextRemindersReceiver allNextRemindersReceiver) {
        this.allNextRemindersReceiver = allNextRemindersReceiver;
    }

    public interface NextReminderReceiver {
        void onNextReminder(Instant timestamp, Medicine medicine, Reminder reminder);
    }

    public interface AllNextRemindersReceiver {
        void onAllNextReminders(List<ScheduledReminder> reminders);

        record ScheduledReminder(Medicine medicine, Reminder reminder, Instant timestamp) {
        }
    }

    public interface TimeAccess {
        ZoneId systemZone();

        LocalDate localDate();
    }
}
